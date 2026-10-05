import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';
import { readFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';
import { cleanupRetention } from '../relay/selfhost.mjs';

const digest = value => createHash('sha256').update(value).digest('hex');
const connector = 'selfhost-test-connector-token-long';
const admin = 'selfhost-test-admin-token-separate';
const instanceId = randomUUID();
const projectId = randomUUID();
let mf, db, bucket, previewWorker;
const sqlParts = source => source.split(/;\s*(?:\r?\n|$)/).filter(sql => sql.trim());
async function req(path, body, token, method = body === undefined ? 'GET' : 'POST') {
  const response = await mf.dispatchFetch('https://relay.test'+path,{method,headers:{Authorization:'Bearer '+(token || ''),'Content-Type':'application/json'},body:body === undefined ? undefined : JSON.stringify(body)});
  return { status: response.status, data: await response.json() };
}
async function phone() {
  const issued = await req('/connector/pairing-code',{},connector);
  const paired = await req('/pair',{code:issued.data.code,instanceId});
  assert.equal(paired.status,200);
  await req(`/projects/${projectId}/permission`,{enabled:true},paired.data.token);
  return paired.data;
}
async function task(owner, overrides = {}) {
  const response = await req('/tasks',{id:randomUUID(),projectId,content:'Public synthetic fixture',message:'Verify this',assets:[],...overrides},owner.token);
  assert.equal(response.status,200,JSON.stringify(response.data));
  return response.data;
}
async function finish(id, at = Date.now()) {
  await db.prepare("UPDATE tasks SET status='completed',thread_id=?,report='Verified synthetic report',updated_at=? WHERE id=?").bind(randomUUID(),at,id).run();
}
before(async () => {
  const modules = main => [main,'relay/selfhost.mjs','relay/snapshots.mjs'].map(path => ({type:'ESModule',path}));
  const common = { compatibilityDate:'2026-09-01',d1Databases:{DB:'drop-test-db'},r2Buckets:{FILES:'drop-test-files'} };
  mf = new Miniflare(convertV4MiniflareOptions({ workers:[
    {...common,name:'relay',modules:modules('relay/worker.mjs'),bindings:{INSTANCE_ID:instanceId,CONNECTOR_HASH:digest(connector),ADMIN_HASH:digest(admin),PREVIEW_ORIGIN:'https://preview.test'}},
    {...common,name:'preview',modules:modules('relay/preview-worker.mjs'),bindings:{INSTANCE_ID:instanceId}}
  ] }));
  db = await mf.getD1Database('DB','relay'); bucket = await mf.getR2Bucket('FILES','relay'); previewWorker = await mf.getWorker('preview');
  for (const sql of sqlParts(await readFile('relay/migrations-fresh/0001-baseline.sql','utf8'))) await db.prepare(sql).run();
  await req('/connector/sync',{name:'Synthetic PC',projects:[{id:projectId,name:'Project'}]},connector);
});
after(async () => { await mf?.dispose(); });

test('health checks actual schema and storage; pairing is bound to the scanned instance',async () => {
  const health = await req('/health');
  assert.equal(health.status,200); assert.equal(health.data.ready,true); assert.equal(health.data.instanceId,instanceId); assert.equal(health.data.schemaVersion,11); assert.equal(health.data.protocolVersion,2);
  assert.equal(health.data.version,JSON.parse(await readFile('package.json','utf8')).version);
  const issued = await req('/connector/pairing-code',{},connector);
  assert.equal(issued.data.instanceId,instanceId); assert.equal(issued.data.relayUrl,'https://relay.test');
  assert.equal((await req('/pair',{code:issued.data.code,instanceId:randomUUID()})).status,409);
  assert.equal((await req('/pair',{code:issued.data.code})).status,409);
  assert.equal((await req('/pair',{code:issued.data.code,instanceId})).status,200);
  await db.prepare("UPDATE state SET value='10' WHERE key='schema_version'").run();
  assert.equal((await req('/health')).status,503);
  await db.prepare("UPDATE state SET value='11' WHERE key='schema_version'").run();
});

test('runtime credentials cannot migrate, publish releases or administer retention',async () => {
  for (const path of ['/connector/migrate','/admin/migrate','/admin/retention']) assert.equal((await req(path,{},connector)).status,403);
  assert.equal((await req('/connector/release',{},connector,'PUT')).status,403);
  assert.equal((await req('/connector/sync',{projects:[]},admin)).status,403);
  assert.equal((await req('/admin/migrate',{},admin)).status,200);
});

test('activity covers all retained history beyond 200 and pagination is stable at equal timestamps',async () => {
  const owner = await phone(), second = randomUUID(), unused = randomUUID();
  await req('/connector/sync',{projects:[{id:projectId,name:'Same name'},{id:second,name:'Same name'},{id:unused,name:'Unused'}]},connector);
  const permission = await db.prepare('SELECT version FROM project_permissions WHERE device_id=? AND project_id=?').bind(owner.deviceId,projectId).first();
  const ids = Array.from({length:235},() => randomUUID());
  for (let offset = 0; offset < ids.length; offset += 50) await db.batch(ids.slice(offset,offset+50).map(id => db.prepare("INSERT INTO tasks(id,device_id,project_id,project_name,content,message,created_at,updated_at,status,permission_version,execution_mode) VALUES(?,?,?,'Former name','source','note',100,100,'completed',?,'direct')").bind(id,owner.deviceId,projectId,permission.version)));
  await req(`/projects/${second}/permission`,{enabled:true},owner.token);
  await task(owner,{projectId:second});
  const activity = (await req('/projects/activity',undefined,owner.token)).data;
  assert.equal(activity.projects.length,2);
  const summary = activity.projects.find(p => p.id === projectId);
  assert.equal(summary.name,'Same name'); assert.equal(summary.task_count,235); assert.equal(summary.dispatch_count,235);
  assert.ok(!activity.projects.some(p => p.id === unused));
  assert.equal((await req('/tasks',undefined,owner.token)).data.tasks.length,200);
  let cursor = null, seen = [];
  do {
    const response = await req(`/tasks?project_id=${projectId}&limit=73${cursor ? '&cursor='+encodeURIComponent(cursor) : ''}`,undefined,owner.token);
    assert.equal(response.status,200);
    seen.push(...response.data.tasks.map(t => t.id)); cursor = response.data.nextCursor;
  } while (cursor);
  assert.equal(seen.length,235); assert.equal(new Set(seen).size,235); assert.deepEqual(seen,[...ids].sort().reverse());
  assert.equal((await req(`/tasks/${ids[0]}`,undefined,owner.token)).status,200);
  const other = await phone();
  assert.equal((await req(`/tasks/${ids[0]}`,undefined,other.token)).status,404);
  assert.equal((await req(`/tasks?project_id=${projectId}`,undefined,other.token)).data.tasks.length,0);
  assert.equal((await req(`/tasks?project_id=${projectId}&cursor=invalid`,undefined,owner.token)).status,400);
  await req('/connector/sync',{projects:[{id:second,name:'Same name'}]},connector);
  const removed = (await req('/projects/activity',undefined,owner.token)).data.projects.find(p => p.id === projectId);
  assert.equal(removed.available,false); assert.equal(removed.name,'Former name');
  await req('/connector/sync',{projects:[{id:projectId,name:'Project'}]},connector);
});

test('followups retain root identity after parent deletion; network retries never increment counts',async () => {
  const owner = await phone(), original = await task(owner);
  await finish(original.id);
  const follow = {id:randomUUID(),message:'Continue'};
  const first = await req(`/tasks/${original.id}/followup`,follow,owner.token);
  assert.equal(first.status,200); assert.equal(first.data.root_task_id,original.id);
  assert.equal((await req(`/tasks/${original.id}/followup`,follow,owner.token)).status,200);
  assert.equal((await req('/projects/activity',undefined,owner.token)).data.projects[0].dispatch_count,2);
  await finish(first.data.id);
  assert.equal((await req(`/tasks/${original.id}`,undefined,owner.token,'DELETE')).status,200);
  const second = await req(`/tasks/${first.data.id}/followup`,{id:randomUUID(),message:'Again'},owner.token);
  assert.equal(second.data.root_task_id,original.id);
  const project = (await req('/projects/activity',undefined,owner.token)).data.projects[0];
  assert.equal(project.task_count,1); assert.equal(project.dispatch_count,2);
});

test('fresh baseline equals canonical schema and upgrade preserves root chains, orphans and reports',async () => {
  const schema = await readFile('relay/schema.sql','utf8'), baseline = await readFile('relay/migrations-fresh/0001-baseline.sql','utf8');
  assert.equal(baseline.slice(baseline.indexOf('\n')+1).replaceAll('\r\n','\n'),schema.replaceAll('\r\n','\n'));
  const legacy = new Miniflare(convertV4MiniflareOptions({modules:true,script:'export default {fetch(){return new Response("fixture")}}',compatibilityDate:'2026-09-01',d1Databases:['DB']}));
  try {
    const database = await legacy.getD1Database('DB');
    const old = schema.replace(/,\s*root_task_id TEXT, history_incomplete INTEGER NOT NULL DEFAULT 0,\s*terminal_at INTEGER/,'').split(/;\s*(?:\r?\n|$)/).filter(sql => sql.trim() && !/tasks_device_project_history|tasks_device_root|upload_lifecycle|preview_snapshots|preview_files|preview_sessions|schema_version|tasks_identity_insert|tasks_terminal_update/.test(sql));
    for (const statement of old) await database.prepare(statement).run();
    const root = randomUUID(), child = randomUUID(), grandchild = randomUUID(), orphan = randomUUID(), missing = randomUUID();
    for (const [id,parent] of [[root,null],[child,root],[grandchild,child],[orphan,missing]]) await database.prepare("INSERT INTO tasks(id,device_id,project_id,project_name,content,message,created_at,updated_at,status,report,parent_task_id) VALUES(?,'owner','project','Project','source','note',10,20,'completed','old report',?)").bind(id,parent).run();
    for (const statement of sqlParts(await readFile('relay/migrations/0011-selfhost.sql','utf8'))) await database.prepare(statement).run();
    const records = (await database.prepare('SELECT * FROM tasks').all()).results;
    assert.equal(records.find(t => t.id === grandchild).root_task_id,root);
    assert.equal(records.find(t => t.id === orphan).root_task_id,missing);
    assert.equal(records.find(t => t.id === orphan).history_incomplete,1);
    assert.ok(records.every(t => t.report === 'old report' && t.updated_at === 20 && t.terminal_at === 20));
  } finally { await legacy.dispose(); }
});

test('retention preserves reports and references in active work while expiring old objects',async () => {
  const owner = await phone(), old = await task(owner), active = await task(owner);
  const now = Date.now(), oldAt = now-40*86400000;
  const oldAsset = randomUUID(), sharedAsset = randomUUID();
  for (const id of [oldAsset,sharedAsset]) {
    await bucket.put('uploads/'+id,'fixture');
    await db.prepare('INSERT INTO uploads VALUES(?,?,?,?,?)').bind(id,owner.deviceId,'source.txt','text/plain',7).run();
    await db.prepare('INSERT INTO upload_lifecycle VALUES(?,?)').bind(id,oldAt).run();
  }
  await db.prepare('UPDATE tasks SET assets=? WHERE id=?').bind(JSON.stringify([{id:oldAsset},{id:sharedAsset}]),old.id).run();
  await db.prepare('UPDATE tasks SET assets=? WHERE id=?').bind(JSON.stringify([{id:sharedAsset}]),active.id).run();
  await finish(old.id,oldAt);
  // Preview refreshes may change updated_at; they must never reset the terminal retention clock.
  await db.prepare('UPDATE tasks SET updated_at=? WHERE id=?').bind(now,old.id).run();
  const fileId = digest('artifact');
  await db.prepare("INSERT INTO deliverables(task_id,id,name,kind,sha256,size,ready,created_at) VALUES(?,?,'result.txt','artifact',?,7,1,?)").bind(old.id,fileId,fileId,oldAt).run();
  await bucket.put(`deliverables/${old.id}/${fileId}/${fileId}`,'fixture');
  const result = await cleanupRetention({DB:db,FILES:bucket},now);
  assert.ok(result.uploads >= 1); assert.ok(result.deliverables >= 1);
  assert.equal(await bucket.get('uploads/'+oldAsset),null); assert.ok(await bucket.get('uploads/'+sharedAsset));
  const row = await db.prepare('SELECT * FROM tasks WHERE id=?').bind(old.id).first();
  assert.equal(row.report,'Verified synthetic report'); assert.equal(row.terminal_at,oldAt);
  assert.deepEqual((await req('/device/retention',undefined,owner.token)).data,{rawDays:7,artifactDays:30});
});

test('immutable snapshots publish on a separate capability origin, expire, reopen and revoke',async () => {
  const owner = await phone(), target = await task(owner);
  const html = Buffer.from('<html><head><link rel="stylesheet" href="/styles.css"></head><body><h1>Actual snapshot</h1><img src="/image.png"></body></html>');
  const css = Buffer.from('body{background:url("/image.png")}');
  const files = [{path:'index.html',bytes:html,contentType:'text/html'},{path:'styles.css',bytes:css,contentType:'text/css'},{path:'nested/index.html',bytes:Buffer.from('<h1>Nested snapshot</h1>'),contentType:'text/html'}];
  const manifest = {version:'sha256:'+digest(html),entryPath:'index.html',spa:true,files:files.map(f => ({path:f.path,sha256:digest(f.bytes),size:f.bytes.length,contentType:f.contentType}))};
  const base = `/connector/tasks/${target.id}/snapshots`;
  const created = await req(base,manifest,connector);
  assert.equal(created.status,200,JSON.stringify(created.data));
  const snapshotId = created.data.snapshotId;
  assert.equal((await req(base,{...manifest,files:[{...manifest.files[0],path:'../private'}]},connector)).status,400);
  assert.equal((await req(`${base}/${snapshotId}/publish`,{},connector)).status,409);
  assert.equal((await req(base,{...manifest,files:[{...manifest.files[0],sha256:digest('different')},manifest.files[1]]},connector)).status,409);
  for (const file of files) {
    const response = await mf.dispatchFetch(`https://relay.test${base}/${snapshotId}/files?path=${encodeURIComponent(file.path)}`,{method:'PUT',headers:{Authorization:'Bearer '+connector},body:file.bytes});
    assert.equal(response.status,200,await response.text());
  }
  assert.deepEqual((await req(base,manifest,connector)).data.missing,[]);
  const published = await req(`${base}/${snapshotId}/publish`,{path:'/work?view=product#result'},connector);
  assert.equal(published.status,200,JSON.stringify(published.data));
  assert.match(published.data.url,/^https:\/\/preview\.test\/s\/[a-f0-9]{64}\/work\?view=product#result$/);
  const response = await previewWorker.fetch(published.data.url);
  assert.equal(response.status,200);
  const content = await response.text(), prefix = new URL(published.data.url).pathname.replace(/work$/,'');
  assert.match(content,/Actual snapshot/); assert.ok(content.includes(`href="${prefix}styles.css"`));
  assert.equal(response.headers.get('referrer-policy'),'no-referrer'); assert.match(response.headers.get('content-security-policy'),/sandbox allow-scripts/);
  const style = await previewWorker.fetch('https://preview.test'+prefix+'styles.css');
  assert.ok((await style.text()).includes(prefix+'image.png'));
  assert.match(await (await previewWorker.fetch('https://preview.test'+prefix+'nested')).text(),/Nested snapshot/);
  assert.equal((await previewWorker.fetch('https://preview.test/index.html')).status,404);
  assert.equal((await previewWorker.fetch(published.data.url,{method:'POST'})).status,405);
  await finish(target.id);
  const report = (await req('/tasks/'+target.id,undefined,owner.token)).data.report;
  await db.prepare('UPDATE preview_sessions SET expires_at=1 WHERE snapshot_id=?').bind(snapshotId).run();
  assert.equal((await previewWorker.fetch(published.data.url)).status,410);
  const reopened = await req(`/tasks/${target.id}/preview/reopen`,{},owner.token);
  assert.equal(reopened.status,200); assert.notEqual(reopened.data.url,published.data.url);
  assert.equal((await previewWorker.fetch(reopened.data.url)).status,200);
  assert.equal((await req(`/connector/tasks/${target.id}/preview`,{status:'stopped'},connector)).data.cloudManaged,true);
  assert.equal((await req('/tasks/'+target.id,undefined,owner.token)).data.preview_url,reopened.data.url);
  assert.equal((await req('/tasks/'+target.id,undefined,owner.token)).data.preview_status,'ready');
  assert.ok(!(await req('/connector/previews',undefined,connector)).data.tasks.some(t => t.id === target.id));
  assert.equal((await req('/tasks/'+target.id,undefined,owner.token)).data.report,report);
  await req(`/projects/${projectId}/permission`,{enabled:false},owner.token);
  assert.equal((await previewWorker.fetch(reopened.data.url)).status,410);
});

test('self-hosted preview lifecycle refuses Quick Tunnels and unconfigured origins',async () => {
  const owner = await phone(), target = await task(owner);
  for (const url of ['https://random.trycloudflare.com/?k=secret','https://relay.test/?k=secret','https://outside.test/?k=secret']) {
    assert.equal((await req(`/connector/tasks/${target.id}/preview`,{status:'ready',url,kind:'live',version:'v1',expiresAt:Date.now()+60000},connector)).status,400);
  }
});

test('independent Relay instances never accept the other instance credentials, code or task',async () => {
  const owner = await phone(), source = await task(owner), secondId = randomUUID();
  const otherConnector = 'second-instance-separate-connector-token';
  const second = new Miniflare(convertV4MiniflareOptions({modules:['relay/worker.mjs','relay/selfhost.mjs','relay/snapshots.mjs'].map(path => ({type:'ESModule',path})),compatibilityDate:'2026-09-01',d1Databases:['DB'],r2Buckets:['FILES'],bindings:{INSTANCE_ID:secondId,CONNECTOR_HASH:digest(otherConnector)}}));
  try {
    const database = await second.getD1Database('DB');
    for (const statement of sqlParts(await readFile('relay/schema.sql','utf8'))) await database.prepare(statement).run();
    for (const [path,token] of [['/tasks/'+source.id,owner.token],['/projects',owner.token],['/connector/recover',connector]]) {
      const response = await second.dispatchFetch('https://second.test'+path,{headers:{Authorization:'Bearer '+token}});
      assert.equal(response.status,401);
    }
    const invitation = await req('/connector/pairing-code',{},connector);
    const wrongCode = await second.dispatchFetch('https://second.test/pair',{method:'POST',body:JSON.stringify({code:invitation.data.code,instanceId:secondId})});
    assert.equal(wrongCode.status,403);
    assert.equal((await database.prepare('SELECT COUNT(*) AS count FROM tasks').first()).count,0);
    assert.equal((await database.prepare('SELECT COUNT(*) AS count FROM devices').first()).count,0);
  } finally { await second.dispose(); }
});
