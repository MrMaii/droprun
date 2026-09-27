import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomUUID, createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { createWorkspace, inspectWorkspace } from '../connector/workspace.mjs';
import { artifactInfo } from '../connector/delivery.mjs';
import { publishDeliverables } from '../connector/publish-deliverables.mjs';
const hash=x=>createHash('sha256').update(x).digest('hex');
async function fixture(t) {
  const root=await mkdtemp(join(tmpdir(),'droprun-publish-'));t.after(()=>rm(root,{recursive:true,force:true}));
  const source=join(root,'source');await mkdir(source);
  await writeFile(join(source,'old.txt'),'before\n');await writeFile(join(source,'deleted.txt'),'remove\n');await writeFile(join(source,'.gitattributes'),'*.txt diff=trap filter=trap\n');
  const workspace=await createWorkspace({taskId:randomUUID(),projectId:randomUUID(),sourceCwd:source,baseDirectory:join(root,'work')});
  for(const key of ['diff.external','diff.trap.textconv','filter.trap.clean'])execFileSync('git',['-C',workspace.cwd,'config',key,'must-not-execute-untrusted-project-tool'],{windowsHide:true});
  await writeFile(join(workspace.cwd,'old.txt'),'after\n');await rm(join(workspace.cwd,'deleted.txt'));
  await writeFile(join(workspace.cwd,'new.txt'),'new content\n');await writeFile(join(workspace.cwd,'binary.bin'),Buffer.from([0,1,255]));
  const evidence={passed:true,artifacts:[await artifactInfo(workspace.cwd,'new.txt'),await artifactInfo(workspace.cwd,'binary.bin'),{path:'deleted.txt',deleted:true}]};
  return {workspace,evidence,changed:(await inspectWorkspace(workspace)).changedFiles};
}
test('publishes exact artifacts and tracked/untracked/binary/deleted diff without touching index or running filters',async t=>{
  const {workspace,evidence,changed}=await fixture(t),index=await readFile(join(workspace.cwd,'.git/index')),sent=[];
  const files=await publishDeliverables(workspace.taskId,workspace,evidence,changed,async(task,metadata,bytes)=>{assert.equal(task,workspace.taskId);assert.equal(hash(bytes),metadata.sha256);sent.push({metadata,bytes});});
  assert.equal(files.length,3);assert.deepEqual(await readFile(join(workspace.cwd,'.git/index')),index);
  assert.equal(await readFile(join(workspace.sourceCwd,'old.txt'),'utf8'),'before\n');
  assert.equal(sent.find(x=>x.metadata.name==='new.txt').bytes.toString(),'new content\n');
  const patch=sent.find(x=>x.metadata.kind==='diff').bytes.toString();
  for(const text of ['-before','+after','+new content','deleted.txt','binary.bin','GIT binary patch'])assert.ok(patch.includes(text),text);
});
test('changed, unverified and sensitive artifacts cannot be uploaded',async t=>{
  const {workspace,evidence,changed}=await fixture(t);let sent=0;const upload=async()=>{sent++;};
  await assert.rejects(publishDeliverables(workspace.taskId,workspace,{...evidence,passed:false},changed,upload));
  await assert.rejects(publishDeliverables(workspace.taskId,workspace,evidence,[...changed,'.env'],upload));
  await writeFile(join(workspace.cwd,'new.txt'),'changed after evidence');
  await assert.rejects(publishDeliverables(workspace.taskId,workspace,evidence,changed,upload),/已变化/);assert.equal(sent,0);
});
