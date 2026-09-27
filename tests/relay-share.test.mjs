import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare } from './miniflare.mjs';
import { readFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';

const hash = s => createHash('sha256').update(s).digest('hex');
const connector = 'share-test-connector-secret-long';
const admin = 'share-test-admin-secret-long-enough';
const projectId = randomUUID();
let mf, db, phone;
async function req(path, body, token = phone, method = body ? 'POST' : 'GET') {
  const r = await mf.dispatchFetch('https://droprun.test' + path, { method, headers: { Authorization: 'Bearer ' + (token || ''), 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
  const type = r.headers.get('content-type') || '';
  return { status: r.status, data: type.includes('json') ? await r.json() : await r.text() };
}
const models = [{ id: 'gpt-test', displayName: 'GPT Test', efforts: ['low', 'medium', 'high'], defaultEffort: 'medium', isDefault: true }, { id: 'gpt-fast', displayName: 'GPT Fast', efforts: ['low'], defaultEffort: 'low', isDefault: false }];

before(async () => {
  mf = new Miniflare({ modules: true, scriptPath: 'relay/worker.mjs', compatibilityDate: '2026-08-06', d1Databases: ['DB'], r2Buckets: ['FILES'], bindings: { CONNECTOR_HASH: hash(connector), ADMIN_HASH: hash(admin) } });
  db = await mf.getD1Database('DB');
  for (const sql of (await readFile('relay/schema.sql', 'utf8')).split(/;\s*(?:\r?\n|$)/).filter(s => s.trim())) await db.prepare(sql).run();
  const issued = await req('/connector/pairing-code', {}, connector);
  phone = (await req('/pair', { code: issued.data.code })).data.token;
  await req('/connector/sync', { name: 'Studio PC', projects: [{ id: projectId, name: 'Site' }], models }, connector);
  await req('/projects/' + projectId + '/permission', { enabled: true });
});
after(async () => { await mf?.dispose(); });

test('a freshly paired phone starts in direct mode and can still opt into plan review', async () => {
  assert.deepEqual((await req('/device/settings')).data, { directExecution: true });
  assert.equal((await req('/device/settings', { directExecution: false })).data.directExecution, false);
  assert.equal((await req('/device/settings', { directExecution: true })).status, 400);
  assert.equal((await req('/device/settings', { directExecution: true, riskAccepted: true })).data.directExecution, true);
});

test('the computer announces its model catalog and the phone reads it with the projects', async () => {
  const data = (await req('/projects')).data;
  assert.equal(data.name, 'Studio PC');
  assert.deepEqual(data.models, models);
  await req('/connector/sync', { projects: [{ id: projectId, name: 'Site' }], models: [{ id: 'bad id!', efforts: ['x'] }, { id: 'ok', efforts: ['low', 42, 'BAD'], defaultEffort: 'nope' }] }, connector);
  assert.deepEqual((await req('/projects')).data.models, [{ id: 'ok', displayName: 'ok', efforts: ['low'], defaultEffort: null, isDefault: false }]);
  await req('/connector/sync', { name: 'Studio PC', projects: [{ id: projectId, name: 'Site' }], models }, connector);
});

test('tasks carry a validated model, effort and title; followups inherit them', async () => {
  const create = extra => req('/tasks', { id: randomUUID(), projectId, content: 'https://example.test/reel/1', message: 'Use it', assets: [], ...extra });
  const task = (await create({ model: 'gpt-test', effort: 'high', title: '  Use it  ' })).data;
  assert.equal(task.model, 'gpt-test'); assert.equal(task.effort, 'high'); assert.equal(task.title, 'Use it'); assert.equal(task.execution_mode, 'direct');
  const plain = (await create({})).data;
  assert.equal(plain.model, null); assert.equal(plain.effort, null); assert.equal(plain.title, null);
  for (const bad of [{ model: 'unknown-model' }, { model: 'gpt-fast', effort: 'high' }, { effort: 'HIGH' }, { model: 'x y' }]) assert.equal((await create(bad)).status, 400, JSON.stringify(bad));
  assert.equal((await create({ effort: 'low' })).status, 200);
  await db.prepare("UPDATE tasks SET status='completed',thread_id=? WHERE id=?").bind(randomUUID(), task.id).run();
  const child = (await req('/tasks/' + task.id + '/followup', { id: randomUUID(), message: 'Again' })).data;
  assert.equal(child.model, 'gpt-test'); assert.equal(child.effort, 'high'); assert.equal(child.title, 'Use it');
  await db.prepare("UPDATE tasks SET status='completed' WHERE id=?").bind(child.id).run();
  const switched = (await req('/tasks/' + child.id + '/followup', { id: randomUUID(), message: 'Faster', model: 'gpt-fast', effort: 'low' })).data;
  assert.equal(switched.model, 'gpt-fast'); assert.equal(switched.effort, 'low');
});

test('the task list carries counts for the dashboard and the connector can attach a title and material summary', async () => {
  const list = (await req('/tasks')).data;
  assert.equal(list.stats.total, list.tasks.length); assert.ok(list.stats.active >= 1); assert.ok(list.stats.completed >= 1);
  assert.equal((await req('/tasks', null, connector)).data.stats, undefined);
  const task = list.tasks.filter(t => t.status === 'queued').at(-1);
  const claimed = (await req('/connector/claim', {}, connector)).data.task;
  assert.equal(claimed.id, task.id);
  const updated = (await req('/connector/task', { id: task.id, status: 'reading', title: 'Reel about onboarding', materialSummary: '视频 0:32 · 已转写 · 4 张关键帧' }, connector)).data;
  assert.equal(updated.title, 'Reel about onboarding'); assert.equal(updated.material_summary, '视频 0:32 · 已转写 · 4 张关键帧');
  const again = (await req('/connector/task', { id: task.id, status: 'running', threadId: randomUUID() }, connector)).data;
  assert.equal(again.title, 'Reel about onboarding');
  assert.equal((await req('/tasks')).data.tasks.find(t => t.id === task.id).material_summary, '视频 0:32 · 已转写 · 4 张关键帧');
});

test('only the administrator can add missing columns; the connector attaches permitted preview links', async () => {
  await db.prepare('ALTER TABLE tasks DROP COLUMN preview_url').run();
  assert.equal((await req('/connector/migrate', {}, connector)).status,403);
  assert.deepEqual((await req('/admin/migrate', {}, admin)).data, { ok: true, added: ['preview_url'] });
  assert.deepEqual((await req('/admin/migrate', {}, admin)).data, { ok: true, added: [] });
  assert.equal((await req('/connector/migrate', {})).status, 403);
  const task = (await req('/tasks', { id: randomUUID(), projectId, content: 'https://example.test/reel/2', message: 'Preview it', assets: [] })).data;
  await db.prepare("UPDATE tasks SET status='reading' WHERE id=?").bind(task.id).run();
  const expiresAt = Date.now() + 1800000;
  const updated = (await req('/connector/task', { id: task.id, status: 'running', threadId: randomUUID(), previewUrl: 'https://demo.trycloudflare.com/about?k=abc', previewExpiresAt: expiresAt }, connector)).data;
  assert.equal(updated.preview_url, 'https://demo.trycloudflare.com/about?k=abc'); assert.equal(updated.preview_expires_at, expiresAt);
  const rejected = (await req('/connector/task', { id: task.id, status: 'running', previewUrl: 'http://insecure.example/x', previewExpiresAt: expiresAt }, connector)).data;
  assert.equal(rejected.preview_url, 'https://demo.trycloudflare.com/about?k=abc');
  const listed = (await req('/tasks')).data.tasks.find(t => t.id === task.id);
  assert.equal(listed.preview_url, 'https://demo.trycloudflare.com/about?k=abc'); assert.equal(listed.deliverable_count, 0);
});

test('the pairing page and app links are public, while the code itself never reaches the server', async () => {
  const page = await req('/pair', null, '');
  assert.equal(page.status, 200); assert.match(page.data, /intent:\/\/pair/); assert.match(page.data, /download\.apk/);
  const links = await req('/.well-known/assetlinks.json', null, '');
  assert.equal(links.status, 200); assert.deepEqual(links.data, [], 'Self-hosting does not publish a private debug signing certificate');
  assert.equal((await req('/health', null, '')).data.version, '0.5.0');
});
