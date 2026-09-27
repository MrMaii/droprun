import { test } from 'node:test';
import assert from 'node:assert/strict';
import { finishReport, parseReport, normalizeEvidence, evidenceSchema, cancellationReport, displayCommand } from '../connector/report.mjs';
import { developerInstructions } from '../connector/project-task.mjs';

test('execution records show the command without its shell wrapper', () => {
  assert.equal(displayCommand('"C:\\WINDOWS\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command \'Get-Content README.md -TotalCount 1\''), 'Get-Content README.md -TotalCount 1');
  assert.equal(displayCommand('bash -lc "npm test -- --runInBand"'), 'npm test -- --runInBand');
  assert.equal(displayCommand('  npm   test '), 'npm test');
  const report = finishReport({ status: 'completed' }, '## 我做了什么\nx\n\n```droprun\n{"outcome":"completed","artifacts":[],"verification":["npm test"],"remaining":[]}\n```', [{ type: 'command', command: 'bash -lc "npm test"', exitCode: 0 }]);
  assert.match(report.report, /- `npm test` 通过/);
});

const evidence = { outcome: 'completed', artifacts: ['style.css'], verification: ['npm test'], remaining: [] };
const block = data => '\n\n```droprun\n' + JSON.stringify(data) + '\n```\n';
const human = '## 我看到了什么\n绿色界面参考。\n\n## 用在哪里\n首页配色。\n\n## 我做了什么\n改了 style.css。\n\n## 验证\nnpm test 通过。\n\n## 未完成\n无';

test('the evidence block is split off and the human report stays intact', () => {
  const parsed = parseReport(human + block(evidence));
  assert.deepEqual(parsed.evidence, evidence);
  assert.equal(parsed.markdown, human);
  assert.equal(parsed.error, null);
  assert.ok(!parsed.markdown.includes('droprun'));
});
test('only the last block counts and a malformed block is reported rather than guessed', () => {
  const parsed = parseReport(human + block({ outcome: 'blocked', artifacts: [], verification: [], remaining: ['x'] }) + '补充说明' + block(evidence));
  assert.equal(parsed.evidence.outcome, 'completed');
  const broken = parseReport(human + '```droprun\n{"outcome":"done"}\n```');
  assert.equal(broken.evidence, null); assert.match(broken.error, /outcome/);
  assert.throws(() => normalizeEvidence({ ...evidence, extra: 1 }));
  assert.throws(() => normalizeEvidence({ ...evidence, artifacts: 'style.css' }));
  assert.deepEqual(normalizeEvidence({ outcome: 'completed', artifacts: [' a.txt ', 'a.txt'], verification: ['npm test'], remaining: [] }).artifacts, ['a.txt']);
  assert.equal(evidenceSchema.additionalProperties, false);
});
test('a completed turn with evidence is pending delivery; a blocked outcome stays blocked with its reasons', () => {
  const result = finishReport({ status: 'completed' }, human + block(evidence), [{ type: 'command', command: 'npm test', exitCode: 0 }]);
  assert.equal(result.status, 'running'); assert.equal(result.deliveryPending, true);
  assert.match(result.report, /## 执行记录/); assert.match(result.report, /npm test.*通过/);
  assert.ok(result.report.startsWith('## 我看到了什么'));
  const blocked = finishReport({ status: 'completed' }, human + block({ ...evidence, outcome: 'blocked', remaining: ['未取得视频'] }));
  assert.equal(blocked.status, 'blocked'); assert.equal(blocked.error, '未取得视频'); assert.equal(blocked.deliveryPending, false);
});
test('a report without an evidence block asks for one instead of completing', () => {
  const result = finishReport({ status: 'completed' }, human);
  assert.equal(result.needsEvidence, true); assert.equal(result.status, 'running'); assert.equal(result.report, human);
  const supplied = finishReport({ status: 'completed' }, human, [], evidence);
  assert.equal(supplied.deliveryPending, true); assert.equal(supplied.needsEvidence, undefined);
  assert.equal(finishReport({ status: 'completed' }, '').report, '（Codex 没有留下文字说明。）');
});
test('interruption or a failed protocol turn overrides any completion claim', () => {
  assert.equal(finishReport({ status: 'interrupted' }, human + block(evidence)).status, 'cancelled');
  const failed = finishReport({ status: 'failed', error: { message: 'Tool failed' } }, human + block(evidence));
  assert.equal(failed.status, 'failed'); assert.equal(failed.error, 'Tool failed'); assert.equal(failed.report, human);
});
test('cancellation reports say what stopped and never claim a rollback', () => {
  const result = cancellationReport('项目授权已停用。', [{ type: 'command', command: 'test', exitCode: 0 }]);
  assert.equal(result.status, 'cancelled'); assert.match(result.report, /不会自动回滚/); assert.match(result.report, /项目授权已停用/); assert.match(result.report, /test.*通过/);
  assert.equal('title' in result, false);
});

const titled = '# 首页配色调整\n\n' + human;

test('a leading H1 names the task and is stripped from the report', () => {
  const parsed = parseReport(titled + block(evidence));
  assert.equal(parsed.title, '首页配色调整'); assert.equal(parsed.markdown, human); assert.deepEqual(parsed.evidence, evidence);
  assert.deepEqual(parseReport('# 只有标题'), { markdown: '', evidence: null, error: null, title: '只有标题' });
  assert.equal(parseReport('\n#\t标题带尾巴  #\r\n\r\n' + human).title, '标题带尾巴');
  assert.equal(parseReport('# ' + '长'.repeat(50) + '\n' + human).title, '长'.repeat(40));
  const finished = finishReport({ status: 'completed' }, titled + block(evidence), [{ type: 'command', command: 'npm test', exitCode: 0 }]);
  assert.equal(finished.title, '首页配色调整'); assert.ok(finished.report.startsWith('## 我看到了什么')); assert.equal(finished.deliveryPending, true);
  assert.equal(finishReport({ status: 'completed' }, titled).title, '首页配色调整');
  assert.equal(finishReport({ status: 'failed', error: { message: 'x' } }, titled).title, '首页配色调整');
  assert.equal(finishReport({ status: 'interrupted' }, titled).report, human);
});
test('reports without a first-line H1 stay unnamed and untouched', () => {
  for (const raw of [human, '## 标题不是名字\n' + human, human + '\n# 后面的标题', '#\n' + human, '']) {
    const parsed = parseReport(raw);
    assert.equal(parsed.title, null, raw); assert.equal(parsed.markdown, raw.replace(/\s+$/, ''));
  }
  assert.equal('title' in finishReport({ status: 'completed' }, human + block(evidence)), false);
  assert.equal('title' in finishReport({ status: 'completed' }, human), false);
  assert.equal('title' in finishReport({ status: 'failed' }, human), false);
});
test('both instruction variants ask for the title line the parser extracts, ahead of the sections', () => {
  const task = { id: 'task', project_name: 'Fixture' };
  for (const planning of [true, false]) {
    const text = developerInstructions(task, planning);
    assert.match(text, /第一行只写标题 # <标题>：不超过 12 个汉字/, String(planning));
    assert.ok(text.indexOf('# <标题>') < text.indexOf('## 我看到了什么'), String(planning));
    const example = text.match(/例如 (# [^（\s]+)/)[1];
    assert.equal(parseReport(example + '\n## 我看到了什么\n材料').title, example.slice(2));
  }
});
