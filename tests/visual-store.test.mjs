import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { createHash } from 'node:crypto';
import { VisualStore } from '../connector/visual-store.mjs';

test('manifest and screenshots survive a new store instance for later delivery retries',async()=>{
  const stateDir=await mkdtemp(join(tmpdir(),'droprun-visual-store-'));
  const store=new VisualStore(stateDir), png=Buffer.from('executor-owned screenshot');
  const shot=await store.addScreenshot('task-1',{png,name:'after',checkId:'check-1'});
  await store.update('task-1',data=>{
    data.intent={targets:[{route:'/work'}]};
    data.preview={revision:'revision-1',mode:'snapshot'};
    data.checks.push({id:'check-1',passed:true});
  });
  const recovered=await new VisualStore(stateDir).read('task-1');
  assert.equal(recovered.screenshots.length,1); assert.equal(recovered.checks.length,1);
  assert.deepEqual(recovered.preview,{revision:'revision-1',mode:'snapshot'});
  assert.equal(recovered.screenshots[0].path,shot.path);
  assert.equal(shot.sha256,createHash('sha256').update(png).digest('hex'));
  assert.deepEqual(await readFile(shot.path),png);
});

test('concurrent screenshot and check writes retain every receipt without torn JSON',async()=>{
  const stateDir=await mkdtemp(join(tmpdir(),'droprun-visual-concurrency-'));
  const store=new VisualStore(stateDir);
  await Promise.all(Array.from({length:20},async(_,index)=>{
    const shot=await store.addScreenshot('task-many',{png:Buffer.from('screenshot-'+index),name:'state',checkId:'check-'+index});
    await store.update('task-many',async data=>{ await Promise.resolve(); data.checks.push({id:'check-'+index,shot:shot.name}); });
  }));
  const saved=await new VisualStore(stateDir).read('task-many');
  assert.equal(saved.screenshots.length,20); assert.equal(saved.checks.length,20);
  assert.equal(new Set(saved.screenshots.map(shot=>shot.name)).size,20);
  assert.equal(new Set(saved.checks.map(check=>check.id)).size,20);
  assert.ok(saved.checks.every(check=>saved.screenshots.some(shot=>shot.name===check.shot)));
});

test('failed mutation keeps the committed manifest and does not poison the next writer',async()=>{
  const stateDir=await mkdtemp(join(tmpdir(),'droprun-visual-failure-'));
  const store=new VisualStore(stateDir);
  await store.update('task-failure',data=>{data.preview={revision:'good'};});
  await assert.rejects(store.update('task-failure',data=>{data.preview.revision='uncommitted';throw new Error('interrupted');}),/interrupted/);
  assert.equal((await store.read('task-failure')).preview.revision,'good');
  await store.update('task-failure',data=>{data.checks.push({id:'retry'});});
  const recovered=await new VisualStore(stateDir).read('task-failure');
  assert.equal(recovered.preview.revision,'good'); assert.equal(recovered.checks[0].id,'retry');
  await writeFile(join(store.dir('task-failure'),'manifest.json.tmp'),'{partial crash write');
  assert.deepEqual(await new VisualStore(stateDir).read('task-failure'),recovered);
});

test('receipt filenames stay in task storage and caller metadata cannot forge path or hash',async()=>{
  const stateDir=await mkdtemp(join(tmpdir(),'droprun-visual-path-'));
  const store=new VisualStore(stateDir), png=Buffer.from('real bytes');
  const shot=await store.addScreenshot('safe-task',{png,name:'../../outside',path:'elsewhere.png',sha256:'forged',capturedAt:0});
  assert.equal(dirname(shot.path),store.dir('safe-task'));
  assert.match(shot.name,/^outside-[a-f\d-]+\.png$/); assert.notEqual(shot.sha256,'forged'); assert.ok(shot.capturedAt>0);
  for(const id of ['../escape','a/b','a\\b','', 'a'.repeat(81)]) assert.throws(()=>store.dir(id),/Invalid task ID/);
});
