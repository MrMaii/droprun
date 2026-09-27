import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, mkdir, symlink, rm, utimes } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createHash } from 'node:crypto';
import { validateDelivery, executionEvent, changeFingerprint, changedSince } from '../connector/delivery.mjs';
import { finishReport } from '../connector/report.mjs';

const sha = text => createHash('sha256').update(text).digest('hex');
const command = { id: 'check-1', type: 'command', command: 'node --test', cwd: 'workspace', status: 'completed', exitCode: 0 };
const report = { outcome: 'completed', artifacts: ['result.txt'], verification: ['node --test'], remaining: [] };
async function fixture(t) {
  const root = await mkdtemp(join(tmpdir(), 'droprun-delivery-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  await writeFile(join(root, 'result.txt'), 'actual');
  return { cwd: root, events: [command], changedFiles: ['result.txt'] };
}
test('completion requires a real artifact produced this turn and a successful recorded verification', async t => {
  const context = await fixture(t);
  const result = await validateDelivery(report, context);
  assert.equal(result.passed, true, result.errors.join(';'));
  assert.equal(result.artifacts[0].sha256, sha('actual')); assert.equal(result.artifacts[0].bytes, 6);
  assert.equal(result.verification[0].event.id, 'check-1');
});
test('missing, outside, unsafe, directory and symlink artifacts cannot pass', async t => {
  const context = await fixture(t);
  await mkdir(join(context.cwd, 'directory'));
  await symlink(context.cwd, join(context.cwd, 'linked'), 'junction');
  for (const path of ['missing.txt', '../outside.txt', 'result.txt:stream', 'directory', 'linked/result.txt']) {
    const result = await validateDelivery({ ...report, artifacts: [path] }, { ...context, changedFiles: [path] });
    assert.equal(result.passed, false, path);
  }
});
test('an artifact neither recorded as changed nor modified since the turn started is rejected', async t => {
  const context = await fixture(t);
  const old = new Date(Date.now() - 3600000); await utimes(join(context.cwd, 'result.txt'), old, old);
  assert.equal((await validateDelivery(report, { ...context, changedFiles: [], startedAt: Date.now() - 60000 })).passed, false);
  assert.equal((await validateDelivery(report, { ...context, changedFiles: [], startedAt: Date.now() - 7200000 })).passed, true);
  assert.equal((await validateDelivery(report, { ...context, changedFiles: ['result.txt'] })).passed, true);
});
test('verification must quote a command that actually ran and succeeded', async t => {
  const context = await fixture(t);
  for (const events of [[], [{ ...command, exitCode: 1, status: 'failed' }], [{ ...command, exitCode: null }]]) assert.equal((await validateDelivery(report, { ...context, events })).passed, false);
  assert.equal((await validateDelivery({ ...report, verification: ['pytest'] }, context)).passed, false);
  const wrapped = { ...command, command: 'bash -lc "npm test -- --runInBand"' };
  assert.equal((await validateDelivery({ ...report, verification: ['npm test'] }, { ...context, events: [wrapped] })).passed, true);
  assert.equal((await validateDelivery({ ...report, verification: ['ls'] }, { ...context, events: [wrapped] })).passed, false);
});
test('a verification that ran before the final edit does not count; a later successful run does', async t => {
  const context = await fixture(t), edit = { id: 'edit', type: 'files', status: 'completed', changes: [] };
  assert.equal((await validateDelivery(report, { ...context, events: [command, edit] })).passed, false);
  assert.equal((await validateDelivery(report, { ...context, events: [edit, command] })).passed, true);
  assert.equal((await validateDelivery(report, { ...context, events: [command, edit, { ...command, id: 'check-2' }] })).passed, true);
});

test('exact command receipt IDs survive Windows quoting but still require success after the last edit', async t => {
  const context = await fixture(t), edit = { id: 'edit', type: 'files', status: 'completed', changes: [] };
  const wrapped = { ...command, command: 'powershell.exe -Command "node -e \\\"console.log(1)\\\""' };
  const receiptReport = { ...report, verification: ['command:check-1'] };
  assert.equal((await validateDelivery(receiptReport, { ...context, events: [edit, wrapped] })).passed, true);
  for (const events of [[wrapped, edit], [{ ...wrapped, exitCode: 1 }], [{ ...wrapped, id: 'other' }]]) assert.equal((await validateDelivery(receiptReport, { ...context, events })).passed, false);
  assert.equal((await validateDelivery({ ...report, verification: ['command:check'] }, { ...context, events: [wrapped] })).passed, false);
});
test('completed work with remaining items, or with no verification, stays unverified', async t => {
  const context = await fixture(t);
  for (const change of [{ verification: [] }, { remaining: ['required test not run'] }]) assert.equal((await validateDelivery({ ...report, ...change }, context)).passed, false);
});
test('not applicable allows no artifacts and no verification, but not undeclared file changes', async t => {
  const context = await fixture(t);
  const analysis = { outcome: 'not_applicable', artifacts: [], verification: [], remaining: [] };
  assert.equal((await validateDelivery(analysis, { ...context, changedFiles: [] })).passed, true);
  assert.equal((await validateDelivery(analysis, context)).passed, false);
  assert.equal((await validateDelivery({ ...analysis, artifacts: ['result.txt'] }, context)).passed, false);
  assert.equal((await validateDelivery({ ...report, artifacts: [] }, context)).passed, false);
});
test('old reports without evidence do not acquire verified completion during recovery', async t => {
  assert.equal((await validateDelivery({ outcome: 'completed' }, await fixture(t))).passed, false);
  assert.equal((await validateDelivery({ outcome: 'completed', delivery_type: 'files', artifacts: [{ path: 'result.txt', sha256: sha('actual') }], verification: [{ command_id: 'check-1' }] }, await fixture(t))).passed, false);
});
test('execution receipts retain identity, cwd and terminal status, without command output', () => {
  assert.deepEqual(executionEvent({ id: 'c', type: 'commandExecution', command: 'test', cwd: 'x', status: 'failed', exitCode: 1, aggregatedOutput: 'private output' }), { id: 'c', type: 'command', command: 'test', cwd: 'x', status: 'failed', exitCode: 1 });
});
test('a report claim remains pending until evidence is checked, and contradictory evidence blocks it', async t => {
  const context = await fixture(t);
  const finished = finishReport({ status: 'completed' }, '## 我做了什么\n完成\n\n```droprun\n' + JSON.stringify(report) + '\n```');
  assert.equal(finished.status, 'running'); assert.equal(finished.deliveryPending, true);
  assert.equal((await validateDelivery(finished.reportData, { ...context, events: [{ ...command, exitCode: 1, status: 'failed' }] })).passed, false);
});
test('followup fingerprints distinguish unchanged earlier delivery from this turn changes', async t => {
  const { cwd } = await fixture(t);
  await writeFile(join(cwd, '__proto__'), 'before');
  const before = await changeFingerprint(cwd, ['result.txt', '__proto__']);
  assert.deepEqual(changedSince(before, await changeFingerprint(cwd, ['result.txt', '__proto__'])), []);
  await writeFile(join(cwd, '__proto__'), 'after');
  assert.deepEqual(changedSince(before, await changeFingerprint(cwd, ['result.txt', '__proto__'])), ['__proto__']);
});
