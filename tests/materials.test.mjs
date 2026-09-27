import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { prepare, run, fetchPage, describeMaterial, renderBrief, assessVisualEvidence, samplingPlan } from '../connector/materials.mjs';

test('a one-second silent video yields a timed overview without claiming motion evidence', async()=>{
  const dir=await mkdtemp(join(tmpdir(),'droprun-media-test-'));
  const source=join(dir,'short.mp4');
  await run('ffmpeg',['-nostdin','-hide_banner','-loglevel','error','-f','lavfi','-i','color=c=green:s=320x240:d=1','-c:v','libx264',source],dir);
  const bytes=await readFile(source);
  const api=async()=>new Response(bytes);
  const task={content:'Short silent video fixture',assets:JSON.stringify([{id:'test-asset',name:'short.mp4',mime:'video/mp4'}])};
  const material=await prepare(task,join(dir,'material'),api,{});
  assert.equal(material.coverage.visual,'partial');
  assert.equal(material.coverage.video,'downloaded');
  assert.equal(material.frames,2); assert.equal(material.sheets.length,1);
  assert.deepEqual(material.images,material.sheets);
  assert.equal(material.coverage.audio,'missing');
  assert.match(material.summary,/视频 0:01/); assert.match(material.summary,/2 张关键帧/);
  assert.match(material.brief,/画面：已提取 2 张时序帧/); assert.match(material.brief,/分享的文字：/);
  const evidence=material.visualEvidence.videos[0];
  assert.equal(evidence.durationSeconds,1); assert.equal(evidence.processedSeconds,1);
  assert.deepEqual(evidence.overview.frames.map(frame=>frame.timeSeconds),[0,0.5]);
  assert.equal(evidence.uniqueFrameCount,1); assert.equal(evidence.motionWindows.length,0);
  assert.equal(assessVisualEvidence(material).sufficient,true);
  assert.equal(assessVisualEvidence(material,{motion:true}).sufficient,false);
  assert.equal(await readFile(join(dir,'material','material.md'),'utf8'),material.brief);
});

test('links the extractor cannot fetch fall back to page metadata and a cover image', async()=>{
  const dir=await mkdtemp(join(tmpdir(),'droprun-page-test-'));
  const html='<html><head><title>Fallback title</title><meta property="og:title" content="Reel &amp; friends"><meta name="description" content="Meta description"><meta property="og:image" content="https://cdn.example.test/cover.jpg"></head></html>';
  const fetchImpl=async(url)=>String(url).includes('cover.jpg')?new Response(new Uint8Array([255,216,255]),{headers:{'content-type':'image/jpeg'}}):new Response(html,{headers:{'content-type':'text/html'}});
  const identity=async value=>new URL(value);
  const page=await fetchPage(new URL('https://example.test/reel/1'),dir,1,fetchImpl,identity);
  assert.equal(page.title,'Reel & friends'); assert.equal(page.description,'Meta description');
  assert.ok(page.image.endsWith('page-1-cover.jpg')); assert.equal((await readFile(page.image)).length,3);
  const task={content:'看看这个 https://example.test/reel/1',assets:'[]'};
  const material=await prepare(task,join(dir,'material'),async()=>{throw new Error('no api');},{},{fetchImpl,checkUrl:identity});
  assert.equal(material.pages.length,1); assert.equal(material.digest.title,'Reel & friends');
  assert.match(material.summary,/网页信息/); assert.match(material.summary,/Reel & friends/);
  assert.match(material.brief,/链接：https:\/\/example.test\/reel\/1/); assert.match(material.brief,/简介：Meta description/);
  assert.match(material.brief,/分享时附带的文字：看看这个/);
  assert.ok(material.files.some(f=>f.endsWith('page-1-cover.jpg')));
  assert.deepEqual(material.visualEvidence.coverImages,material.images);
  assert.equal(assessVisualEvidence(material).sufficient,true);
  assert.deepEqual(assessVisualEvidence(material,{motion:true}),{sufficient:false,kind:'static',reasons:['没有取得可读取的原视频；链接、标题、字幕和封面不足以复现动效。']});
});

async function movingVideo() {
  const dir=await mkdtemp(join(tmpdir(),'droprun-motion-test-'));
  const source=join(dir,'motion.mp4');
  // A brief yellow panel appears and vanishes against blue, with a second change later.
  await run('ffmpeg',['-nostdin','-hide_banner','-loglevel','error','-f','lavfi','-i','color=c=blue:s=320x240:r=24:d=6','-vf',"drawbox=x=40:y=40:w=240:h=160:color=yellow:t=fill:enable='between(t,2,2.5)+between(t,4.5,5)'",'-c:v','libx264',source],dir);
  const bytes=await readFile(source);
  const task={content:'把第二页替换成视频中的展开动效',assets:JSON.stringify([{id:'motion',name:'motion.mp4',mime:'video/mp4'}])};
  return {dir,bytes,task,api:async()=>new Response(bytes)};
}

test('real video changes get bounded dense temporal windows, traceable timestamps and represented sheets',async()=>{
  const {dir,task,api}=await movingVideo();
  const material=await prepare(task,join(dir,'material'),api,{});
  const evidence=material.visualEvidence.videos[0];
  assert.equal(evidence.decoded,true); assert.equal(evidence.durationSeconds,6);
  assert.equal(evidence.processedSeconds,6); assert.equal(evidence.truncated,false);
  assert.equal(evidence.overview.frames.length,12);
  assert.ok(evidence.motionWindows.length>=1&&evidence.motionWindows.length<=2);
  assert.ok(evidence.motionWindows.some(window=>window.startSeconds<=2&&window.endSeconds>=2.5));
  assert.ok(evidence.motionWindows.every(window=>window.intervalSeconds===0.125&&window.frames.length<=16));
  assert.ok(evidence.motionWindows.some(window=>new Set(window.frames.map(frame=>frame.sha256)).size>=2));
  for(const sequence of [evidence.overview,...evidence.motionWindows]) {
    assert.ok(sequence.sheets.every(sheet=>material.images.includes(sheet)));
    for(const frame of sequence.frames) { assert.equal((await readFile(frame.path))[0],0xff); assert.match(frame.sha256,/^[a-f\d]{64}$/); }
  }
  assert.ok(material.frames<=44); assert.ok(material.images.length<=5);
  assert.deepEqual(assessVisualEvidence(material,{motion:true}),{sufficient:true,kind:'motion',reasons:[]});
  assert.match(material.brief,/每 0\.125 秒一帧/); assert.match(material.brief,/0s、0\.5s/);
  assert.match(material.brief,/非逐帧完整观看/);
  const saved=JSON.parse(await readFile(join(dir,'material','material.json'),'utf8'));
  assert.deepEqual(saved.visualEvidence,material.visualEvidence);
  // The image budget is part of coverage: on-disk frames alone cannot claim the model saw them.
  const omitted={...material,images:material.images.filter(image=>!evidence.motionWindows.flatMap(window=>window.sheets).includes(image))};
  assert.equal(assessVisualEvidence(omitted,{motion:true}).sufficient,false);
});

test('duration truncation and ffmpeg failure block motion while preserving honest fallback',async()=>{
  const {dir,task,api}=await movingVideo();
  const truncated=await prepare(task,join(dir,'truncated'),api,{maxVideoMinutes:0.05});
  assert.equal(truncated.visualEvidence.videos[0].processedSeconds,3);
  assert.equal(truncated.visualEvidence.videos[0].truncated,true);
  assert.equal(assessVisualEvidence(truncated,{motion:true}).sufficient,false);
  assert.ok(assessVisualEvidence(truncated,{motion:true}).reasons.some(reason=>reason.includes('未完整采样')));
  const failed=await prepare(task,join(dir,'failed'),api,{ffmpeg:join(dir,'missing-ffmpeg.exe')});
  assert.equal(failed.coverage.video,'downloaded'); assert.equal(failed.frames,0);
  assert.equal(failed.visualEvidence.videos[0].processedSeconds,0);
  assert.equal(assessVisualEvidence(failed,{motion:true}).sufficient,false);
  assert.match(failed.brief,/关键帧处理失败/);
  const unknown=await prepare(task,join(dir,'unknown'),api,{ffprobe:join(dir,'missing-ffprobe.exe')});
  assert.equal(unknown.visualEvidence.videos[0].durationSeconds,null);
  assert.equal(assessVisualEvidence(unknown,{motion:true}).sufficient,false);
});

test('Instagram extraction failure records cover-only evidence and cannot pass a motion task',async()=>{
  const dir=await mkdtemp(join(tmpdir(),'droprun-login-test-'));
  const fetchImpl=async(url)=>String(url).includes('cover.jpg')?new Response(new Uint8Array([255,216,255]),{headers:{'content-type':'image/jpeg'}}):new Response('<meta property="og:title" content="Effect demo"><meta property="og:image" content="https://cdn.example.test/cover.jpg">');
  const material=await prepare({content:'https://www.instagram.com/reel/example',assets:'[]'},dir,async()=>{throw new Error('no api');},{python:join(dir,'missing-python.exe')},{fetchImpl,checkUrl:async value=>new URL(value)});
  assert.equal(material.coverage.video,'missing'); assert.equal(material.frames,0);
  assert.equal(material.visualEvidence.videos.length,0); assert.equal(material.visualEvidence.coverImages.length,1);
  assert.equal(assessVisualEvidence(material,{motion:true}).sufficient,false);
  assert.match(material.brief,/视频未完整取得/);
});

test('sampling bounds stay explicit and missing evidence never grants motion',()=>{
  assert.deepEqual(samplingPlan(12.5),{interval:0.5,frames:25,covered:12.5,truncated:false});
  assert.equal(samplingPlan(60).frames,60); assert.equal(samplingPlan(600).frames,60);
  assert.equal(samplingPlan(4000).covered,1800); assert.equal(samplingPlan(4000).truncated,true);
  assert.equal(samplingPlan(0).truncated,true);
  assert.equal(assessVisualEvidence({coverage:{video:'downloaded',visual:'partial'},images:['cover.jpg']},{motion:true}).sufficient,false);
});

test('each video keeps its own fractional duration, and a preparation retry does not reuse stale frames',async()=>{
  const {dir,task,bytes}=await movingVideo();
  const short=join(dir,'short.mp4');
  await run('ffmpeg',['-nostdin','-hide_banner','-loglevel','error','-f','lavfi','-i','color=c=green:s=320x240:r=25:d=1.2','-c:v','libx264',short],dir);
  const shortBytes=await readFile(short), materialDir=join(dir,'material');
  await prepare(task,materialDir,async()=>new Response(bytes),{});
  const retried=await prepare(task,materialDir,async()=>new Response(shortBytes),{});
  assert.equal(retried.visualEvidence.videos[0].durationSeconds,1.2);
  assert.equal(retried.visualEvidence.videos[0].processedSeconds,1.2);
  assert.deepEqual(retried.visualEvidence.videos[0].overview.frames.map(frame=>frame.timeSeconds),[0,0.5,1]);
  assert.equal(retried.visualEvidence.videos[0].motionWindows.length,0);
  assert.equal(retried.frames,3); assert.equal(assessVisualEvidence(retried,{motion:true}).sufficient,false);
  const combined=await prepare({...task,assets:JSON.stringify([{id:'a',name:'short.mp4',mime:'video/mp4'},{id:'b',name:'long.mp4',mime:'video/mp4'}])},join(dir,'combined'),async path=>new Response(path.endsWith('/a')?shortBytes:bytes),{});
  assert.deepEqual(combined.visualEvidence.videos.map(video=>video.durationSeconds),[1.2,6]);
  assert.deepEqual(combined.visualEvidence.videos.map(video=>video.processedSeconds),[1.2,6]);
});

test('summary and brief describe what was actually read, including transcripts and limits', ()=>{
  const material={source:'https://x.test/1',dir:'D:/m',files:[],attachments:[],urls:['https://x.test/1'],pages:[],frames:6,limitations:['关键帧每 10 秒一张'],digest:{title:'A very long title that keeps going and going beyond forty characters',description:'desc',author:'someone',duration:92,transcript:'hello world',subtitles:''},coverage:{text:'partial',video:'downloaded',visual:'partial',audio:'transcribed'}};
  const summary=describeMaterial(material);
  assert.match(summary,/^视频 1:32 · 已转写 · 6 张关键帧 · 「A very long title/); assert.match(summary,/…」$/);
  const brief=renderBrief(material);
  assert.match(brief,/作者：someone/); assert.match(brief,/时长：1:32/); assert.match(brief,/语音文字稿[\s\S]*hello world/); assert.match(brief,/读取范围说明：\n- 关键帧每 10 秒一张/);
  assert.match(describeMaterial({...material,urls:[],source:'plain text',frames:0,coverage:{text:'shared_text_only',video:'missing',visual:'missing',audio:'missing'},digest:{title:''}}),/^文字$/);
});
