import test from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { mkdtemp, mkdir, writeFile, readFile, unlink } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomUUID } from 'node:crypto';
import { processProjectTask } from '../connector/project-task.mjs';
import { VisualStore } from '../connector/visual-store.mjs';

const visualIntent={outcome:'ready',summary:'把第二页作品卡片改为悬停展开',effect:'悬停展开',motion:true,referenceRequired:true,targets:[{route:'/work',purpose:'作品展示',files:['work.html'],expected:'悬停卡片展开，作品文字仍可见'}],limitations:[]};
const reportData={outcome:'completed',artifacts:['work.html'],verification:['read work.html'],remaining:[]};
const report='## 我看到了什么\n参考片段中卡片悬停展开。\n## 用在哪里\n/work 是作品展示页。\n## 我做了什么\n更新卡片交互。\n## 验证\n核对页面与截图。\n## 未完成\n无\n\n```droprun\n'+JSON.stringify(reportData)+'\n```';

async function fixture(options={}) {
  const root=await mkdtemp(join(tmpdir(),'droprun-project-visual-')),cwd=join(root,'project'),stateDir=join(root,'state');
  await mkdir(cwd); await mkdir(stateDir); await writeFile(join(cwd,'work.html'),'<main>Original portfolio</main>');
  const task={id:randomUUID(),project_id:randomUUID(),permission_version:randomUUID(),execution_mode:'direct',project_name:'Portfolio',content:'https://example.test/ui-demo',message:'把第2页替换成这个效果',status:'reading'};
  const f={root,cwd,stateDir,task,turns:[],uploads:[],completedTurns:[],enabled:true,writes:0,executionTurns:0,restores:0,readCalls:0,store:new VisualStore(stateDir)};
  f.intent=options.intent||visualIntent;
  const sheet=join(root,'reference.jpg'); await writeFile(sheet,'reference fixture');
  const frames=[{path:'frame-a',timeSeconds:0,sha256:'a'},{path:'frame-b',timeSeconds:0.5,sha256:'b'}];
  const material={files:[sheet],images:[sheet],brief:'视频 1 秒，已取得时序画面',summary:'时序参考',digest:{title:'Card motion'},limitations:[],visualEvidence:{version:1,attachedImages:[],coverImages:[],videos:[{path:'source.mp4',durationSeconds:1,processedSeconds:1,decoded:true,truncated:false,uniqueFrameCount:2,overview:{intervalSeconds:0.5,frames,sheets:[sheet]},motionWindows:[{startSeconds:0,endSeconds:1,intervalSeconds:0.125,frames,sheets:[sheet]}]}]}};
  if(options.coverOnly) {material.visualEvidence.videos=[];material.visualEvidence.coverImages=[sheet];material.brief='仅取得标题与封面';}
  const permission=()=>({taskId:task.id,projectId:task.project_id,enabled:f.enabled,version:task.permission_version,profile:'original-project',executionMode:'direct'});
  const api=async(path,body)=>{
    if(path.endsWith('/permission')) return permission();
    if(path.endsWith('/invalidate')) return {ok:true};
    if(path==='/connector/task') {task.status=body.status;task.thread_id=body.threadId;task.turn_id=body.turnId;return {...task};}
    throw new Error('Unexpected API '+path);
  };
  const thread=()=>({id:f.threadId,projectId:task.project_id,status:{type:'idle'},turns:f.completedTurns});
  class Runner extends EventEmitter {
    async call(method,params) {
      if(method==='thread/start') {f.threadId=randomUUID();return {thread:thread()};}
      if(method==='thread/read'||method==='thread/resume') return {thread:thread()};
      if(method==='thread/name/set') return {};
      if(method==='mcpServerStatus/list') return {data:[]};
      if(method!=='turn/start') throw new Error('Unexpected method '+method);
      f.turns.push(params);
      const id=randomUUID(),items=[];
      let raw;
      if(params.sandboxPolicy.type==='readOnly'&&params.outputSchema) {
        raw=options.invalidInspection && (options.neverRepairInspection || f.turns.length===1) ? JSON.stringify({...f.intent,targets:[{...f.intent.targets[0],files:[]}]}) : JSON.stringify(f.intent);
        if(options.revokeAfterInspection) f.enabled=false;
      } else if(params.sandboxPolicy.type==='readOnly') {
        raw=f.lastReport||report;
      } else {
        f.executionTurns++;
        const receiptRepair=options.repairOnlyBrowser&&f.executionTurns>1;
        if(!receiptRepair) {
          f.writes++;
          await writeFile(join(cwd,'work.html'),'<main>Portfolio <article>Expanded card</article></main>');
          items.push({id:'edit-'+id,type:'fileChange',status:'completed',changes:[{path:join(cwd,'work.html'),kind:{type:'update'}}]});
        }
        items.push({id:'verify-'+id,type:'commandExecution',command:receiptRepair?'check visual report':'read work.html',cwd,status:'completed',exitCode:0});
        for(const item of items) this.emit('event',{method:'item/completed',params:{threadId:f.threadId,turnId:id,item}});
        if(!options.noBrowserReceipts&&(!options.repairOnlyBrowser||receiptRepair)) {
          const startedAt=Date.now()+5;
          f.preview={url:'https://preview.example.test/work?k=fixture',localUrl:'http://127.0.0.1:3000/',revision:'result-sha',mode:'snapshot',expiresAt:Date.now()+60000};
          for(const target of f.intent.targets) {
            const checkId='browser-'+id+'-'+target.route;
            await f.store.addScreenshot(task.id,{png:Buffer.from('before-'+checkId),name:'before',checkId});
            if(options.extraShots) for(const name of ['hover','wait']) await f.store.addScreenshot(task.id,{png:Buffer.from(name+'-'+checkId),name,checkId,...(options.returnsToInitial ? { action:name } : {})});
            await f.store.addScreenshot(task.id,{png:Buffer.from((options.returnsToInitial?'before-':'after-')+checkId),name:'after',checkId});
            await f.store.update(task.id,data=>{
              data.preview={...f.preview,cwd,snapshotPath:join(root,'snapshot')};
              data.checks.push({id:checkId,expectedPath:target.route,passed:true,previewVersion:f.preview.revision,startedAt,finishedAt:startedAt+5,eventCount:f.activeState.executionEvents.length,steps:[{action:'hover',selector:'article',passed:true},{action:'assert',selector:'main',text:'Portfolio',passed:true}]});
            });
          }
        }
        raw=receiptRepair?report.replace(JSON.stringify(reportData),JSON.stringify({...reportData,verification:['read work.html','check visual report']})):report;
        f.lastReport=raw;
      }
      const agent={id:'report-'+id,type:'agentMessage',text:raw};
      const completed={id,status:'completed',items:[...items,agent]};f.completedTurns.push(completed);
      setImmediate(()=>{this.emit('event',{method:'item/completed',params:{threadId:f.threadId,turnId:id,item:agent}});this.emit('event',{method:'turn/completed',params:{threadId:f.threadId,turn:completed}});});
      return {turn:{id}};
    }
    send() {throw new Error('Unexpected approval');}
    close() {}
  }
  f.context={
    codex:{projects:async()=>[{id:task.project_id,roots:[{path:cwd}]}],call:async method=>{assert.equal(method,'thread/read');f.readCalls++;return {thread:thread()};}},
    api,stateDir,config:{},openDesktop:()=>{},onState:state=>{f.activeState=state;},
    save:async(id,state)=>{
      await writeFile(join(stateDir,id+'.json'),JSON.stringify(state));
      if(options.beforePublish&&!f.altered&&state.visualReviewed&&state.visualEvidence?.passed&&state.status==='running') {f.altered=true;await options.beforePublish(f);}
    },
    startRunner:async()=>new Runner(),prepareMaterial:async()=>material,wait:()=>new Promise(resolve=>setImmediate(resolve)),
    onIntent:intent=>f.store.update(task.id,data=>{data.intent=intent;}),visualFor:id=>f.store.read(id),previewFor:()=>f.preview,
    restorePreview:async id=>{f.restores++;f.preview={...(await f.store.read(id)).preview,expiresAt:Date.now()+60000};return f.preview;},
    renewPreview:async()=>{if(!f.preview) throw new Error('Preview missing');f.preview.expiresAt=Date.now()+60000;},
    upload:async(id,file,bytes)=>{if(options.failImageUpload&&!f.uploadFailed&&file.name.endsWith('.png')){f.uploadFailed=true;const error=new Error('temporary upload failure');error.retryable=true;throw error;}f.uploads.push({id,file,bytes});}
  };
  return f;
}

test('direct page work first performs read-only inspection, then delivers verified screenshots and a preview',async()=>{
  const f=await fixture();
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'completed',result.error);assert.equal(f.writes,1);assert.equal(f.turns.length,3);
  assert.equal(f.turns[0].sandboxPolicy.type,'readOnly');assert.equal(f.turns[0].approvalPolicy,'never');
  assert.equal(f.turns[1].sandboxPolicy.type,'workspaceWrite');assert.equal(f.turns[0].threadId,f.turns[1].threadId);
  assert.equal(f.turns[2].sandboxPolicy.type,'readOnly');assert.equal(f.turns[2].approvalPolicy,'never');
  assert.ok(f.turns[2].input.filter(item=>item.type==='localImage').length>=2,'final read-only audit must receive actual output screenshots');
  assert.equal(f.uploads.filter(upload=>upload.file.name.endsWith('.png')).length,2);
  assert.ok(f.uploads.some(upload=>upload.file.name==='work.html'));
  assert.equal(result.visualEvidence.passed,true);
});

test('query route inspection reaches execution and delivery for an existing product',async()=>{
  const f=await fixture({intent:{...visualIntent,targets:[{...visualIntent.targets[0],route:'/?view=product'}]}});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'completed',result.error); assert.equal(f.executionTurns,1);
  assert.equal(result.visualIntent.targets[0].route,'/?view=product');
  assert.equal(result.inspectionResults.length,1);
});

test('invalid inspection is repaired once in the same read-only session before execution',async()=>{
  const f=await fixture({invalidInspection:true});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'completed',result.error); assert.equal(result.inspectionResults.length,2);
  assert.ok(f.turns.slice(0,2).every(turn=>turn.sandboxPolicy.type==='readOnly'&&turn.approvalPolicy==='never'));
  assert.equal(f.turns[0].threadId,f.turns[1].threadId); assert.equal(f.executionTurns,1);
  assert.match(result.inspectionResults[0].error,/内部定位问题/);
});

test('persistent malformed inspection stops with a specific internal reason and zero edits',async()=>{
  const f=await fixture({invalidInspection:true,neverRepairInspection:true});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'blocked'); assert.equal(f.turns.length,2); assert.equal(f.writes,0);
  assert.match(result.error,/没有列出可修改的文件/); assert.match(result.error,/不要求你提供/);
});

test('cover-only motion material blocks before any write turn',async()=>{
  const f=await fixture({coverOnly:true});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'blocked');assert.match(result.error,/参考画面不足/);
  assert.equal(f.writes,0);assert.equal(f.turns.length,1);
  assert.match(await readFile(join(f.cwd,'work.html'),'utf8'),/Original portfolio/);
});

test('the final audit includes before and after screenshots for every target within the image budget',async()=>{
  const targets=['/work','/about','/news','/contact'].map(route=>({...visualIntent.targets[0],route}));
  const f=await fixture({intent:{...visualIntent,targets},extraShots:true});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'completed',result.error);
  const images=f.turns.at(-1).input.filter(item=>item.type==='localImage').map(item=>item.path),manifest=await f.store.read(f.task.id);
  assert.ok(images.length<=12);
  for(const target of targets) {
    const check=manifest.checks.findLast(check=>check.expectedPath===target.route),shots=manifest.screenshots.filter(shot=>shot.checkId===check.id);
    assert.ok(images.includes(shots[0].path),'missing initial state for '+target.route);
    assert.ok(images.includes(shots.at(-1).path),'missing final state for '+target.route);
  }
});

test('permission revoked at the end of inspection prevents the write turn',async()=>{
  const f=await fixture({revokeAfterInspection:true});
  const result=await processProjectTask(f.task,f.context);
  assert.notEqual(result.status,'completed');assert.equal(f.writes,0);
  assert.equal(f.turns.length,1);assert.match(result.error||result.report,/授权|取消/);
});

test('motion audit sees the changed middle state even when the last screenshot returns to the initial state',async()=>{
  const f=await fixture({extraShots:true,returnsToInitial:true});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'completed',result.error);
  const shots=(await f.store.read(f.task.id)).screenshots;
  const images=f.turns.at(-1).input.filter(item=>item.type==='localImage').map(item=>item.path);
  assert.equal(shots[0].sha256,shots.at(-1).sha256);
  assert.ok(images.includes(shots[0].path));
  assert.ok(images.includes(shots[1].path));
  assert.ok(images.includes(shots[2].path),'another distinct interaction state must fit the remaining image budget');
  assert.ok(!images.includes(shots.at(-1).path));
});

test('a visual file and command receipt without browser evidence cannot become completed',async()=>{
  const f=await fixture({noBrowserReceipts:true});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'blocked');assert.match(result.error,/浏览器|截图|预览/);
  assert.ok(f.turns.length<=3,'only one repair turn is allowed');assert.equal(f.uploads.length,0);
});

test('image upload failure resumes from durable receipts after restart without rerunning Codex edits',async()=>{
  const f=await fixture({failImageUpload:true});
  await assert.rejects(processProjectTask(f.task,f.context),/temporary upload failure/);
  const saved=JSON.parse(await readFile(join(f.stateDir,f.task.id+'.json'),'utf8'));
  assert.equal(saved.deliveryPending,true);assert.equal(saved.deliveryEvidence.passed,true);assert.equal(f.writes,1);
  f.store=new VisualStore(f.stateDir);f.preview=null;
  const result=await processProjectTask(f.task,f.context,true);
  assert.equal(result.status,'completed',result.error);assert.equal(f.writes,1);assert.equal(f.turns.length,3);
  assert.ok(f.restores>=1);assert.equal(f.uploads.filter(upload=>upload.file.name.endsWith('.png')).length,2);
  assert.equal(result.uploadedScreenshots.length,2);
});

test('delivery recovery keeps execution receipts from before a later visual repair turn',async()=>{
  const f=await fixture({repairOnlyBrowser:true,failImageUpload:true});
  await assert.rejects(processProjectTask(f.task,f.context),/temporary upload failure/);
  assert.equal(f.turns.length,4);assert.equal(f.writes,1);
  f.store=new VisualStore(f.stateDir);f.preview=null;
  const recovered=await processProjectTask(f.task,f.context,true);
  assert.equal(recovered.status,'completed',recovered.error);
  assert.equal(f.turns.length,4);assert.equal(f.writes,1);
  assert.ok(recovered.executionEvents.some(event=>event.command==='read work.html'));
  assert.ok(recovered.executionEvents.some(event=>event.command==='check visual report'));
});

test('recovery cannot publish a visual execution when its final read-only audit never ran',async()=>{
  const f=await fixture();
  await processProjectTask(f.task,f.context);
  // Recreate the durable state available if the process died after the write turn completed.
  f.completedTurns=f.completedTurns.slice(0,2);f.turns=f.turns.slice(0,2);f.uploads=[];
  const file=join(f.stateDir,f.task.id+'.json'),saved=JSON.parse(await readFile(file,'utf8'));
  Object.assign(saved,{stage:'execution',status:'running',turnId:f.completedTurns[1].id,turnRequested:true,visualReviewed:false,uploadedScreenshots:[],deliveryPending:false});
  delete saved.deliverables;
  await writeFile(file,JSON.stringify(saved));
  f.store=new VisualStore(f.stateDir);f.preview=null;
  const recovered=await processProjectTask(f.task,f.context,true);
  assert.equal(recovered.status,'completed',recovered.error);
  assert.equal(f.turns.length,3,'recovery must run the missing read-only audit');
  assert.equal(f.writes,1,'recovery must never repeat project edits');
  assert.equal(f.turns.at(-1).sandboxPolicy.type,'readOnly');
});

for(const failure of ['tampered','missing']) test('screenshot '+failure+' between validation and upload is blocked without transport retries',async()=>{
  const f=await fixture({beforePublish:async f=>{const shot=(await f.store.read(f.task.id)).screenshots[0];if(failure==='missing')await unlink(shot.path);else await writeFile(shot.path,'substituted bytes');}});
  const result=await processProjectTask(f.task,f.context);
  assert.equal(result.status,'blocked');assert.match(result.error,/截图/);
  assert.equal(f.uploads.length,0);assert.equal(f.writes,1);
});
