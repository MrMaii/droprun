import { test, after, before } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare } from './miniflare.mjs';
import { readFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';
import { privateAddress, publicUrl } from '../connector/materials.mjs';
const digest = s => createHash('sha256').update(s).digest('hex');
const connectorToken = 'test-connector-secret-long-enough';
const pairCode = 'TEST-PAIR-ONLY';
async function pairPhone() {
  const issued = await req('/connector/pairing-code', {}, connectorToken);
  assert.equal(issued.status, 200);
  const paired = await req('/pair', { code: issued.data.code });
  assert.equal(paired.status, 200);
  // These existing execution/command-approval fixtures opt in; review mode has its own suite.
  assert.equal((await req('/device/settings',{directExecution:true,riskAccepted:true},paired.data.token)).status,200);
  return paired.data.token;
}
let mf, phone, projectId = randomUUID();
async function req(path, body, token = phone, method = body ? 'POST' : 'GET') {
  const r = await mf.dispatchFetch('https://droprun.test' + path, { method, headers: { Authorization: 'Bearer ' + (token || ''), 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
  return { status: r.status, data: await r.json() };
}
before(async () => {
  mf = new Miniflare({ modules: true, scriptPath: 'relay/worker.mjs', compatibilityDate: '2026-08-06', d1Databases: ['DB'], r2Buckets: ['FILES'], bindings: { CONNECTOR_HASH: digest(connectorToken), PAIR_HASH: digest(pairCode) } });
  const db = await mf.getD1Database('DB');
  for (const sql of (await readFile('relay/schema.sql', 'utf8')).split(/;\s*(?:\r?\n|$)/).filter(s => s.trim())) await db.prepare(sql).run();
  phone = await pairPhone();
  await req('/connector/sync', { projects: [{ id: projectId, name: 'Real Project' }] }, connectorToken);
  await req('/projects/'+projectId+'/permission',{enabled:true});
});
after(async () => { await mf?.dispose(); });
test('all projects remain visible but a fresh phone must authorize before submission', async () => {
  const token=await pairPhone();
  const project=(await req('/projects',null,token)).data.projects.find(p=>p.id===projectId);
  assert.ok(project);assert.equal(project.permission?.enabled,false);
  const body={id:randomUUID(),projectId,content:'Permission fixture',message:'',assets:[],permission_version:'forged'};
  assert.equal((await req('/tasks',body,token)).status,403);
  assert.equal((await req('/projects/'+randomUUID()+'/permission',{enabled:true},token)).status,404);
  const route='/projects/'+projectId+'/permission';
  assert.equal((await req(route,{enabled:'yes'},token)).status,400);
  const grant=await req(route,{enabled:true},token);assert.equal(grant.status,200);
  assert.equal((await req(route,{enabled:true},token)).data.version,grant.data.version);
  const task=await req('/tasks',body,token);assert.equal(task.status,200);assert.equal(task.data.permission_version,grant.data.version);
  assert.notEqual(task.data.permission_version,'forged');
  assert.equal((await req('/connector/tasks/'+body.id+'/permission',null,token)).status,403);
  assert.equal((await req('/connector/tasks/'+body.id+'/permission',null,connectorToken)).data.enabled,true);
  const other=await pairPhone();
  assert.equal((await req('/projects',null,other)).data.projects[0].permission.enabled,false);
  await req('/tasks/'+body.id+'/cancel',{},token);
});
test('project revocation cancels queued work, signals active work, and never revives old tasks', async () => {
  const token=await pairPhone(),route='/projects/'+projectId+'/permission';
  const first=(await req(route,{enabled:true},token)).data;
  const ids=[randomUUID(),randomUUID(),randomUUID()];
  for(const id of ids)assert.equal((await req('/tasks',{id,projectId,content:'Project revoke fixture',message:'',assets:[]},token)).status,200);
  await req('/connector/task',{id:ids[1],status:'running',threadId:randomUUID(),turnId:randomUUID()},connectorToken);
  await req('/connector/task',{id:ids[2],status:'completed',threadId:randomUUID(),report:'Old report'},connectorToken);
  assert.equal((await req(route,{enabled:false},token)).status,200);
  const tasks=(await req('/tasks',null,token)).data.tasks;
  assert.equal(tasks.find(t=>t.id===ids[0]).status,'cancelled');assert.equal(tasks.find(t=>t.id===ids[1]).cancel_requested,1);
  assert.equal(tasks.find(t=>t.id===ids[2]).report,'Old report');
  assert.equal((await req('/tasks/'+ids[2]+'/followup',{id:randomUUID(),message:'Continue'},token)).status,403);
  const next=(await req(route,{enabled:true},token)).data;assert.notEqual(first.version,next.version);
  assert.equal((await req('/connector/tasks/'+ids[1]+'/permission',null,connectorToken)).data.enabled,false);
  const followup=await req('/tasks/'+ids[2]+'/followup',{id:randomUUID(),message:'Continue'},token);assert.equal(followup.status,200);assert.equal(followup.data.permission_version,next.version);
  await req('/tasks/'+followup.data.id+'/cancel',{},token);
  await req('/connector/task',{id:ids[1],status:'cancelled'},connectorToken);
});
test('project list requires pairing and phone cannot invoke connector endpoints', async () => {
  assert.equal((await req('/projects', null, 'wrong')).status, 401);
  assert.equal((await req('/connector/claim', {}, phone)).status, 403);
  const r = await req('/projects');
  assert.equal(r.data.projects[0].id, projectId); assert.equal(r.data.online, true);
});
test('invalid pairing and unknown project fail', async () => {
  assert.equal((await req('/pair', { code: 'BAD' })).status, 403);
  assert.equal((await req('/tasks', { id: randomUUID(), projectId: randomUUID(), content: 'x', message: '', assets: [] })).status, 400);
});
test('same share ID creates only one task; parallel claims execute once', async () => {
  const t = { id: randomUUID(), projectId, content: 'https://x.com/example/status/123', message: '只参考颜色', assets: [] };
  const a = await req('/tasks', t), b = await req('/tasks', t);
  assert.equal(a.status, 200); assert.equal(a.data.id, b.data.id);
  assert.equal(b.data.message, '只参考颜色');
  const claims = await Promise.all([req('/connector/claim', {}, connectorToken), req('/connector/claim', {}, connectorToken)]);
  assert.equal(claims.filter(c => c.data.task?.id === t.id).length, 1);
  const recovery = await req('/connector/recover', null, connectorToken);
  assert.equal(recovery.data.tasks.filter(x => x.id === t.id).length, 1);
  await req('/connector/task', { id: t.id, status: 'completed', report: 'Evidence report', events: [] }, connectorToken);
  await req('/connector/task', { id: t.id, status: 'running', events: [] }, connectorToken);
  const tasks = (await req('/tasks')).data.tasks;
  assert.equal(tasks.find(x => x.id === t.id).status, 'completed');
});
test('device ownership prevents reading or cancelling another phone task', async () => {
  const token = await pairPhone();
  const t = { id: randomUUID(), projectId, content: 'Shared reference', message: '', assets: [] };
  await req('/tasks', t);
  assert.equal((await req('/tasks', null, token)).data.tasks.length, 0);
  assert.equal((await req('/tasks', t, token)).status, 409);
  await req('/tasks/' + t.id + '/cancel', {}, token);
  assert.equal((await req('/tasks')).data.tasks.find(x => x.id === t.id).cancel_requested, 0);
  await req('/tasks/' + t.id + '/cancel', {});
  assert.equal((await req('/tasks')).data.tasks.find(x => x.id === t.id).status, 'cancelled');
});
test('untrusted shared links cannot target private addresses', async () => {
  for (const ip of ['127.0.0.1','10.1.2.3','192.168.2.1','169.254.169.254','172.20.1.1','::1']) assert.equal(privateAddress(ip),true);
  await assert.rejects(publicUrl('http://127.0.0.1/secret'));
  await assert.rejects(publicUrl('file:///etc/passwd'));
});
test('revoking a phone invalidates its token and cancels its queued work only', async()=>{
  const token=await pairPhone();
  await req('/projects/'+projectId+'/permission',{enabled:true},token);
  const id=randomUUID();
  await req('/tasks',{id,projectId,content:'Revocation test',message:'',assets:[]},token);
  assert.equal((await req('/device/revoke',{},token)).status,200);
  assert.equal((await req('/projects',null,token)).status,401);
  const task=(await req('/tasks',null,connectorToken)).data.tasks.find(t=>t.id===id);
  assert.equal(task.status,'cancelled');assert.equal(task.cancel_requested,1);
  assert.equal((await req('/projects')).status,200);
});
test('task deletion requires ownership and terminal state, and deletes unshared objects', async()=>{
  const bucket=await mf.getR2Bucket('FILES'),db=await mf.getD1Database('DB');
  const device=await db.prepare('SELECT id FROM devices WHERE token_hash=?').bind(digest(phone)).first();
  const asset=randomUUID();
  await bucket.put('uploads/'+asset,'synthetic attachment');
  await db.prepare('INSERT INTO uploads VALUES(?,?,?,?,?)').bind(asset,device.id,'fixture.txt','text/plain',20).run();
  const id=randomUUID(),other=await pairPhone();
  await req('/tasks',{id,projectId,content:'Delete fixture',message:'',assets:[asset]});
  assert.equal((await req('/tasks/'+id,null,phone,'DELETE')).status,409);
  await req('/tasks/'+id+'/cancel',{});
  assert.equal((await req('/tasks/'+id,null,other,'DELETE')).status,404);
  assert.ok(await bucket.get('uploads/'+asset));
  assert.equal((await req('/tasks/'+id,null,phone,'DELETE')).status,200);
  assert.equal(await bucket.get('uploads/'+asset),null);
  assert.equal((await req('/tasks')).data.tasks.some(t=>t.id===id),false);
});
test('followups are owner-only, idempotent, retain old reports and serialize a conversation', async () => {
  const parent = randomUUID(), threadId = randomUUID(), child = randomUUID();
  await req('/tasks', { id: parent, projectId, content: 'Original reference', message: 'Original request', assets: [] });
  assert.equal((await req('/tasks/' + parent + '/followup', { id: child, message: 'Continue' })).status, 409);
  await req('/connector/task', { id: parent, status: 'completed', threadId, report: 'Original report' }, connectorToken);
  const other = await pairPhone();
  assert.equal((await req('/tasks/' + parent + '/followup', { id: child, message: 'Continue' }, other)).status, 404);
  assert.equal((await req('/tasks/' + parent + '/followup', { id: child, message: ' ' })).status, 400);
  const first = await req('/tasks/' + parent + '/followup', { id: child, message: 'Continue', threadId: 'forged' });
  const retry = await req('/tasks/' + parent + '/followup', { id: child, message: 'Continue' });
  assert.equal(first.status, 200); assert.equal(retry.data.id, child);
  assert.equal(first.data.thread_id, threadId); assert.equal(first.data.parent_task_id, parent);
  assert.equal(first.data.turn_id, null); assert.equal(first.data.report, null);
  assert.equal((await req('/tasks')).data.tasks.find(task => task.id === parent).report, 'Original report');
  assert.equal((await req('/tasks/' + parent + '/followup', { id: randomUUID(), message: 'Second' })).status, 409);
  await req('/tasks/' + child + '/cancel', {});
  const parallel = await Promise.all([1, 2].map(n => req('/tasks/' + parent + '/followup', { id: randomUUID(), message: 'Next ' + n })));
  assert.equal(parallel.filter(result => result.status === 200).length, 1);
  assert.equal(parallel.filter(result => result.status === 409).length, 1);
});

test('deleting a parent preserves attachments needed by its followup', async () => {
  const bucket = await mf.getR2Bucket('FILES'), db = await mf.getD1Database('DB');
  const device = await db.prepare('SELECT id FROM devices WHERE token_hash=?').bind(digest(phone)).first();
  const asset = randomUUID(), parent = randomUUID(), child = randomUUID();
  await bucket.put('uploads/' + asset, 'followup reference');
  await db.prepare('INSERT INTO uploads VALUES(?,?,?,?,?)').bind(asset, device.id, 'reference.txt', 'text/plain', 18).run();
  await req('/tasks', { id: parent, projectId, content: 'Original', message: '', assets: [asset] });
  await req('/connector/task', { id: parent, status: 'completed', threadId: randomUUID(), report: 'Original report' }, connectorToken);
  assert.equal((await req('/tasks/' + parent + '/followup', { id: child, message: 'Continue' })).status, 200);
  assert.equal((await req('/tasks/' + parent, null, phone, 'DELETE')).status, 200);
  assert.ok(await bucket.get('uploads/' + asset));
  await req('/tasks/' + child + '/cancel', {});
  assert.equal((await req('/tasks/' + child, null, phone, 'DELETE')).status, 200);
  assert.equal(await bucket.get('uploads/' + asset), null);
});

async function approvalFixture(token=phone) {
  await req('/projects/'+projectId+'/permission',{enabled:true},token);
  const taskId=randomUUID(),threadId=randomUUID(),turnId=randomUUID(),id=randomUUID();
  await req('/tasks',{id:taskId,projectId,content:'Approval fixture',message:'',assets:[]},token);
  await req('/connector/task',{id:taskId,status:'waiting_for_approval',threadId,turnId},connectorToken);
  const body={id,taskId,threadId,turnId,itemId:'item',details:{kind:'command',command:'Write-Output synthetic',cwd:'C:/test',reason:'Acceptance test'}};
  assert.equal((await req('/connector/approvals',body,connectorToken)).status,200);
  return {body,id,taskId,route:'/tasks/'+taskId+'/approvals/'+id};
}
test('approval is immutable, owner-only, scoped to exact turn, and has one winning decision', async () => {
  const f=await approvalFixture(),other=await pairPhone();
  assert.equal((await req('/connector/approvals',f.body,phone)).status,403);
  assert.equal((await req('/connector/approvals',{...f.body,turnId:randomUUID()},connectorToken)).status,409);
  assert.equal((await req('/connector/approvals',{...f.body,details:{...f.body.details,command:'changed'}},connectorToken)).status,409);
  assert.equal((await req(f.route,{decision:'approved'},other)).status,404);
  assert.equal((await req(f.route,{decision:'acceptForSession'})).status,400);
  assert.equal((await req('/tasks',null,other)).data.tasks.some(t=>t.approvals?.some(a=>a.id===f.id)),false);
  assert.equal((await req('/tasks')).data.tasks.find(t=>t.id===f.taskId).approvals[0].details.command,f.body.details.command);
  const choices=await Promise.all(['approved','denied'].map(decision=>req(f.route,{decision})));
  assert.equal(choices.filter(c=>c.status===200).length,1); assert.equal(choices.filter(c=>c.status===409).length,1);
  const result=await req('/connector/approvals/'+f.id,null,connectorToken);
  assert.ok(['approved','denied'].includes(result.data.status));
});
test('approval expires and cannot survive cancellation, device or project revocation, restart or a different turn', async () => {
  const db=await mf.getD1Database('DB');
  for(const condition of ['expiry','cancel','revoke','project','restart','turn']) {
    const token=condition==='revoke'?await pairPhone():phone;
    const f=await approvalFixture(token);
    assert.equal((await req(f.route,{decision:'approved'},token)).status,200);
    if(condition==='expiry') await db.prepare('UPDATE approvals SET expires_at=? WHERE id=?').bind(1,f.id).run();
    if(condition==='cancel') await req('/tasks/'+f.taskId+'/cancel',{},token);
    if(condition==='revoke') await req('/device/revoke',{},token);
    if(condition==='project') await req('/projects/'+projectId+'/permission',{enabled:false},token);
    if(condition==='restart') await req('/connector/approvals/invalidate',{taskId:f.taskId},connectorToken);
    if(condition==='turn') await req('/connector/task',{id:f.taskId,status:'running',turnId:randomUUID()},connectorToken);
    if(condition!=='revoke') assert.equal((await req(f.route,{decision:'approved'},token)).status,409);
    const result=await req('/connector/approvals/'+f.id,null,connectorToken);
    assert.equal(result.data.status,condition==='expiry'?'expired':'invalidated');
  }
});
test('a legacy or tampered queued task without an authorization generation is never claimed',async()=>{
  const db=await mf.getD1Database('DB'),device=await db.prepare('SELECT id FROM devices WHERE token_hash=?').bind(digest(phone)).first(),id=randomUUID();
  await db.prepare('INSERT INTO tasks (id,device_id,project_id,project_name,content,message,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)').bind(id,device.id,projectId,'Real Project','Legacy queued fixture','',0,0).run();
  const claim=await req('/connector/claim',{},connectorToken);assert.notEqual(claim.data.task?.id,id);
  const task=await db.prepare('SELECT status,cancel_requested FROM tasks WHERE id=?').bind(id).first();assert.equal(task.status,'cancelled');assert.equal(task.cancel_requested,1);
});
