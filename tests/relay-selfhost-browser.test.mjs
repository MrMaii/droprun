import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHash, randomUUID } from 'node:crypto';
import { createServer } from 'node:http';
import { readFile, mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Miniflare } from './miniflare.mjs';
import { findBrowser, verifyVisual } from '../connector/browser.mjs';

const executable = findBrowser();
const digest = value => createHash('sha256').update(value).digest('hex');

test('real browser runs isolated snapshot HTML, CSS and module imports without sharing cookies', { skip: !executable, timeout: 60000 }, async t => {
  const mf = new Miniflare({modules:true,scriptPath:'relay/preview-worker.mjs',compatibilityDate:'2026-09-01',d1Databases:['DB'],r2Buckets:['FILES'],bindings:{INSTANCE_ID:randomUUID()}});
  const profileDir = await mkdtemp(join(tmpdir(),'droprun-snapshot-browser-'));
  let server;
  t.after(async () => {
    if (server) { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
    await mf.dispose();
    await rm(profileDir,{recursive:true,force:true,maxRetries:10,retryDelay:100});
  });
  const db = await mf.getD1Database('DB'), bucket = await mf.getR2Bucket('FILES');
  for (const sql of (await readFile('relay/schema.sql','utf8')).split(/;\s*(?:\r?\n|$)/).filter(sql => sql.trim())) await db.prepare(sql).run();
  const device = randomUUID(), project = randomUUID(), task = randomUUID(), snapshot = randomUUID(), token = 'a'.repeat(64), now = Date.now();
  await db.prepare('INSERT INTO devices(id,token_hash,created_at) VALUES(?,?,?)').bind(device,digest('synthetic-phone'),now).run();
  await db.prepare('INSERT INTO project_permissions VALUES(?,?,1,?,?)').bind(device,project,'permission',now).run();
  await db.prepare("INSERT INTO tasks(id,device_id,project_id,project_name,content,message,status,permission_version,created_at,updated_at) VALUES(?,?,?,'Fixture','Fixture','Fixture','completed','permission',?,?)").bind(task,device,project,now,now).run();
  await db.prepare('INSERT INTO preview_snapshots(id,task_id,version,manifest,created_at,expires_at,ready) VALUES(?,?,?,?,?,?,1)').bind(snapshot,task,'browser-version',JSON.stringify({entryPath:'docs/index.html',spa:false}),now,now+86400000).run();
  await db.prepare('INSERT INTO preview_sessions VALUES(?,?,?)').bind(digest(token),snapshot,now+60000).run();
  const files = [
    ['docs/index.html','text/html','<!doctype html><title>Stored snapshot</title><link rel="icon" href="data:,"><link rel="stylesheet" href="/style.css"><h1 id="state">Loading</h1><p id="isolation">Checking</p><p id="unicode">灵感 · Progress</p><script type="module" src="/app.mjs"></script>'],
    ['style.css','text/css','body{background:rgb(240, 246, 231);color:rgb(18, 34, 24)}'],
    ['app.mjs','text/javascript',"import { label } from './chunk.mjs'; document.querySelector('#state').textContent=label; try { document.cookie; document.querySelector('#isolation').textContent='Shared origin'; } catch { document.querySelector('#isolation').textContent='Isolated origin'; }"],
    ['chunk.mjs','text/javascript',"export const label='Module loaded';"]
  ];
  for (const [path,type,body] of files) {
    await db.prepare('INSERT INTO preview_files VALUES(?,?,?,?,?,1)').bind(snapshot,path,digest(body),Buffer.byteLength(body),type).run();
    await bucket.put('snapshots/'+snapshot+'/'+path,body,{httpMetadata:{contentType:type}});
  }
  const requests = [];
  server = createServer(async (req,res) => {
    requests.push(req.url);
    try {
      const response = await mf.dispatchFetch('https://preview.test'+req.url,{method:req.method,headers:req.headers});
      res.writeHead(response.status,Object.fromEntries(response.headers));
      res.end(Buffer.from(await response.arrayBuffer()));
    } catch { res.writeHead(500); res.end('Fixture bridge failed'); }
  });
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  const origin = 'http://127.0.0.1:'+server.address().port;
  const path = '/s/'+token+'/docs';
  const result = await verifyVisual({executable,profileDir,url:origin+path,expectedPath:path,wait:100,steps:[
    {action:'assert',selector:'#state',text:'Module loaded',wait:0},
    {action:'assert',selector:'body',computedStyle:{'background-color':'rgb(240, 246, 231)'},wait:0},
    {action:'assert',selector:'#isolation',text:'Isolated origin',wait:0}
    ,{action:'assert',selector:'#unicode',text:'灵感 · Progress',wait:0}
  ]});
  assert.equal(result.passed,true,JSON.stringify(result.errors));
  assert.ok(result.screenshots.length >= 4);
  for (const name of ['style.css','app.mjs','chunk.mjs']) assert.ok(requests.includes('/s/'+token+'/'+name),name+' must keep its capability scope');
  const cookieOnly = await fetch(origin+'/s/'+'b'.repeat(64)+'/docs',{headers:{Cookie:'droprun_preview='+token}});
  assert.equal(cookieOnly.status,410,'An unrelated cookie cannot authorize another snapshot URL');
  await db.prepare('UPDATE preview_sessions SET expires_at=1 WHERE token_hash=?').bind(digest(token)).run();
  assert.equal((await fetch(origin+path)).status,410);
});
