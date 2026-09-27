import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomUUID } from "node:crypto";
import { processProjectTask } from '../connector/project-task.mjs';

async function fixture(t, { mode = 'direct', artifact = 'result.txt' } = {}) {
  const root = await mkdtemp(join(tmpdir(), 'droprun-project-recovery-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const source = join(root, 'source'), stateDir = join(root, 'state');
  await mkdir(source); await mkdir(stateDir);
  const bytes = 'SYNTHETIC_NON_SECRET_FIXTURE';
  await writeFile(join(source, artifact), bytes);
  const task = { id: randomUUID(), project_id: randomUUID(), permission_version: randomUUID(), execution_mode: mode, status: mode === 'review' ? 'planning' : 'running', cancel_requested: 0 };
  const threadId = randomUUID(), turnId = randomUUID();
  const report = '## 我看到了什么\nSynthetic material\n\n## 用在哪里\nSynthetic project\n\n## 我做了什么\nCreated synthetic fixture\n\n## 验证\nsynthetic read\n\n## 未完成\n无\n\n```droprun\n' + JSON.stringify({ outcome: 'completed', artifacts: [artifact], verification: ['synthetic read'], remaining: [] }) + '\n```';
  const items = [
    { id: 'check', type: 'commandExecution', command: 'synthetic read', cwd: source, status: 'completed', exitCode: 0 },
    { id: 'report', type: 'agentMessage', text: report }
  ];
  const state = {
    id: task.id, status: task.status, stage: mode === 'review' ? 'planning' : 'execution',
    threadId, turnId, turnRequested: true, turnStartedAt: Date.now(), cwd: source, events: [], executionEvents: [],
    ...(mode === 'review' ? { planReady: true, planTurnId: turnId, planVersion: randomUUID(), planReport: 'A completed read-only plan awaiting publication' } : {})
  };
  const statePath = join(stateDir, task.id + '.json');
  await writeFile(statePath, JSON.stringify(state));
  const calls = { turnReads: 0, planPublications: 0, uploads: 0, finalPublications: 0 };
  const hooks = { permissionEnabled: true, onPlan: null, onUpload: null, onTask: null };
  const context = {
    stateDir,
    save: async (id, state) => writeFile(join(stateDir, id + '.json'), JSON.stringify(state)),
    codex: { call: async (method, params) => {
      assert.equal(method, 'thread/read'); assert.equal(params.threadId, threadId); calls.turnReads++;
      return { thread: { id: threadId, turns: [{ id: turnId, status: 'completed', items }] } };
    } },
    startRunner: async () => { throw new Error('Recovery must not launch another turn'); },
    api: async (path, body) => {
      if (path.endsWith('/invalidate')) return { ok: true };
      if (path.endsWith('/permission')) return { enabled: hooks.permissionEnabled };
      if (path.endsWith('/plan')) {
        calls.planPublications++; await hooks.onPlan?.();
        task.status = 'awaiting_plan_approval'; return { ...task };
      }
      assert.equal(path, '/connector/task');
      await hooks.onTask?.(body);
      calls.finalPublications++; task.status = body.status;
      return { ...task };
    },
    upload: async (id, file, content) => {
      assert.equal(id, task.id); assert.equal(file.name, artifact); assert.equal(content.toString(), bytes);
      calls.uploads++; await hooks.onUpload?.(); return file;
    }
  };
  return { task, context, hooks, calls, source, statePath, saved: async () => JSON.parse(await readFile(statePath, 'utf8')) };
}

test('a permanently undeliverable sensitive artifact becomes blocked and never loops or starts another turn', async t => {
  const f = await fixture(t, { artifact: '.env' });
  const result = await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(result.status, 'blocked'); assert.match(result.error, /交付含敏感路径/);
  assert.equal(f.task.status, 'blocked'); assert.equal((await f.saved()).deliveryPending, false);
  assert.equal(f.calls.uploads, 0); assert.equal(f.calls.turnReads, 1);
  await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(f.calls.turnReads, 1); assert.equal(f.calls.uploads, 0);
  assert.equal(await readFile(join(f.source, '.env'), 'utf8'), 'SYNTHETIC_NON_SECRET_FIXTURE');
});

test('a transport upload failure retries the completed turn delivery without executing again', async t => {
  const f = await fixture(t);
  f.hooks.onUpload = () => { if (f.calls.uploads === 1) throw new Error('synthetic network interruption'); };
  await assert.rejects(processProjectTask({ ...f.task }, f.context, true), /synthetic network interruption/);
  assert.equal(f.task.status, 'running'); assert.equal((await f.saved()).deliveryPending, true);
  const result = await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(result.status, 'completed'); assert.equal(f.calls.uploads, 2);
  assert.equal(f.calls.finalPublications, 1); assert.equal((await f.saved()).deliveryPending, false);
});

test('a non-retryable upload response publishes a blocked result instead of retrying indefinitely', async t => {
  const f = await fixture(t);
  f.hooks.onUpload = () => { const error = new Error('synthetic HTTP 413'); error.retryable = false; throw error; };
  const result = await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(result.status, 'blocked'); assert.match(result.error, /HTTP 413/);
  await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(f.calls.uploads, 1); assert.equal(f.calls.turnReads, 1);
});

test('a lost final report response retries only publication, preserving already uploaded artifacts', async t => {
  const f = await fixture(t); let lost = false;
  f.hooks.onTask = body => { if (body.status === 'completed' && !lost) { lost = true; throw new Error('synthetic final response lost'); } };
  await assert.rejects(processProjectTask({ ...f.task }, f.context, true), /synthetic final response lost/);
  assert.equal((await f.saved()).status, 'completed'); assert.equal(f.calls.uploads, 1);
  const result = await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(result.status, 'completed'); assert.equal(f.calls.uploads, 1); assert.equal(f.calls.turnReads, 1);
});

test('cancellation between the final permission check and plan publication resolves on recovery', async t => {
  const f = await fixture(t, { mode: 'review' });
  f.hooks.onPlan = () => { f.task.cancel_requested = 1; throw new Error('409 plan cancelled during publication'); };
  await assert.rejects(processProjectTask({ ...f.task }, f.context, true), /409 plan cancelled/);
  assert.equal(f.calls.planPublications, 1); assert.equal((await f.saved()).planReady, true);
  const result = await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(result.status, 'cancelled'); assert.equal(f.task.status, 'cancelled');
  assert.equal((await f.saved()).planReady, false); assert.equal(f.calls.planPublications, 1); assert.equal(f.calls.turnReads, 0);
});

test('revoked authorization before publishing a completed plan cancels without offering approval or rerunning', async t => {
  const f = await fixture(t, { mode: 'review' }); f.hooks.permissionEnabled = false;
  const result = await processProjectTask({ ...f.task }, f.context, true);
  assert.equal(result.status, 'cancelled'); assert.equal(f.calls.planPublications, 0); assert.equal(f.calls.turnReads, 0);
  assert.equal((await f.saved()).planReady, false);
});
