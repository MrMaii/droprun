import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare } from './miniflare.mjs';
import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
const connector='pairing-test-connector-secret',legacy='LEGACY-PAIR-ONLY';
const hash=s=>createHash('sha256').update(s).digest('hex');
let mf,db;
async function req(path,body,token='',method=body?'POST':'GET') {
  const r=await mf.dispatchFetch('https://droprun.test'+path,{method,headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
  return {status:r.status,data:await r.json(),cache:r.headers.get('Cache-Control')};
}
const issue=()=>req('/connector/pairing-code',{},connector);
before(async()=>{
  mf=new Miniflare({modules:true,scriptPath:'relay/worker.mjs',compatibilityDate:'2026-08-06',d1Databases:['DB'],r2Buckets:['FILES'],bindings:{CONNECTOR_HASH:hash(connector),PAIR_HASH:hash(legacy)}});
  db=await mf.getD1Database('DB');
  for(const sql of (await readFile('relay/schema.sql','utf8')).split(/;\s*(?:\r?\n|$)/).filter(s=>s.trim()))await db.prepare(sql).run();
});
after(async()=>{await mf?.dispose();});
test('pairing code is single-use under parallel redemption, hashed, and expires in ten minutes',async()=>{
  const start=Date.now(),issued=await issue();assert.equal(issued.status,200);assert.equal(issued.cache,'no-store');
  assert.match(issued.data.code,/^[0-9A-F]{5}(?:-[0-9A-F]{5}){3}$/);
  assert.ok(issued.data.expiresAt>=start+600000 && issued.data.expiresAt<=Date.now()+600000);
  const row=await db.prepare('SELECT * FROM pairing_codes').first();
  assert.equal(row.code_hash,hash(issued.data.code.replaceAll('-','')));assert.equal(JSON.stringify(row).includes(issued.data.code),false);
  const results=await Promise.all(Array.from({length:8},()=>req('/pair',{code:issued.data.code.toLowerCase()})));
  assert.equal(results.filter(r=>r.status===200).length,1);assert.equal(results.filter(r=>r.status===403).length,7);
  assert.equal((await db.prepare('SELECT COUNT(*) AS n FROM devices').first()).n,1);
  const phone=results.find(r=>r.status===200).data;
  assert.equal((await req('/connector/pairing-code',{},phone.token)).status,403);
  assert.equal((await req('/connector/pairing-code',null,connector)).data.available,false);
  assert.equal((await req('/pair',{code:issued.data.code})).status,403);
  assert.equal((await req('/device/revoke',{},phone.token)).status,200);
  assert.equal((await req('/pair',{code:issued.data.code})).status,403);
});
test('expired, replaced, revoked and legacy codes cannot create devices; old phones remain paired',async()=>{
  assert.equal((await req('/connector/pairing-code',{})).status,401);
  assert.equal((await req('/pair',{code:legacy})).status,403);
  const first=(await issue()).data,phone=(await req('/pair',{code:first.code})).data;
  const expired=(await issue()).data;
  await db.prepare('UPDATE pairing_codes SET expires_at=?').bind(Date.now()-1).run();
  assert.equal((await req('/pair',{code:expired.code,expiresAt:Date.now()+9999999})).status,403);
  const replaced=(await issue()).data,current=(await issue()).data;
  assert.notEqual(replaced.code,current.code);assert.equal((await req('/pair',{code:replaced.code})).status,403);
  assert.equal((await req('/connector/pairing-code',null,connector,'DELETE')).status,200);
  assert.equal((await req('/pair',{code:current.code})).status,403);
  assert.equal((await req('/projects',null,phone.token)).status,200);
  assert.equal((await db.prepare('SELECT COUNT(*) AS n FROM devices').first()).n,1);
  assert.equal((await db.prepare('SELECT COUNT(*) AS n FROM pairing_codes').first()).n,1);
});
test('device insert failure rolls back code consumption',async()=>{
  const issued=(await issue()).data;
  await db.prepare("CREATE TRIGGER fail_pair BEFORE INSERT ON devices BEGIN SELECT RAISE(ABORT,'synthetic insert failure'); END").run();
  assert.equal((await req('/pair',{code:issued.code})).status,400);
  assert.equal((await db.prepare('SELECT consumed_at FROM pairing_codes').first()).consumed_at,null);
  await db.prepare('DROP TRIGGER fail_pair').run();
  assert.equal((await req('/pair',{code:issued.code})).status,200);
});
