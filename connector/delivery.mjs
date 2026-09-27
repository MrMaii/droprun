import { lstat, open, realpath } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { isAbsolute, relative, resolve, sep } from 'node:path';

export function executionEvent(item) {
  if (item.type === 'commandExecution') return { id: item.id, type: 'command', command: item.command, cwd: item.cwd, status: item.status, exitCode: item.exitCode ?? null };
  if (item.type === 'fileChange') return { id: item.id, type: 'files', status: item.status, changes: item.changes };
  return null;
}

export async function artifactInfo(cwd, path, includeContent = false) {
  if (typeof path !== 'string' || !path || isAbsolute(path) || /[:\0]/.test(path)) throw new Error('产物必须使用工作目录内相对路径');
  const parts = path.split(/[\/]/);
  if (parts.some(part => !part || part === '.' || part === '..' || part.toLowerCase() === '.git')) throw new Error('产物路径不安全');
  const root = await realpath(cwd);
  let full = root;
  for (const part of parts) {
    full = resolve(full, part);
    let stat;
    try { stat = await lstat(full); } catch (error) { if (error.code === 'ENOENT') return null; throw error; }
    if (stat.isSymbolicLink()) throw new Error('产物路径包含链接');
  }
  const actual = await realpath(full), rel = relative(root, actual);
  if (isAbsolute(rel) || rel === '..' || rel.startsWith('..' + sep)) throw new Error('产物路径越界');
  const file = await open(actual, 'r');
  try {
    const stat = await file.stat();
    if (!stat.isFile() || stat.size > 100 * 1024 * 1024) throw new Error('产物不是普通文件或超过 100 MiB');
    if (includeContent && stat.size > 50 * 1024 * 1024) throw new Error('手机交付单文件最多50MiB');
    const digest = createHash('sha256');
    const chunks = [];
    for await (const chunk of file.createReadStream({ autoClose: false })) { digest.update(chunk); if (includeContent) chunks.push(chunk); }
    const after = await file.stat();
    if (after.size !== stat.size || after.mtimeMs !== stat.mtimeMs) throw new Error('核对期间产物发生变化');
    return { path, bytes: stat.size, sha256: digest.digest('hex'), mtimeMs: stat.mtimeMs, ...(includeContent ? { content: Buffer.concat(chunks) } : {}) };
  } finally { await file.close(); }
}

// Compare each turn with its own starting state, not all preceding followup changes.
export async function changeFingerprint(cwd, paths) {
  const result = Object.create(null);
  for (const path of paths) result[path] = (await artifactInfo(cwd, path))?.sha256 ?? null;
  return result;
}
export function changedSince(before, after) {
  return [...new Set([...Object.keys(before), ...Object.keys(after)])].filter(path => before[path] !== after[path]);
}

const normalizePath = value => value.replaceAll('\\', '/').replace(/^\.\//, '');
const normalizeCommand = value => String(value ?? '').replace(/\s+/g, ' ').trim();
const successful = event => event.type === 'command' && event.status === 'completed' && event.exitCode === 0;
// A claimed verification counts when the model quotes a command that really ran and succeeded.
function matchesClaim(event, claim) {
  const recorded = normalizeCommand(event.command), wanted = normalizeCommand(claim);
  if (wanted.startsWith('command:')) return wanted.slice(8) === event.id;
  if (!wanted || wanted.length < 2) return false;
  return recorded === wanted || (wanted.length >= 4 && recorded.includes(wanted));
}

/**
 * Evidence v3: artifacts are paths that must exist and have been produced this turn; verification entries are
 * commands that must match successful recorded executions after the final edit. Nothing here proves business
 * correctness; it only stops a report from claiming work that the executor never saw.
 */
export async function validateDelivery(data, { cwd, events = [], changedFiles = [], startedAt = null }) {
  const errors = [], artifacts = [];
  if (!data || !['completed', 'not_applicable', 'blocked'].includes(data.outcome) || !Array.isArray(data.artifacts) || !Array.isArray(data.verification) || data.artifacts.some(item => typeof item !== 'string') || data.verification.some(item => typeof item !== 'string')) return { passed: false, errors: ['报告缺少可核对的证据块'], artifacts, verification: [] };
  const changed = new Set((changedFiles || []).map(normalizePath));
  const lastEdit = events.findLastIndex(event => event.type === 'files' && event.status === 'completed');
  if (data.outcome === 'completed' && data.remaining?.length) errors.push('仍有未完成事项，Codex 应把 outcome 标为 blocked');
  if (data.outcome === 'completed' && !data.verification.length) errors.push('没有列出任何验证命令');
  if (data.outcome === 'not_applicable' && data.artifacts.length) errors.push('判断为不适用却声明了产物');
  const verification = [];
  for (const claim of data.verification) {
    const matches = events.map((event, index) => ({ event, index })).filter(({ event }) => successful(event) && matchesClaim(event, claim));
    if (!matches.length) { errors.push('验证命令没有成功执行记录：' + claim); verification.push({ command: claim, event: null }); continue; }
    const afterEdit = matches.filter(({ index }) => index > lastEdit);
    if (!afterEdit.length) { errors.push('验证之后仍发生文件修改，需要重新验证：' + claim); verification.push({ command: claim, event: matches.at(-1).event }); continue; }
    verification.push({ command: claim, event: afterEdit.at(-1).event });
  }
  for (const declared of data.artifacts) {
    try {
      const actual = await artifactInfo(cwd, declared);
      if (!actual) throw new Error('产物不存在');
      const producedNow = changed.has(normalizePath(declared)) || (startedAt != null && actual.mtimeMs >= startedAt - 2000);
      if (!producedNow) throw new Error('文件没有在本次执行中产生或修改');
      artifacts.push({ path: declared, bytes: actual.bytes, sha256: actual.sha256 });
    } catch (error) { errors.push(declared + '：' + error.message); }
  }
  if (data.outcome !== 'blocked' && !data.artifacts.length && changed.size) errors.push('执行记录显示有文件变更，但报告没有声明产物：' + [...changed].slice(0, 5).join('、'));
  return { passed: errors.length === 0, checkedAt: new Date().toISOString(), errors, artifacts, verification };
}
