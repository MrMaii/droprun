import { test } from 'node:test';
import assert from 'node:assert/strict';
import { openConversation, recoveryTurn } from '../connector/conversation.mjs';
const project = { id: 'project-a' }, cwd = 'C:/projects/test';
test('new shares create a thread but followups resume the exact original thread', async () => {
  const calls = [];
  const codex = { call: async (method, params) => { calls.push({ method, params }); return { thread: { id: method === 'thread/start' ? 'new' : 'original', projectId: project.id, status: { type: 'idle' } } }; } };
  assert.equal((await openConversation(codex, {}, project, cwd)).id, 'new');
  assert.deepEqual(calls[0].params.runtimeWorkspaceRoots, [cwd]);
  assert.equal(calls[0].params.approvalPolicy, 'on-request');
  assert.equal(calls[0].params.approvalsReviewer, 'user');
  calls.length = 0;
  assert.equal((await openConversation(codex, { parent_task_id: 'parent', thread_id: 'original' }, project, cwd)).id, 'original');
  assert.deepEqual(calls.map(call => call.method), ['thread/read', 'thread/resume']);
  assert.equal(calls[1].params.threadId, 'original');
  assert.deepEqual(calls[1].params.runtimeWorkspaceRoots, [cwd]);
});
test('missing thread, wrong project and active conversation never create replacement threads', async () => {
  const calls = [];
  const task = { parent_task_id: 'parent', thread_id: 'original' };
  for (const original of [{ projectId: 'other' }, { projectId: project.id, status: { type: 'active' } }]) {
    await assert.rejects(openConversation({ call: async method => { calls.push(method); return { thread: original }; } }, task, project, cwd));
  }
  await assert.rejects(openConversation({ call: async method => { calls.push(method); } }, { parent_task_id: 'parent' }, project, cwd));
  assert.ok(!calls.includes('thread/start')); assert.ok(!calls.includes('thread/resume'));
});
test('recovery uses this task turn ID, never the parent or a later unrelated turn', () => {
  const thread = { turns: [{ id: 'parent', status: 'completed' }, { id: 'ours', status: 'failed' }, { id: 'later', status: 'completed' }] };
  assert.throws(() => recoveryTurn(thread, {}, {}));
  assert.equal(recoveryTurn(thread, { turnId: 'ours' }, {}).status, 'failed');
  assert.equal(recoveryTurn(thread, {}, { turn_id: 'ours' }).id, 'ours');
  assert.throws(() => recoveryTurn(thread, { turnId: 'missing' }, {}));
});
