import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare } from './miniflare.mjs';
import { readFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';
const hash=x=>createHash('sha256').update(x).digest('hex');
const connector='delivery-connector-token-long',phone='delivery-phone-token-long',other='delivery-other-token-long';
let mf,db;
const owner=randomUUID(), stranger=randomUUID(), project=randomUUID();
async function request(path,token=phone,options={}) { return mf.dispatchFetch('https://droprun.test'+path,{...options,headers:{Authorization:'Bearer '+token,...options.headers}}); }
async function task() {
  const id=randomUUID();
  await db.prepare("INSERT INTO tasks (id,device_id,project_id,project_name,content,message,created_at,updated_at,status,permission_version) VALUES (?,?,?,'Test','Test','Test',0,0,'running','v1')").bind(id,owner,project).run();return id;
}
const artifactId='a'.repeat(64);
async function put(id,bytes='real artifact',options={}) {
  return request('/connector/tasks/'+id+'/deliverables/'+artifactId,options.token||connector,{method:'PUT',body:bytes,headers:{'X-Filename':encodeURIComponent(options.name||'report.txt'),'X-Kind':options.kind||'artifact','X-Sha256':options.sha256||hash(bytes),'X-Size':String(options.size??Buffer.byteLength(bytes))}});
}
before(async()=>{
  mf=new Miniflare({modules:true,scriptPath:'relay/worker.mjs',compatibilityDate:'2026-08-06',d1Databases:['DB'],r2Buckets:['FILES'],bindings:{CONNECTOR_HASH:hash(connector)}});
  db=await mf.getD1Database('DB');
  for(const sql of (await readFile('relay/schema.sql','utf8')).split(/;\s*(?:\r?\n|$)/).filter(x=>x.trim()))await db.prepare(sql).run();
  await db.prepare('INSERT INTO devices (id,token_hash,created_at) VALUES (?,?,0),(?,?,0)').bind(owner,hash(phone),stranger,hash(other)).run();
  await db.prepare('INSERT INTO project_permissions VALUES (?,?,1,?,0)').bind(owner,project,'v1').run();
});
after(async()=>{await mf?.dispose();});
test('connector uploads once; only owning phone can list/download immutable exact bytes',async()=>{
  const id=await task();assert.equal((await put(id)).status,200);
  assert.equal((await put(id)).status,200);
  assert.equal((await put(id,'changed')).status,409);
  const list=await (await request('/tasks/'+id+'/deliverables')).json();assert.equal(list.deliverables.length,1);
  assert.equal(list.deliverables[0].sha256,hash('real artifact'));
  const route='/tasks/'+id+'/deliverables/'+artifactId,r=await request(route);
  assert.equal(r.status,200);assert.equal(await r.text(),'real artifact');assert.equal(r.headers.get('Cache-Control'),'no-store');assert.equal(r.headers.get('X-Content-Type-Options'),'nosniff');
  assert.equal((await request(route,other)).status,404);assert.equal((await request('/tasks/'+id+'/deliverables',other)).status,404);
  assert.equal((await request(route,'')).status,401);assert.equal((await put(id,'real artifact',{token:phone})).status,403);
  assert.equal((await request('/tasks/'+randomUUID()+'/deliverables/'+artifactId)).status,404);
});
test('bad digest, size, path and kind never become downloadable',async()=>{
  for(const option of [{sha256:'0'.repeat(64)},{size:1},{size:50*1024*1024+1},{name:'../secret'},{name:'.env'},{kind:'script'}]) {
    const id=await task();assert.notEqual((await put(id,'real artifact',option)).status,200);
    assert.equal((await request('/tasks/'+id+'/deliverables/'+artifactId)).status,404);
  }
});
test('zero-byte files are valid and remain hash checked',async()=>{
  const id=await task();assert.equal((await put(id,'')).status,200);assert.equal(await (await request('/tasks/'+id+'/deliverables/'+artifactId)).text(),'');
});
test('uploads stop after cancellation, terminal state or authorization-generation change',async()=>{
  for(const sql of ["status='completed'","cancel_requested=1","permission_version='old'"]) {
    const id=await task();await db.prepare('UPDATE tasks SET '+sql+' WHERE id=?').bind(id).run();
    assert.equal((await put(id)).status,409);
  }
});
test('task deletion removes its deliverables without removing another task artifact',async()=>{
  const id=await task(),kept=await task();await put(id);await put(kept);
  await db.prepare("UPDATE tasks SET status='completed' WHERE id=?").bind(id).run();
  assert.equal((await request('/tasks/'+id,other,{method:'DELETE'})).status,404);
  assert.equal((await request('/tasks/'+id,phone,{method:'DELETE'})).status,200);
  assert.equal((await request('/tasks/'+id+'/deliverables/'+artifactId)).status,404);
  assert.equal(await (await mf.getR2Bucket('FILES')).get('deliverables/'+id+'/'+artifactId+'/'+hash('real artifact')),null);
  assert.equal((await request('/tasks/'+kept+'/deliverables/'+artifactId)).status,200);
});
test('revoked device cannot download previously delivered files',async()=>{
  const token='temporary-delivery-phone-token',device=randomUUID(),id=randomUUID();
  await db.prepare('INSERT INTO devices (id,token_hash,created_at) VALUES (?,?,0)').bind(device,hash(token)).run();
  await db.prepare('INSERT INTO project_permissions VALUES (?,?,1,?,0)').bind(device,project,'v1').run();
  await db.prepare("INSERT INTO tasks (id,device_id,project_id,project_name,content,message,created_at,updated_at,status,permission_version) VALUES (?,?,?,'Test','Test','Test',0,0,'running','v1')").bind(id,device,project).run();
  await put(id);assert.equal((await request('/tasks/'+id+'/deliverables/'+artifactId,token)).status,200);
  assert.equal((await request('/device/revoke',token,{method:'POST',body:'{}'})).status,200);
  assert.equal((await request('/tasks/'+id+'/deliverables/'+artifactId,token)).status,401);
});
