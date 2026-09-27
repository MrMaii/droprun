import { test, before, beforeEach, after } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare } from './miniflare.mjs';
import { readFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';

const hash=value=>createHash('sha256').update(value).digest('hex');
const connector='review-test-connector-secret',phone='review-test-phone-token',other='review-test-other-token';
const owner=randomUUID(),stranger=randomUUID(),projectId=randomUUID();
let mf,db;
async function req(path,body,token=phone,method=body?'POST':'GET') {
  const response=await mf.dispatchFetch('https://droprun.test'+path,{method,headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
  return {status:response.status,data:await response.json()};
}
async function create(extra={},token=phone) {
  const result=await req('/tasks',{id:randomUUID(),projectId,content:'Shared material',message:'Use this idea',assets:[],...extra},token);
  assert.equal(result.status,200);return result.data;
}
async function planning() {
  const task=await create(),threadId=randomUUID(),turnId=randomUUID();
  const claimed=await req('/connector/claim',{},connector);assert.equal(claimed.data.task.id,task.id);
  assert.equal((await req('/connector/task',{id:task.id,status:'planning',threadId,turnId},connector)).status,200);
  return {task,threadId,turnId,planVersion:randomUUID(),report:'我收到材料。理解：调整项目界面。计划：检查组件，修改颜色，运行测试。'};
}
async function publish(fixture) {
  const {task,...body}=fixture;return req('/connector/tasks/'+task.id+'/plan',body,connector);
}
const decide=(f,decision='approved',token=phone)=>req('/tasks/'+f.task.id+'/plan-decision',{planVersion:f.planVersion,decision},token);
before(async()=>{
  mf=new Miniflare({modules:true,scriptPath:'relay/worker.mjs',compatibilityDate:'2026-08-06',d1Databases:['DB','LEGACY'],r2Buckets:['FILES'],bindings:{CONNECTOR_HASH:hash(connector)}});
  db=await mf.getD1Database('DB');
  for(const sql of (await readFile('relay/schema.sql','utf8')).split(/;\s*(?:\r?\n|$)/).filter(sql=>sql.trim()))await db.prepare(sql).run();
});
beforeEach(async()=>{
  for(const table of ['approvals','deliverables','tasks','project_permissions','devices'])await db.prepare('DELETE FROM '+table).run();
  await db.prepare('INSERT INTO devices (id,token_hash,created_at) VALUES (?,?,0),(?,?,0)').bind(owner,hash(phone),stranger,hash(other)).run();
  await db.prepare("INSERT INTO project_permissions VALUES (?,?,1,'v1',0),(?,?,1,'v2',0)").bind(owner,projectId,stranger,projectId).run();
  await req('/connector/sync',{projects:[{id:projectId,name:'Existing project'}]},connector);
});
after(async()=>{await mf?.dispose();});

test('device defaults to review; direct requires explicit risk acceptance and belongs only to that phone',async()=>{
  assert.deepEqual((await req('/device/settings')).data,{directExecution:false});
  assert.equal((await req('/device/settings',{directExecution:true})).status,400);
  assert.equal((await req('/device/settings',{directExecution:true,riskAccepted:'true'})).status,400);
  assert.equal((await req('/device/settings',{directExecution:'yes',riskAccepted:true})).status,400);
  assert.equal((await req('/device/settings',{directExecution:true,riskAccepted:true},connector)).status,404);
  assert.equal((await req('/device/settings',{directExecution:true,riskAccepted:true},'invalid')).status,401);
  assert.equal((await req('/device/settings',{directExecution:true,riskAccepted:true,deviceId:stranger})).data.directExecution,true);
  assert.equal((await req('/device/settings',null,other)).data.directExecution,false);
  assert.equal((await req('/device/settings',{directExecution:true,riskAccepted:true})).status,200);
  assert.equal((await req('/device/settings',{directExecution:false})).data.directExecution,false);
});

test('task creation snapshots server mode; forged flags and later setting changes cannot bypass a pending plan',async()=>{
  const task=await create({executionMode:'direct',execution_mode:'direct',plan_decision:'approved'});
  assert.equal(task.execution_mode,'review');assert.equal(task.plan_decision,null);
  await req('/device/settings',{directExecution:true,riskAccepted:true});
  const direct=await create();assert.equal(direct.execution_mode,'direct');
  const retry=await req('/tasks',{id:task.id,projectId,content:task.content,message:task.message,assets:[],execution_mode:'direct'});
  assert.equal(retry.data.execution_mode,'review');
  await req('/device/settings',{directExecution:false});
  const saved=await db.prepare('SELECT execution_mode FROM tasks WHERE id=?').bind(direct.id).first();assert.equal(saved.execution_mode,'direct');
  assert.equal((await create()).execution_mode,'review');
  const permission=(await req('/connector/tasks/'+task.id+'/permission',null,connector)).data;
  assert.equal(permission.profile,'original-project');assert.equal(permission.executionMode,'review');assert.equal(permission.planDecision,null);
});

test('review cannot run, complete, ask command approval or publish deliverables before phone approval',async()=>{
  const f=await planning();
  for(const status of ['running','waiting_for_approval','completed'])assert.equal((await req('/connector/task',{id:f.task.id,status,plan_decision:'approved'},connector)).status,409);
  assert.equal((await req('/connector/task',{id:f.task.id,status:'queued_execution'},connector)).status,400);
  assert.equal((await req('/connector/task',{id:f.task.id,status:'awaiting_plan_approval'},connector)).status,400);
  assert.equal((await req('/connector/approvals',{id:randomUUID(),taskId:f.task.id,threadId:f.threadId,turnId:f.turnId,itemId:'command',details:{kind:'command',command:'npm test'}},connector)).status,409);
  await req('/connector/task',{id:f.task.id,status:'reading'},connector);
  const bytes='test',response=await mf.dispatchFetch('https://droprun.test/connector/tasks/'+f.task.id+'/deliverables/'+'a'.repeat(64),{method:'PUT',headers:{Authorization:'Bearer '+connector,'X-Filename':'report.txt','X-Kind':'artifact','X-Sha256':hash(bytes),'X-Size':'4'},body:bytes});
  assert.equal(response.status,409);
});

test('plan publication binds exact task turn and immutable content; stale state reports cannot overwrite it',async()=>{
  const f=await planning();
  assert.equal((await publish({...f,turnId:randomUUID()})).status,409);
  assert.equal((await publish({...f,threadId:randomUUID()})).status,409);
  assert.equal((await req('/connector/tasks/'+f.task.id+'/plan',{...f,task:undefined})).status,403);
  const concurrent=await Promise.all([publish(f),publish({...f,report:'A different plan'})]);
  assert.equal(concurrent.filter(r=>r.status===200).length,1);assert.equal(concurrent.filter(r=>r.status===409).length,1);
  const winner=concurrent.find(r=>r.status===200).data;f.report=winner.plan_report;
  assert.equal((await publish(f)).status,200);
  assert.equal((await publish({...f,planVersion:randomUUID()})).status,409);
  assert.equal(winner.status,'awaiting_plan_approval');assert.equal(winner.plan_turn_id,f.turnId);
  const stale=await req('/connector/task',{id:f.task.id,status:'planning',turnId:randomUUID()},connector);
  assert.equal(stale.data.status,'awaiting_plan_approval');assert.equal(stale.data.turn_id,f.turnId);
  assert.equal((await req('/connector/recover',null,connector)).data.tasks.length,0);
  assert.equal((await req('/connector/claim',{},connector)).data.task,null);
});

test('phone approval is versioned, owner-only and atomic; one decision wins and exact retries retain its timestamp',async()=>{
  const f=await planning();await publish(f);
  assert.equal((await decide(f,'approved',other)).status,404);
  assert.equal((await decide({...f,planVersion:randomUUID()})).status,409);
  assert.equal((await req('/tasks/'+f.task.id+'/plan-decision',{planVersion:f.planVersion,decision:'approve-all'})).status,400);
  const results=await Promise.all([decide(f,'approved'),decide(f,'rejected')]);
  assert.equal(results.filter(r=>r.status===200).length,1);assert.equal(results.filter(r=>r.status===409).length,1);
  const winner=results.find(r=>r.status===200).data;
  const retry=await decide(f,winner.plan_decision);assert.equal(retry.status,200);assert.equal(retry.data.plan_decided_at,winner.plan_decided_at);
  assert.equal(winner.status,winner.plan_decision==='approved'?'queued_execution':'cancelled');
});

test('approved plan resumes same task once with original plan intact; switching settings does not alter approval',async()=>{
  const f=await planning();await publish(f);
  await req('/device/settings',{directExecution:true,riskAccepted:true});
  await req('/device/settings',{directExecution:false});
  assert.equal((await req('/connector/task',{id:f.task.id,status:'running'},connector)).data.status,'awaiting_plan_approval');
  assert.equal((await decide(f)).data.status,'queued_execution');
  assert.equal((await req('/connector/task',{id:f.task.id,status:'planning'},connector)).data.status,'queued_execution');
  assert.equal((await req('/connector/recover',null,connector)).data.tasks.length,0);
  const results=await Promise.all([req('/connector/claim',{},connector),req('/connector/claim',{},connector)]);
  assert.equal(results.filter(r=>r.data.task?.id===f.task.id).length,1);
  const task=results.find(r=>r.data.task).data.task;assert.equal(task.plan_decision,'approved');assert.equal(task.plan_report,f.report);assert.equal(task.thread_id,f.threadId);
  assert.equal((await req('/connector/task',{id:task.id,status:'planning'},connector)).status,409);
  for(const status of ['running','waiting_for_approval','completed'])assert.equal((await req('/connector/task',{id:task.id,status,turnId:f.turnId},connector)).status,409);
  const executionTurn=randomUUID();assert.equal((await req('/connector/task',{id:task.id,status:'running',turnId:executionTurn},connector)).status,200);
  assert.equal((await req('/connector/task',{id:task.id,status:'running',threadId:randomUUID(),turnId:executionTurn},connector)).status,409);
  for(const status of ['reading','running','completed','blocked'])assert.equal((await req('/connector/task',{id:task.id,status,turnId:f.turnId},connector)).status,409);
  assert.equal((await publish(f)).status,200);
  assert.equal((await req('/connector/task',{id:task.id,status:'completed',report:'Work verified'},connector)).status,200);
  const saved=await db.prepare('SELECT * FROM tasks WHERE id=?').bind(task.id).first();assert.equal(saved.plan_turn_id,f.turnId);assert.equal(saved.turn_id,executionTurn);
});

test('claim serializes tasks but releases the connector while a phone reviews a plan',async()=>{
  const first=await create(),second=await create();
  await db.prepare('UPDATE tasks SET created_at=1 WHERE id=?').bind(first.id).run();
  await db.prepare('UPDATE tasks SET created_at=2 WHERE id=?').bind(second.id).run();
  const claims=await Promise.all([req('/connector/claim',{},connector),req('/connector/claim',{},connector)]);
  assert.equal(claims.filter(r=>r.data.task).length,1);assert.equal(claims.find(r=>r.data.task).data.task.id,first.id);
  const f={task:first,threadId:randomUUID(),turnId:randomUUID(),planVersion:randomUUID(),report:'First plan'};
  await req('/connector/task',{id:first.id,status:'planning',threadId:f.threadId,turnId:f.turnId},connector);
  assert.equal((await req('/connector/recover',null,connector)).data.tasks[0].status,'planning');
  await publish(f);assert.equal((await req('/connector/claim',{},connector)).data.task.id,second.id);
});

test('cancellation and revocation close pending plans and queued execution instead of leaving them suspended',async()=>{
  for(const phase of ['planning','awaiting_plan_approval','queued_execution']) {
    for(const action of ['cancel','project','device']) {
      await db.prepare('DELETE FROM tasks').run();
      await db.prepare('INSERT OR IGNORE INTO devices (id,token_hash,created_at) VALUES (?,?,0)').bind(owner,hash(phone)).run();
      await req('/projects/'+projectId+'/permission',{enabled:true});
      const f=await planning();
      if(phase!=='planning')await publish(f);
      if(phase==='queued_execution')await decide(f);
      if(action==='cancel')await req('/tasks/'+f.task.id+'/cancel',{});
      if(action==='project')await req('/projects/'+projectId+'/permission',{enabled:false});
      if(action==='device')await req('/device/revoke',{});
      const task=await db.prepare('SELECT * FROM tasks WHERE id=?').bind(f.task.id).first();
      assert.equal(task.cancel_requested,1);assert.equal(task.status,phase==='planning'?'planning':'cancelled');
      const poll=await req('/connector/task',{id:task.id,status:'planning',threadId:f.threadId,turnId:f.turnId},connector);
      assert.equal(poll.status,200);assert.equal(poll.data.cancel_requested,1);
      assert.equal((await req('/connector/tasks/'+task.id+'/permission',null,connector)).data.enabled,false);
      if(phase==='planning')assert.equal((await publish(f)).status,409);
      if(action!=='device')assert.equal((await decide(f)).status,409);
      await req('/connector/task',{id:task.id,status:'cancelled'},connector);
      assert.equal((await req('/connector/claim',{},connector)).data.task,null);
    }
  }
});

test('direct tasks execute immediately and followups snapshot current device mode while retaining their thread',async()=>{
  await req('/device/settings',{directExecution:true,riskAccepted:true});
  const task=await create(),threadId=randomUUID();
  assert.equal((await req('/connector/claim',{},connector)).data.task.id,task.id);
  assert.equal((await req('/connector/task',{id:task.id,status:'running',threadId,turnId:randomUUID()},connector)).status,200);
  assert.equal((await req('/connector/task',{id:task.id,status:'completed',report:'Done'},connector)).status,200);
  await req('/device/settings',{directExecution:false});
  const child=await req('/tasks/'+task.id+'/followup',{id:randomUUID(),message:'Continue',execution_mode:'direct'});
  assert.equal(child.status,200);assert.equal(child.data.execution_mode,'review');assert.equal(child.data.thread_id,threadId);assert.equal(child.data.plan_decision,null);
});

test('legacy execution remains isolated and can complete without plan approval',async()=>{
  const id=randomUUID();
  await db.prepare("INSERT INTO tasks (id,device_id,project_id,project_name,content,message,created_at,updated_at,permission_version) VALUES (?,?,?,'Legacy','Old share','',0,0,'v1')").bind(id,owner,projectId).run();
  const permission=(await req('/connector/tasks/'+id+'/permission',null,connector)).data;
  assert.equal(permission.executionMode,'legacy-isolated');assert.equal(permission.profile,'isolated-workspace');
  assert.equal((await req('/connector/claim',{},connector)).data.task.id,id);
  assert.equal((await req('/connector/task',{id,status:'running',threadId:randomUUID(),turnId:randomUUID()},connector)).status,200);
  assert.equal((await req('/connector/task',{id,status:'completed',report:'Existing delivery'},connector)).status,200);
});

test('incremental migration keeps existing paired devices, task status, reports and threads unchanged',async()=>{
  const legacy=await mf.getD1Database('LEGACY');
  await legacy.prepare('CREATE TABLE devices (id TEXT PRIMARY KEY,token_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)').run();
  await legacy.prepare('CREATE TABLE tasks (id TEXT PRIMARY KEY,status TEXT,report TEXT,thread_id TEXT,permission_version TEXT)').run();
  await legacy.prepare("INSERT INTO devices VALUES ('old-device','old-hash',123)").run();
  await legacy.prepare("INSERT INTO tasks VALUES ('old-task','completed','Historical report','original-thread','v1')").run();
  for(const sql of (await readFile('relay/migrations/0006-execution-mode.sql','utf8')).split(/;\s*(?:\r?\n|$)/).filter(sql=>sql.trim()))await legacy.prepare(sql).run();
  assert.deepEqual(await legacy.prepare('SELECT * FROM devices').first(),{id:'old-device',token_hash:'old-hash',created_at:123,direct_execution:0});
  assert.deepEqual(await legacy.prepare('SELECT * FROM tasks').first(),{id:'old-task',status:'completed',report:'Historical report',thread_id:'original-thread',permission_version:'v1',execution_mode:'legacy-isolated',plan_report:null,plan_turn_id:null,plan_version:null,plan_decision:null,plan_decided_at:null});
});
