import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare } from './miniflare.mjs';
import { readFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';
import worker from '../relay/worker.mjs';

const hash = value => createHash('sha256').update(value).digest('hex');
const connector = 'preview-state-test-connector-secret';
const admin = 'preview-state-test-admin-secret-long';
const projectId = randomUUID();
const previewUrl = 'https://verified-preview.trycloudflare.com/about?k=fixture-token';
let mf, db;

async function req(path, body, token, method = body === undefined ? 'GET' : 'POST') {
  const response = await mf.dispatchFetch('https://droprun.test' + path, {
    method,
    headers: { Authorization: 'Bearer ' + (token || ''), 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { status: response.status, data: await response.json() };
}
async function phone() {
  const issued = await req('/connector/pairing-code', {}, connector);
  assert.equal(issued.status, 200);
  const paired = await req('/pair', { code: issued.data.code });
  assert.equal(paired.status, 200);
  const token = paired.data.token;
  assert.equal((await req(`/projects/${projectId}/permission`, { enabled: true }, token)).status, 200);
  return token;
}
async function task(token, status = 'completed') {
  const id = randomUUID();
  const created = await req('/tasks', { id, projectId, content: 'Synthetic visual reference', message: 'Update the about page', assets: [] }, token);
  assert.equal(created.status, 200);
  await db.prepare('UPDATE tasks SET status=?,report=?,thread_id=? WHERE id=?').bind(status, 'Original verified report', randomUUID(), id).run();
  return id;
}
const ready = (overrides = {}) => ({ status: 'ready', url: previewUrl, expiresAt: Date.now() + 1800000, kind: 'snapshot', version: 'sha256:fixture-version', ...overrides });
const update = (id, body, token = connector) => req(`/connector/tasks/${id}/preview`, body, token);
const reopen = (id, token) => req(`/tasks/${id}/preview/reopen`, {}, token);
const row = id => db.prepare('SELECT * FROM tasks WHERE id=?').bind(id).first();

before(async () => {
  mf = new Miniflare({ modules: true, scriptPath: 'relay/worker.mjs', compatibilityDate: '2026-08-06', d1Databases: ['DB'], r2Buckets: ['FILES'], bindings: { CONNECTOR_HASH: hash(connector) } });
  db = await mf.getD1Database('DB');
  for (const sql of (await readFile('relay/schema.sql', 'utf8')).split(/;\s*(?:\r?\n|$)/).filter(value => value.trim())) await db.prepare(sql).run();
  assert.equal((await req('/connector/sync', { projects: [{ id: projectId, name: 'Preview Fixture' }] }, connector)).status, 200);
});
after(async () => { await mf?.dispose(); });

test('ready previews require a gated HTTPS URL, future expiry, known kind and a concrete version', async () => {
  const token = await phone(), id = await task(token);
  for (const bad of [
    { status: 'unknown' }, { url: null }, { url: 'http://verified-preview.trycloudflare.com/?k=token' },
    { url: 'https://example.com/?k=token' }, { url: 'https://trycloudflare.com.evil.example/?k=token' },
    { url: 'https://verified-preview.trycloudflare.com/' }, { url: 'https://user:pass@verified-preview.trycloudflare.com/?k=token' },
    { expiresAt: Date.now() - 1 }, { expiresAt: 'future' }, { expiresAt: Number.MAX_SAFE_INTEGER + 1 },
    { kind: null }, { kind: 'arbitrary' }, { version: null }, { version: '' }, { version: 17 }, { version: 'x'.repeat(129) },
  ]) assert.equal((await update(id, ready(bad))).status, 400, JSON.stringify(bad));
  assert.equal((await update(id, ready(), token)).status, 403, 'A paired phone cannot impersonate the Connector');
  assert.equal((await update(id, ready(), 'wrong')).status, 401);
  assert.equal((await update(randomUUID(), ready())).status, 409);
  assert.equal((await update(id, ready())).status, 200);
  const current = await row(id);
  assert.equal(current.preview_url, previewUrl);
  assert.equal(current.preview_kind, 'snapshot');
  assert.equal(current.preview_version, 'sha256:fixture-version');
});

test('preview updates on a completed task preserve its original report and execution state', async () => {
  const token = await phone(), id = await task(token);
  const original = await row(id);
  assert.equal((await update(id, ready({ report: 'Forged replacement report', taskStatus: 'running', threadId: 'forged-thread' }))).status, 200);
  assert.equal((await update(id, { status: 'stopped' })).status, 200);
  const current = await row(id);
  assert.equal(current.status, original.status);
  assert.equal(current.report, original.report);
  assert.equal(current.thread_id, original.thread_id);
  assert.equal(current.preview_status, 'stopped');
  assert.equal(current.preview_url, previewUrl, 'Stopped previews retain the descriptor needed for reopening');
  assert.equal(current.preview_kind, 'snapshot');
  assert.equal(current.preview_version, 'sha256:fixture-version');
});

test('preview reopening is owner-only and idempotent without creating another task or changing its report', async () => {
  const token = await phone(), other = await phone(), id = await task(token);
  assert.equal((await update(id, ready())).status, 200);
  assert.equal((await update(id, { status: 'expired' })).status, 200);
  assert.equal((await reopen(id, other)).status, 404);
  assert.equal((await reopen(id, 'wrong')).status, 401);
  const original = await row(id);
  assert.deepEqual(await reopen(id, token), { status: 200, data: { requested: true } });
  const requested = await row(id);
  assert.equal(requested.preview_status, 'reopening');
  assert.ok(requested.preview_requested_at > 0);
  assert.deepEqual(await reopen(id, token), { status: 200, data: { requested: true } });
  const retried = await row(id);
  assert.equal(retried.preview_requested_at, requested.preview_requested_at);
  assert.equal(retried.status, original.status);
  assert.equal(retried.report, original.report);
  assert.equal((await req('/tasks', undefined, token)).data.tasks.length, 1);
  assert.equal((await update(id, ready({ version: 'sha256:reopened', requestedAt: requested.preview_requested_at }))).status, 200);
  assert.equal((await row(id)).preview_requested_at, null, 'A Connector acknowledgement consumes the reopen request');
});

test('active, cancelled, uninitialized and revoked tasks cannot request or receive a preview', async () => {
  const token = await phone(), active = await task(token, 'running'), uninitialized = await task(token), cancelled = await task(token, 'cancelled');
  assert.equal((await update(active, ready())).status, 200);
  assert.equal((await reopen(active, token)).status, 409);
  assert.equal((await reopen(uninitialized, token)).status, 409);
  await db.prepare('UPDATE tasks SET cancel_requested=1,preview_url=? WHERE id=?').bind(previewUrl, cancelled).run();
  assert.equal((await reopen(cancelled, token)).status, 409);
  assert.equal((await update(cancelled, ready())).status, 409);
  const revoked = await task(token);
  assert.equal((await update(revoked, ready())).status, 200);
  assert.equal((await req(`/projects/${projectId}/permission`, { enabled: false }, token)).status, 200);
  assert.equal((await reopen(revoked, token)).status, 404);
  assert.equal((await update(revoked, ready())).status, 409);
  assert.equal((await req(`/projects/${projectId}/permission`, { enabled: true }, token)).status, 200);
  assert.equal((await reopen(revoked, token)).status, 404, 'Reauthorizing a project does not revive an old authorization generation');
  assert.equal((await update(revoked, ready())).status, 409);
  const deviceToken = await phone(), deviceTask = await task(deviceToken);
  assert.equal((await update(deviceTask, ready())).status, 200);
  assert.equal((await req('/device/revoke', {}, deviceToken)).status, 200);
  assert.equal((await reopen(deviceTask, deviceToken)).status, 401);
  assert.equal((await update(deviceTask, ready())).status, 409);
});

test('the Connector sees only eligible ready or requested previews, never revoked or stopped entries', async () => {
  const token = await phone(), deniedToken = await phone(), valid = await task(token), requested = await task(token), stopped = await task(token), denied = await task(deniedToken);
  for (const id of [valid, requested, stopped, denied]) assert.equal((await update(id, ready())).status, 200);
  assert.equal((await reopen(requested, token)).status, 200);
  assert.equal((await update(stopped, { status: 'stopped' })).status, 200);
  assert.equal((await req(`/projects/${projectId}/permission`, { enabled: false }, deniedToken)).status, 200);
  assert.equal((await req('/connector/previews', undefined, token)).status, 403);
  const result = await req('/connector/previews', undefined, connector);
  assert.equal(result.status, 200);
  const ids = result.data.tasks.map(item => item.id);
  assert.ok(ids.includes(valid));
  assert.ok(ids.includes(requested));
  assert.ok(!ids.includes(stopped));
  assert.ok(!ids.includes(denied));
  assert.ok(ids.indexOf(requested) < ids.indexOf(valid), 'Pending requests take priority over already ready previews');
});

test('actual preview expiry overrides a stale ready flag while explicit stopped status overrides a future expiry', async () => {
  const token = await phone(), expired = await task(token), stopped = await task(token);
  assert.equal((await update(expired, ready())).status, 200);
  assert.equal((await update(stopped, ready())).status, 200);
  const baseline = (await req('/tasks', undefined, token)).data;
  await db.prepare('UPDATE tasks SET preview_expires_at=1,updated_at=0 WHERE id=?').bind(expired).run();
  assert.equal((await update(stopped, { status: 'stopped' })).status, 200);
  const tasks = (await req('/tasks', undefined, token)).data.tasks;
  assert.equal(tasks.find(item => item.id === expired).preview_status, 'expired');
  assert.equal(tasks.find(item => item.id === stopped).preview_status, 'stopped');
  const delta = (await req(`/tasks?since=${baseline.syncCursor}`, undefined, token)).data;
  assert.equal(delta.tasks.find(item => item.id === expired).preview_status, 'expired', 'Clock expiry reaches incremental clients without a task update');
  assert.equal((await row(expired)).preview_status, 'ready', 'Expiry presentation does not rewrite the stored Connector state');
  assert.equal((await reopen(expired, token)).status, 200);
  assert.equal((await req('/tasks', undefined, token)).data.tasks.find(item => item.id === expired).preview_status, 'reopening', 'A pending reopen request must not be replaced by the expired old link state');
});

test('task deltas transfer changed rows and approvals with full ordered IDs, deletion and aggregate counts', async () => {
  const token = await phone(), unchanged = await task(token), changed = await task(token, 'running'), deleted = await task(token), waiting = await task(token, 'waiting_for_approval');
  const all = [unchanged, changed, deleted, waiting];
  await db.batch(all.map(id => db.prepare('UPDATE tasks SET updated_at=0 WHERE id=?').bind(id)));
  const waitingTask = await row(waiting), approvalId = randomUUID(), turnId = randomUUID();
  await db.prepare('UPDATE tasks SET turn_id=? WHERE id=?').bind(turnId, waiting).run();
  await db.prepare('INSERT INTO approvals (id,task_id,thread_id,turn_id,item_id,details,created_at,expires_at) VALUES (?,?,?,?,?,?,?,?)').bind(approvalId, waiting, waitingTask.thread_id, turnId, 'command-fixture', JSON.stringify({ command: 'echo synthetic', reason: 'Test-only fixture' }), Date.now(), Date.now() + 60000).run();
  const baseline = (await req('/tasks?since=0', undefined, token)).data;
  assert.equal(baseline.tasks.length, 4);
  assert.equal(baseline.taskIds.length, 4);
  assert.ok(Number.isSafeInteger(baseline.syncCursor));
  assert.deepEqual(baseline.taskIds, baseline.tasks.map(item => item.id));
  const noChanges = (await req(`/tasks?since=${baseline.syncCursor}`, undefined, token)).data;
  assert.deepEqual(noChanges.tasks.map(item => item.id), [waiting], 'Awaiting approvals refresh even when their task timestamp is unchanged');
  assert.equal(noChanges.tasks[0].approvals[0].id, approvalId);
  assert.deepEqual(noChanges.taskIds, baseline.taskIds);
  await db.prepare("UPDATE tasks SET status='completed',report='New verified report',updated_at=? WHERE id=?").bind(baseline.syncCursor, changed).run();
  await db.prepare('UPDATE approvals SET expires_at=1 WHERE id=?').bind(approvalId).run();
  assert.equal((await req(`/tasks/${deleted}`, undefined, token, 'DELETE')).status, 200);
  const delta = (await req(`/tasks?since=${baseline.syncCursor}`, undefined, token)).data;
  assert.deepEqual(new Set(delta.tasks.map(item => item.id)), new Set([changed, waiting]));
  assert.equal(delta.tasks.find(item => item.id === changed).report, 'New verified report');
  assert.deepEqual(delta.tasks.find(item => item.id === waiting).approvals, [], 'Expired command approvals disappear without rewriting the task timestamp');
  assert.equal(delta.tasks.some(item => item.id === unchanged), false);
  assert.ok(!delta.taskIds.includes(deleted));
  assert.equal(delta.taskIds.length, 3);
  assert.deepEqual(delta.stats, { total: 3, active: 1, completed: 2 });
  const legacy = (await req('/tasks', undefined, token)).data;
  assert.equal(legacy.tasks.length, 3);
  assert.deepEqual(legacy.taskIds, legacy.tasks.map(item => item.id));
  assert.ok(legacy.tasks.some(item => item.id === unchanged && item.report === 'Original verified report'));
  const other = await phone();
  assert.equal((await req('/tasks?since=0', undefined, other)).data.taskIds.length, 0);
});

test('an older active task remains visible after more than one hundred newer completed shares', async () => {
  const token = await phone(), active = await task(token, 'running'), fixture = await row(active);
  await db.prepare('UPDATE tasks SET created_at=0,updated_at=0 WHERE id=?').bind(active).run();
  await db.batch(Array.from({ length: 130 }, (_, index) => db.prepare("INSERT INTO tasks (id,device_id,project_id,project_name,content,message,status,created_at,updated_at,permission_version,execution_mode,report) VALUES (?,?,?,?,?,?,'completed',?,?,?,'direct',?)").bind(randomUUID(), fixture.device_id, projectId, 'Preview Fixture', 'Historical reference', '', index + 1, index + 1, fixture.permission_version, 'Historical verified report')));
  const full = (await req('/tasks', undefined, token)).data;
  assert.equal(full.tasks[0].id, active);
  assert.equal(full.tasks.length, 131);
  assert.equal(full.stats.active, 1);
  const delta = (await req(`/tasks?since=${full.syncCursor}`, undefined, token)).data;
  assert.deepEqual(delta.tasks, []);
  assert.equal(delta.taskIds.length, 131);
  assert.equal(delta.taskIds[0], active);
});

test('late lifecycle updates cannot erase a pending phone preview reopen request', async () => {
  const token = await phone(), id = await task(token);
  assert.equal((await update(id, ready())).status, 200);
  assert.equal((await reopen(id, token)).status, 200);
  const requested = await row(id);
  // The previous tunnel expires after the phone request but before the next Connector poll.
  await update(id, { status: 'expired' });
  const afterExpiry = await row(id);
  assert.equal(afterExpiry.preview_status, 'reopening');
  assert.equal(afterExpiry.preview_requested_at, requested.preview_requested_at);
  const polled = (await req('/connector/previews', undefined, connector)).data.tasks;
  assert.ok(polled.some(task => task.id === id && task.preview_status === 'reopening'));
  assert.equal((await update(id, ready({ requestedAt: requested.preview_requested_at }))).status, 200);
  assert.equal((await row(id)).preview_requested_at, null);
});

test('a stale reopen acknowledgement cannot consume a newer request', async () => {
  const token = await phone(), id = await task(token);
  await update(id, ready());
  await reopen(id, token);
  const first = (await row(id)).preview_requested_at;
  assert.equal((await update(id, ready({ requestedAt: first }))).status, 200);
  await reopen(id, token);
  const second = (await row(id)).preview_requested_at;
  assert.notEqual(second, first);
  await update(id, ready({ requestedAt: first }));
  const current = await row(id);
  assert.equal(current.preview_status, 'reopening');
  assert.equal(current.preview_requested_at, second);
});

test('a task update committed after a sync read is included by the following delta', async () => {
  const token = await phone(), id = await task(token, 'running');
  let release, entered;
  const held = new Promise(resolve => { release = resolve; });
  const intercepted = new Promise(resolve => { entered = resolve; });
  // Model D1/network latency: the writer computes its values before the reader,
  // but its SQL update reaches the database only after that reader has completed.
  const delayedDb = {
    prepare(sql) {
      const statement = db.prepare(sql);
      if (!sql.startsWith('UPDATE tasks SET status=')) return statement;
      return { bind(...values) { const bound = statement.bind(...values); return { async run() { entered(); await held; return bound.run(); } }; } };
    },
  };
  const pending = worker.fetch(new Request('https://droprun.test/connector/task', { method: 'POST', headers: { Authorization: 'Bearer ' + connector, 'Content-Type': 'application/json' }, body: JSON.stringify({ id, status: 'completed', report: 'A newly committed completion report' }) }), { DB: delayedDb, CONNECTOR_HASH: hash(connector) });
  await intercepted;
  let baseline;
  try { baseline = (await req('/tasks', undefined, token)).data; } finally { release(); }
  assert.equal((await pending).status, 200);
  const delta = (await req(`/tasks?since=${baseline.syncCursor}`, undefined, token)).data;
  assert.equal(delta.tasks.find(item => item.id === id)?.report, 'A newly committed completion report', 'a late commit must not fall permanently behind the phone cursor');
});

test('legacy timestamp cursors reconcile completely and then use database versions', async () => {
  const token=await phone(),id=await task(token);
  const reset=(await req('/tasks?since='+Date.now(),undefined,token)).data;
  assert.ok(reset.tasks.some(value=>value.id===id));
  assert.ok(reset.syncCursor>0&&reset.syncCursor<Date.now());
  assert.deepEqual((await req('/tasks?since='+reset.syncCursor,undefined,token)).data.tasks,[]);
});

test('sync-version migration preserves existing reports and is idempotent', async () => {
  const legacy=new Miniflare({modules:true,scriptPath:'relay/worker.mjs',compatibilityDate:'2026-08-06',d1Databases:['DB'],bindings:{CONNECTOR_HASH:hash(connector),ADMIN_HASH:hash(admin)}});
  try {
    const database=await legacy.getD1Database('DB');
    const schema=(await readFile('relay/schema.sql','utf8')).replace(/,\s*sync_version INTEGER NOT NULL DEFAULT 0/,'').split(/;\s*(?:\r?\n|$)/).filter(sql=>sql.trim()&&!sql.includes('task_sync_version'));
    for(const sql of schema) await database.prepare(sql).run();
    const id=randomUUID();
    await database.prepare("INSERT INTO tasks(id,device_id,project_id,project_name,content,message,status,report,created_at,updated_at) VALUES(?,?,?,?,?,?,'completed','Historical report',12,34)").bind(id,'legacy-device',projectId,'Legacy','old','old').run();
    const migrate=()=>legacy.dispatchFetch('https://droprun.test/admin/migrate',{method:'POST',headers:{Authorization:'Bearer '+admin},body:'{}'});
    const first=await migrate();assert.equal(first.status,200);assert.ok((await first.json()).added.includes('sync_version'));
    const migrated=await database.prepare('SELECT * FROM tasks WHERE id=?').bind(id).first();
    assert.equal(migrated.report,'Historical report');assert.equal(migrated.updated_at,34);assert.ok(migrated.sync_version>0);
    assert.deepEqual(await (await migrate()).json(),{ok:true,added:[]});
    assert.equal((await database.prepare('SELECT sync_version FROM tasks WHERE id=?').bind(id).first()).sync_version,migrated.sync_version);
    await database.prepare("UPDATE tasks SET title='Fresh title',updated_at=1 WHERE id=?").bind(id).run();
    assert.ok((await database.prepare('SELECT sync_version FROM tasks WHERE id=?').bind(id).first()).sync_version>migrated.sync_version);
  } finally { await legacy.dispose(); }
});
