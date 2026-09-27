const outcomes = ['completed', 'not_applicable', 'blocked'];
const stringList = { type: 'array', items: { type: 'string' } };
// The evidence block is the only machine-read part of the report. Everything else is written for the user.
export const evidenceSchema = {
  type: 'object', additionalProperties: false,
  properties: { outcome: { type: 'string', enum: outcomes }, artifacts: stringList, verification: stringList, remaining: stringList },
  required: ['outcome', 'artifacts', 'verification', 'remaining']
};
export const reportSchema = evidenceSchema;

const fence = /```droprun[ \t]*\r?\n([\s\S]*?)\r?\n```[ \t]*(?:\r?\n|$)/g;

export function normalizeEvidence(value) {
  const data = typeof value === 'string' ? JSON.parse(value) : value;
  if (!data || Array.isArray(data) || typeof data !== 'object') throw new Error('证据不是对象');
  if (Object.keys(data).some(key => !evidenceSchema.required.includes(key))) throw new Error('证据包含未知字段');
  if (!outcomes.includes(data.outcome)) throw new Error('outcome 无效');
  const list = key => {
    const items = data[key] ?? [];
    if (!Array.isArray(items) || items.some(item => typeof item !== 'string')) throw new Error(key + ' 必须是字符串数组');
    return [...new Set(items.map(item => item.trim()).filter(Boolean))];
  };
  return { outcome: data.outcome, artifacts: list('artifacts'), verification: list('verification'), remaining: list('remaining') };
}

// Only a first-line `# ` heading names the task; `##` headings are the report's own sections.
const heading = /^\s*#[ \t]+([^\r\n]*)(?:\r?\n|$)/;

/** Splits the model's final message into the optional title line, the human report and the trailing evidence block. */
export function parseReport(raw) {
  const text = typeof raw === 'string' ? raw : '';
  let evidence = null, error = null, markdown = text, title = null;
  const blocks = [...text.matchAll(fence)];
  if (blocks.length) {
    const last = blocks.at(-1);
    try { evidence = normalizeEvidence(last[1]); } catch (cause) { error = cause.message; }
    markdown = text.slice(0, last.index) + text.slice(last.index + last[0].length);
  }
  const named = markdown.match(heading);
  if (named) {
    title = named[1].replace(/[ \t#]+$/, '').trim().slice(0, 40).trimEnd() || null;
    if (title) markdown = markdown.slice(named[0].length).replace(/^\s+/, '');
  }
  return { markdown: markdown.replace(/\s+$/, ''), evidence, error, title };
}

/** Strips the shell wrapper Codex adds so the phone shows the command the user would recognize. */
export function displayCommand(command) {
  let text = String(command ?? '').replace(/\s+/g, ' ').trim();
  const wrapped = text.match(/^"?(?:[A-Za-z]:\\[^"]*\\)?(?:powershell|pwsh)(?:\.exe)?"?\s+(?:-\w+\s+)*-Command\s+(['"])([\s\S]*)\1$/i) || text.match(/^(?:\/bin\/)?(?:bash|sh|zsh)\s+-l?c\s+(['"])([\s\S]*)\1$/i);
  if (wrapped) text = wrapped[2];
  return text;
}

function commandSection(events) {
  const commands = events.filter(event => event.type === 'command').slice(-8);
  const files = [...new Set(events.filter(event => event.type === 'files').flatMap(event => (event.changes || []).map(change => change.path)))];
  if (!commands.length && !files.length) return '';
  const lines = [...files.map(path => '- 改动：' + path), ...commands.map(event => '- `' + displayCommand(event.command).replace(/`/g, "'").slice(0, 140) + '`' + (event.exitCode === 0 ? ' 通过' : event.exitCode == null ? ' 未完成' : ' 退出码 ' + event.exitCode))];
  return '\n\n## 执行记录\n' + lines.join('\n');
}

export function finishReport(turn, raw, events = [], evidence = null) {
  const parsed = parseReport(raw);
  const named = parsed.title ? { title: parsed.title } : {};
  if (turn.status !== 'completed') return {
    status: turn.status === 'interrupted' ? 'cancelled' : 'failed',
    report: (typeof raw === 'string' && raw.trim()) ? parsed.markdown : '这次执行没有完成。可以在电脑的 Codex 里查看会话。',
    error: turn.error?.message || null, ...named
  };
  const data = evidence || parsed.evidence;
  const markdown = parsed.markdown.trim() || '（Codex 没有留下文字说明。）';
  if (!data) return { status: 'running', report: markdown, needsEvidence: true, error: parsed.error, ...named };
  const blocked = data.outcome === 'blocked';
  const report = markdown + commandSection(events);
  return { status: blocked ? 'blocked' : 'running', deliveryPending: !blocked, report, error: blocked ? (data.remaining.join('；') || 'Codex 报告任务受阻。') : null, reportData: data, ...named };
}

export function cancellationReport(reason, events = []) {
  return { status: 'cancelled', deliveryPending: false, error: null, reportData: null, report: '## 已停止\n' + reason + '\n\n之前已经发生的操作不会自动回滚；如果需要，可以在电脑上核对。' + commandSection(events) };
}
