import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { join, dirname } from 'node:path';
import { tmpdir } from 'node:os';
import { createHash } from 'node:crypto';
import { artifactInfo } from './delivery.mjs';
const exec = promisify(execFile), hash = bytes => createHash('sha256').update(bytes).digest('hex');
const sensitive = path => path.split(/[\\/]/).some(p => /^\.env($|\.)/i.test(p) || p.toLowerCase() === '.git' || /\.(pem|key|jks|keystore)$/i.test(p));
async function git(cwd,args) {
  try { return (await exec('git',['-c','core.fsmonitor=false',...args],{cwd,windowsHide:true,timeout:60000,maxBuffer:50*1024*1024,env:{...process.env,GIT_CONFIG_NOSYSTEM:'1',GIT_CONFIG_GLOBAL:process.platform==='win32'?'NUL':'/dev/null',GIT_PAGER:'',GIT_EXTERNAL_DIFF:''}})).stdout; }
  catch(error) { if(error.code===1 && args.includes('--no-index'))return error.stdout;throw error; }
}

// Never run git add or project filters from the unsandboxed connector.
export async function publishDeliverables(taskId, workspace, evidence, changedFiles, upload) {
  if(!evidence.passed)throw new Error('未核验产物不能上传');
  const paths=[...new Set([...evidence.artifacts.map(a=>a.path),...changedFiles])];
  if(paths.some(sensitive))throw new Error('交付含敏感路径，未上传；请在电脑核对');
  if(evidence.artifacts.length>63)throw new Error('手机交付最多63个文件及1份差异');
  const published=[];let total=0;
  const send=async(name,kind,bytes,expected)=>{
    if(hash(bytes)!==expected)throw new Error('上传前产物已变化：'+name);
    total+=bytes.length;if(bytes.length>50*1024*1024 || total>100*1024*1024)throw new Error('手机交付超过50MiB单文件或100MiB任务限额');
    const metadata={id:hash(kind+'\0'+name),name,kind,sha256:expected,size:bytes.length};
    await upload(taskId,metadata,bytes);published.push(metadata);
  };
  for(const file of evidence.artifacts) {
    if(file.deleted)continue;
    const actual=await artifactInfo(workspace.cwd,file.path,true);
    if(!actual || actual.sha256!==file.sha256)throw new Error('上传前产物已变化：'+file.path);
    await send(file.path,'artifact',actual.content,file.sha256);
  }
  if(changedFiles.length) {
    let patch=await git(workspace.cwd,['diff','--no-ext-diff','--no-textconv','--binary',workspace.baselineCommit,'--']);
    const tracked=new Set([...workspace.baseline.map(f=>f.path),...(await git(workspace.cwd,['ls-files','--cached','-z'])).split('\0')]);
    const scratch=await mkdtemp(join(tmpdir(),'droprun-diff-'));
    try {
      for(const path of changedFiles.filter(p=>!tracked.has(p))) {
        const actual=await artifactInfo(workspace.cwd,path,true);if(!actual)continue;
        const output=join(scratch,'b',path);await mkdir(dirname(output),{recursive:true});await writeFile(output,actual.content);
        patch+=await git(scratch,['diff','--no-index','--no-ext-diff','--no-textconv','--binary','--no-prefix','--',process.platform==='win32'?'NUL':'/dev/null','b/'+path.replaceAll('\\','/')]);
      }
    } finally {await rm(scratch,{recursive:true,force:true});}
    const bytes=Buffer.from(patch,'utf8');await send('changes.patch','diff',bytes,hash(bytes));
  }
  return published;
}
