import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, unlink, utimes, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createHash } from 'node:crypto';
import { needsVisualInspection, parseVisualIntent, captureTargetFiles, assertTargetFilesUnchanged, validateVisualDelivery } from '../connector/visual-contract.mjs';

const digest=bytes=>createHash('sha256').update(bytes).digest('hex');
function intent(overrides={}) {
  return {outcome:'ready',summary:'为作品页复现展开卡片',effect:'悬停展开卡片',motion:true,referenceRequired:true,targets:[{route:'/work',purpose:'作品展示',files:['work.html'],expected:'悬停后卡片展开且作品标题可见'}],limitations:[],...overrides};
}
async function fixture() {
  const cwd=await mkdtemp(join(tmpdir(),'droprun-visual-contract-'));
  await writeFile(join(cwd,'work.html'),'<main>Portfolio</main>');
  const screenshots=[];
  for(const [name,bytes] of [['before.png',Buffer.from('initial screenshot')],['after.png',Buffer.from('expanded card screenshot')]]) {
    const path=join(cwd,name); await writeFile(path,bytes);
    screenshots.push({name,path,sha256:digest(bytes),checkId:'check-1'});
  }
  const check={id:'check-1',passed:true,expectedPath:'/work',previewVersion:'revision-1',startedAt:Date.now()+5,finishedAt:Date.now()+10,eventCount:0,steps:[{action:'hover',passed:true},{action:'assert',text:'Portfolio',passed:true}]};
  return {cwd,manifest:{checks:[check],screenshots},preview:{url:'https://preview.example.test/work?k=test',revision:'revision-1',expiresAt:Date.now()+60000},check};
}

test('the actual numbered-page effect request requires inspection before edits',()=>{
  assert.equal(needsVisualInspection({message:'把第2页替换成这个效果'}),true);
  assert.equal(needsVisualInspection({message:'replace page 2 with this effect'}),true);
  assert.equal(needsVisualInspection({message:'只解释这段函数，没有改动'}),false);
});

test('ready intent requires an exact local route and a nonempty target',()=>{
  assert.deepEqual(parseVisualIntent(JSON.stringify(intent())),intent());
  assert.throws(()=>parseVisualIntent(JSON.stringify(intent({targets:[]}))),/页面/);
  for(const route of ['', 'work','https://example.test/work','//example.test/work','/\\example.test/work','/work\n']) {
    assert.throws(()=>parseVisualIntent(JSON.stringify(intent({targets:[{...intent().targets[0],route}]}))),/路由|页面/);
  }
});

test('real product query routes and client hash routes are valid targets',()=>{
  for(const route of ['/?view=product','/search?q=loading&sort=recent','/#/search','/work?view=product#details']) {
    const result=parseVisualIntent(JSON.stringify(intent({targets:[{...intent().targets[0],route}]})));
    assert.equal(result.targets[0].route,route);
  }
});

test('blocked inspection preserves its specific explanation despite incomplete candidate targets',()=>{
  const blocked=intent({outcome:'blocked',summary:'作品页有两个候选，请说明是个人作品还是团队作品。',targets:[{route:'',purpose:'候选页',files:[],expected:''}]});
  assert.deepEqual(parseVisualIntent(JSON.stringify(blocked)),blocked);
  const incomplete=intent({targets:[{...intent().targets[0],files:[]}]});
  assert.throws(()=>parseVisualIntent(JSON.stringify(incomplete)),/内部定位问题/);
});

test('verified target and screenshot hashes pass only for the same live preview version',async()=>{
  const f=await fixture();
  assert.equal((await validateVisualDelivery(intent(),f.manifest,f.preview,{cwd:f.cwd})).passed,true);
  const changed=await validateVisualDelivery(intent(),f.manifest,{...f.preview,revision:'revision-2'},{cwd:f.cwd});
  assert.equal(changed.passed,false); assert.match(changed.errors.join(' '),/目标页面/);
  assert.equal((await validateVisualDelivery(intent(),f.manifest,{...f.preview,expiresAt:Date.now()-1},{cwd:f.cwd})).passed,false);
  assert.equal((await validateVisualDelivery(intent(),f.manifest,null,{cwd:f.cwd})).passed,false);
  assert.equal((await validateVisualDelivery(null,f.manifest,f.preview,{cwd:f.cwd})).passed,false);
  const anotherPage=intent({targets:[{...intent().targets[0],route:'/about'}]});
  assert.equal((await validateVisualDelivery(anotherPage,f.manifest,f.preview,{cwd:f.cwd})).passed,false);
});

test('missing or modified screenshots cannot be marked delivered',async()=>{
  const f=await fixture();
  await writeFile(f.manifest.screenshots[0].path,'changed after verification');
  const changed=await validateVisualDelivery(intent(),f.manifest,f.preview,{cwd:f.cwd});
  assert.equal(changed.passed,false); assert.match(changed.errors.join(' '),/截图已改变/);
  await unlink(f.manifest.screenshots[1].path);
  const missing=await validateVisualDelivery(intent(),f.manifest,f.preview,{cwd:f.cwd});
  assert.equal(missing.passed,false); assert.match(missing.errors.join(' '),/截图文件不可读/);
  assert.equal((await validateVisualDelivery(intent(),{...f.manifest,screenshots:[]},f.preview,{cwd:f.cwd})).passed,false);
});

test('motion delivery needs a real action or timing step plus an assertion',async()=>{
  const f=await fixture();
  f.check.steps=[{action:'assert',text:'Portfolio',passed:true}];
  assert.match((await validateVisualDelivery(intent(),f.manifest,f.preview,{cwd:f.cwd})).errors.join(' '),/触发|时序/);
  f.check.steps=[{action:'hover',passed:true}];
  assert.match((await validateVisualDelivery(intent(),f.manifest,f.preview,{cwd:f.cwd})).errors.join(' '),/断言/);
});

test('file changes after browser verification require another check, including edits within one second',async()=>{
  const f=await fixture();
  const events=[{id:'new-edit',type:'files',status:'completed',changes:[{path:'work.html'}]}];
  const recorded=await validateVisualDelivery(intent(),f.manifest,f.preview,{cwd:f.cwd,events});
  assert.equal(recorded.passed,false); assert.match(recorded.errors.join(' '),/修改/);
  await writeFile(join(f.cwd,'work.html'),'<main>Unverified change</main>');
  const changedAt=new Date(f.check.finishedAt+500);
  await utimes(join(f.cwd,'work.html'),changedAt,changedAt);
  const outsideEditor=await validateVisualDelivery(intent(),f.manifest,f.preview,{cwd:f.cwd,artifacts:[{path:'work.html'}]});
  assert.equal(outsideEditor.passed,false,'a subsecond external edit must not reuse older browser evidence');
});

test('target file fingerprint prevents external edits or new-file collisions before execution',async()=>{
  const f=await fixture();
  const targets=intent({targets:[{...intent().targets[0],files:['work.html','new.html']}]});
  const baseline=await captureTargetFiles(f.cwd,targets);
  assert.equal(baseline['work.html'],digest(await readFile(join(f.cwd,'work.html'))));
  assert.equal(baseline['new.html'],null);
  await assertTargetFilesUnchanged(f.cwd,baseline);
  await writeFile(join(f.cwd,'new.html'),'written by another editor');
  await assert.rejects(assertTargetFilesUnchanged(f.cwd,baseline),/其他操作修改/);
  await unlink(join(f.cwd,'new.html'));
  await writeFile(join(f.cwd,'work.html'),'external edit');
  await assert.rejects(assertTargetFilesUnchanged(f.cwd,baseline),/其他操作修改/);
  await assert.rejects(captureTargetFiles(f.cwd,intent({targets:[{...intent().targets[0],files:['../private.txt']}]})),/路径/);
});
