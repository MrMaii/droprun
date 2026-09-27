import { test } from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { mkdtemp, mkdir, writeFile, readFile, readdir, rm, realpath } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomUUID } from 'node:crypto';
import { processProjectTask, prompt, developerInstructions } from '../connector/project-task.mjs';
import { renderPlan, projectTurnPolicy, assertExecutionApproved } from '../connector/plan.mjs';
import { requireTaskPermission } from '../connector/permissions.mjs';

const plan = '## 我看到了什么\n测试材料\n\n## 我理解你的意思\n新增 result.txt\n\n## 我准备怎么做\n1. 新增结果文件并读取核对\n\n## 可能的影响\n会写入原项目\n\n## 材料读取限制\n无';
const human = '## 我看到了什么\n收到材料\n\n## 用在哪里\n测试项目\n\n## 我做了什么\n原项目已写入结果\n\n## 验证\n读回了 result.txt\n\n## 未完成\n无';
const evidence = { outcome: 'completed', artifacts: ['result.txt'], verification: ['read result.txt'], remaining: [] };

async function fixture(t, mode = 'review', { withEvidence = true } = {}) {
  const root = await realpath(await mkdtemp(join(tmpdir(), 'droprun-project-task-')));
  t.after(() => rm(root, { recursive: true, force: true }));
  const source = join(root, 'source'), stateDir = join(root, 'state');
  await mkdir(source); await mkdir(stateDir); await writeFile(join(source, 'user.txt'), 'UNCOMMITTED_USER_WORK');
  const task = { id: randomUUID(), project_id: randomUUID(), permission_version: randomUUID(), execution_mode: mode, project_name: 'Fixture', content: 'https://example.test/reel/1', message: 'Create result.txt', status: 'reading', model: 'gpt-test', effort: 'high' };
  const calls = [], uploads = [], turns = [], threads = [], opened = [], runners = [];
  const permission = () => ({ taskId: task.id, projectId: task.project_id, enabled: true, version: task.permission_version, profile: 'original-project', executionMode: task.execution_mode, planDecision: task.plan_decision || null, planVersion: task.plan_version || null });
  let planFailures = 0;
  const api = async (path, body) => {
    calls.push({ path, body: body && structuredClone(body) });
    if (path.endsWith('/permission')) return permission();
    if (path.endsWith('/plan')) {
      if (planFailures > 0) { planFailures--; throw new Error('network unavailable'); }
      Object.assign(task, { status: 'awaiting_plan_approval', thread_id: body.threadId, plan_turn_id: body.turnId, plan_report: body.report, plan_version: body.planVersion });
      return { ...task };
    }
    if (path === '/connector/task') { task.status = body.status; if (body.threadId) task.thread_id = body.threadId; if (body.turnId) task.turn_id = body.turnId; if (body.title) task.title = body.title; if (body.materialSummary) task.material_summary = body.materialSummary; return { ...task }; }
    if (path.endsWith('/invalidate')) return { ok: true };
    throw new Error('Unexpected API: ' + path);
  };
  class Runner extends EventEmitter {
    async call(method, params) {
      calls.push({ method, params });
      if (method === 'thread/start') { const thread = { id: randomUUID(), projectId: task.project_id }; threads.push(thread); return { thread }; }
      if (method === 'thread/read' || method === 'thread/resume') return { thread: { id: params.threadId, projectId: task.project_id, status: { type: 'idle' } } };
      if (method === 'thread/name/set') return {};
      if (method === 'mcpServerStatus/list') return { data: [] };
      if (method !== 'turn/start') throw new Error('Unexpected method: ' + method);
      turns.push(params);
      const id = randomUUID(), threadId = params.threadId;
      let raw, items = [];
      if (params.outputSchema) raw = JSON.stringify(evidence);
      else if (params.sandboxPolicy.type === 'readOnly') raw = plan;
      else {
        await writeFile(join(source, 'result.txt'), 'ACTUAL_RESULT');
        items = [{ id: 'edit', type: 'fileChange', status: 'completed', changes: [{ path: join(source, 'result.txt'), kind: { type: 'add' } }] }, { id: 'check', type: 'commandExecution', command: 'read result.txt', cwd: source, status: 'completed', exitCode: 0 }];
        raw = human + (withEvidence ? '\n\n```droprun\n' + JSON.stringify(evidence) + '\n```' : '');
      }
      const all = [...items, { id: 'report-' + turns.length, type: 'agentMessage', text: raw }];
      setImmediate(() => {
        for (const item of all) this.emit('event', { method: 'item/completed', params: { threadId, turnId: id, item } });
        this.emit('event', { method: 'turn/completed', params: { threadId, turn: { id, status: 'completed', items: all } } });
      });
      return { turn: { id } };
    }
    send() { throw new Error('No approval expected in fixture'); }
    close() { this.closed = true; }
  }
  const context = {
    codex: { projects: async () => [{ id: task.project_id, roots: [{ path: source }] }] }, api,
    save: async (id, state) => writeFile(join(stateDir, id + '.json'), JSON.stringify(state)), stateDir, config: {},
    openDesktop: id => { assert.ok(turns.some(turn => turn.threadId === id), 'Only open a thread after its first turn has started and can be persisted'); opened.push(id); }, upload: async (id, file, bytes) => { uploads.push({ id, file, bytes }); },
    startRunner: async (catalog, cwd) => { assert.equal(cwd, source); const runner = new Runner(); runners.push(runner); return runner; },
    prepareMaterial: async () => ({ files: [], limitations: [], summary: '视频 0:32 · 已转写', brief: '链接：https://example.test/reel/1\n标题：示例视频', digest: { title: '示例视频' } }), wait: () => new Promise(resolve => setImmediate(resolve))
  };
  return { root, source, stateDir, task, context, calls, uploads, turns, threads, opened, runners, failPlans: count => { planFailures = count; } };
}

test('default review only plans, frees the runner, then approval continues the same thread in the original project', async t => {
  const f = await fixture(t);
  const planned = await processProjectTask(f.task, f.context);
  assert.equal(planned.status, 'awaiting_plan_approval');
  assert.equal(f.turns.length, 1); assert.equal(f.turns[0].approvalPolicy, 'never');
  assert.deepEqual(f.turns[0].sandboxPolicy, { type: 'readOnly', networkAccess: false });
  assert.equal(f.turns[0].cwd, f.source); assert.equal(f.turns[0].outputSchema, undefined);
  assert.deepEqual(await readdir(f.source), ['user.txt']);
  assert.ok(f.runners[0].closed); assert.equal(f.uploads.length, 0);
  assert.equal(f.task.plan_report, plan);
  Object.assign(f.task, { plan_decision: 'approved', status: 'reading' });
  const finished = await processProjectTask(f.task, f.context);
  assert.equal(finished.status, 'completed'); assert.equal(f.threads.length, 1);
  assert.equal(f.turns[0].threadId, f.turns[1].threadId);
  assert.equal(f.turns[1].cwd, f.source); assert.equal(f.turns[1].sandboxPolicy.type, 'workspaceWrite');
  assert.match(f.turns[1].input[0].text, /计划已批准/);
  assert.equal(await readFile(join(f.source, 'result.txt'), 'utf8'), 'ACTUAL_RESULT');
  assert.equal(await readFile(join(f.source, 'user.txt'), 'utf8'), 'UNCOMMITTED_USER_WORK');
  assert.deepEqual(f.uploads.map(value => value.file.name), ['result.txt']);
  assert.ok(!f.calls.some(call => call.path === '/connector/task' && call.body.status === 'completed' && call.body.turnId === f.task.plan_turn_id));
  assert.ok(finished.report.startsWith('## 我看到了什么'));
});

test('direct mode skips planning and completes original-directory work with verified artifacts', async t => {
  const f = await fixture(t, 'direct');
  const finished = await processProjectTask(f.task, f.context);
  assert.equal(finished.status, 'completed'); assert.equal(f.turns.length, 1);
  assert.equal(f.turns[0].sandboxPolicy.type, 'workspaceWrite');
  assert.ok(!f.calls.some(call => call.path?.endsWith('/plan')));
  assert.deepEqual(f.uploads.map(value => value.file.name), ['result.txt']);
  assert.match(finished.report, /## 执行记录/);
});

test('the visible Codex message is the note plus extracted material; rules and format stay in developer instructions', async t => {
  const f = await fixture(t, 'direct');
  await processProjectTask(f.task, f.context);
  const visible = f.turns[0].input.filter(item => item.type === 'text').map(item => item.text).join('\n');
  assert.ok(visible.startsWith('Create result.txt'), visible);
  assert.match(visible, /标题：示例视频/);
  assert.ok(!/droprun|JSON|Schema|凭据|sha256/i.test(visible), visible);
  const start = f.calls.find(call => call.method === 'thread/start').params;
  assert.match(start.developerInstructions, /```droprun/); assert.match(start.developerInstructions, /verification/);
  assert.match(start.developerInstructions, /receipts\.json/);
  assert.equal(start.model, 'gpt-test');
  assert.equal(f.turns[0].model, 'gpt-test'); assert.equal(f.turns[0].effort, 'high');
  assert.equal(f.task.title, 'Create result.txt'); assert.equal(f.task.material_summary, '视频 0:32 · 已转写');
  const named = f.calls.find(call => call.method === 'thread/name/set').params;
  assert.equal(named.name, 'DropRun · Create result.txt');
  assert.match(developerInstructions(f.task, true), /只读/); assert.ok(!/```droprun/.test(developerInstructions(f.task, true)));
  assert.match(prompt({ ...f.task, message: '' }, { material: { brief: 'x' } }, false), /看看这个对项目有没有用/);
});

test('contact sheets are attached as images, screenshots the connector took ride along as trusted deliverables', async t => {
  const f = await fixture(t, 'direct');
  const shot = join(f.root, 'after.png'); await writeFile(shot, Buffer.from([137, 80, 78, 71]));
  f.context.prepareMaterial = async () => ({ files: [join(f.root, 'frame-001.jpg'), join(f.root, 'frame-002.jpg')], images: [join(f.root, 'sheet-1.jpg')], limitations: [], summary: '视频 0:20 · 20 张关键帧', brief: '画面：联络表', digest: { title: '' } });
  f.context.onProject = async cwd => { f.projectSeen = cwd; };
  f.context.screenshotsFor = id => id === f.task.id ? [{ name: 'after.png', path: shot, url: 'http://127.0.0.1:3000/' }] : [];
  const finished = await processProjectTask(f.task, f.context);
  assert.equal(finished.status, 'completed', finished.error);
  assert.equal(f.projectSeen, f.source);
  assert.deepEqual(f.turns[0].input.filter(part => part.type === 'localImage').map(part => part.path), [join(f.root, 'sheet-1.jpg')]);
  assert.deepEqual(f.uploads.map(value => value.file.name).sort(), ['after.png', 'result.txt']);
  assert.equal(f.uploads.find(value => value.file.name === 'after.png').file.kind, 'artifact');
  assert.deepEqual(finished.uploadedScreenshots, ['after.png']);
  assert.match(f.calls.find(call => call.method === 'thread/start').params.developerInstructions, /47493\/preview/);
});

test('a human report without the evidence block gets one constrained follow-up turn, not a rerun', async t => {
  const f = await fixture(t, 'direct', { withEvidence: false });
  const finished = await processProjectTask(f.task, f.context);
  assert.equal(finished.status, 'completed', finished.error); assert.equal(f.turns.length, 2);
  assert.equal(f.turns[1].threadId, f.turns[0].threadId);
  assert.deepEqual(f.turns[1].outputSchema.required, ['outcome', 'artifacts', 'verification', 'remaining']);
  assert.match(f.turns[1].input[0].text, /证据/);
  assert.ok(finished.report.startsWith('## 我看到了什么')); assert.ok(!finished.report.includes('```'));
  assert.deepEqual(f.uploads.map(value => value.file.name), ['result.txt']);
});

test('a runner killed mid-turn is reopened on the same thread once and the task still completes', async t => {
  const f = await fixture(t, 'direct');
  let killed = false;
  const original = f.context.startRunner;
  f.context.startRunner = async (catalog, cwd) => {
    const runner = await original(catalog, cwd);
    if (!killed) {
      const call = runner.call.bind(runner);
      runner.call = async (method, params) => {
        if (method === 'turn/start' && !killed) { killed = true; f.turns.push(params); setImmediate(() => runner.emit('disconnected', -1)); return { turn: { id: 'lost-turn' } }; }
        return call(method, params);
      };
    }
    return runner;
  };
  const finished = await processProjectTask(f.task, f.context);
  assert.equal(finished.status, 'completed', finished.error);
  assert.equal(f.runners.length, 2); assert.ok(f.runners[0].closed);
  assert.equal(f.threads.length, 1);
  const resumed = f.calls.filter(call => call.method === 'thread/resume');
  assert.equal(resumed.length, 1); assert.equal(resumed[0].params.threadId, f.threads[0].id);
  assert.match(f.turns.at(-1).input[0].text, /中断/);
  assert.equal(finished.resumedAfterDisconnect, true);
});

test('a large historical artifacts directory no longer blocks creation or causes a project copy', async t => {
  const f = await fixture(t);
  const artifacts = join(f.source, 'artifacts'); await mkdir(artifacts);
  for (let start = 0; start < 20001; start += 128) await Promise.all(Array.from({ length: Math.min(128, 20001 - start) }, (_, i) => writeFile(join(artifacts, String(start + i)), '')));
  const result = await processProjectTask(f.task, f.context);
  assert.equal(result.status, 'awaiting_plan_approval'); assert.equal(f.threads.length, 1);
  assert.deepEqual((await readdir(f.root)).sort(), ['source', 'state']);
  assert.equal((await readdir(artifacts)).length, 20001);
});

test('offline plan publishing remains retryable across recovery without another Codex turn', async t => {
  const f = await fixture(t); f.failPlans(2);
  await assert.rejects(processProjectTask(f.task, f.context), /network unavailable/);
  await assert.rejects(processProjectTask(f.task, f.context, true), /network unavailable/);
  assert.equal(JSON.parse(await readFile(join(f.stateDir, f.task.id + '.json'))).planReady, true);
  const result = await processProjectTask(f.task, f.context, true);
  assert.equal(result.status, 'awaiting_plan_approval'); assert.equal(f.turns.length, 1);
});

test('recovery after approval starts one execution rather than interpreting the earlier plan as a delivery', async t => {
  const f = await fixture(t);
  await processProjectTask(f.task, f.context);
  Object.assign(f.task, { plan_decision: 'approved', status: 'reading' });
  const result = await processProjectTask(f.task, f.context, true);
  assert.equal(result.status, 'completed'); assert.equal(f.turns.length, 2);
  assert.equal(f.threads.length, 1);
});

test('a lost plan response followed by phone approval does not strand execution in the planning branch', async t => {
  const f = await fixture(t);
  await processProjectTask(f.task, f.context);
  const path = join(f.stateDir, f.task.id + '.json'), saved = JSON.parse(await readFile(path));
  saved.planReady = true; saved.status = 'planning'; await writeFile(path, JSON.stringify(saved));
  Object.assign(f.task, { plan_decision: 'approved', status: 'reading' });
  const result = await processProjectTask(f.task, f.context, true);
  assert.equal(result.status, 'completed'); assert.equal(f.turns.length, 2);
  assert.equal(f.uploads.length, 1); assert.equal(f.threads.length, 1);
});

test('original project cannot use stale isolated permissions or a mismatched approved plan', async () => {
  const task = { id: 'task', project_id: 'project', permission_version: 'v', execution_mode: 'review', plan_decision: 'approved', plan_version: 'p', plan_report: 'Plan' };
  await assert.rejects(requireTaskPermission(task, async () => ({ taskId: 'task', projectId: 'project', enabled: true, version: 'v', profile: 'isolated-workspace' })), /授权已失效/);
  for (const permission of [{ executionMode: 'direct' }, { executionMode: 'review', planDecision: null, planVersion: 'p' }, { executionMode: 'review', planDecision: 'approved', planVersion: 'other' }]) assert.throws(() => assertExecutionApproved(task, permission), /尚未获手机批准/);
});

test('planning output must be non-empty markdown and planning policy has no write roots or escalation', () => {
  assert.throws(() => renderPlan(''), /没有给出理解与计划/);
  assert.throws(() => renderPlan('   \n```droprun\n{}\n```'), /没有给出理解与计划/);
  assert.equal(renderPlan(plan + '\n\n```droprun\n{"outcome":"completed","artifacts":[],"verification":[],"remaining":[]}\n```'), plan);
  assert.deepEqual(projectTurnPolicy('original', true), { type: 'readOnly', networkAccess: false });
  assert.throws(() => renderPlan('x'.repeat(100001)), /过长/);
});
