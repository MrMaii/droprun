import { readFile, stat } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { join } from 'node:path';
import { artifactInfo } from './delivery.mjs';
import { targetRoute } from './routes.mjs';

const text = { type: 'string' };
const texts = { type: 'array', items: text };
export const visualIntentSchema = {
  type: 'object', additionalProperties: false,
  properties: {
    outcome: { type: 'string', enum: ['ready', 'blocked', 'not_applicable'] },
    summary: text, effect: text, motion: { type: 'boolean' }, referenceRequired: { type: 'boolean' },
    targets: { type: 'array', maxItems: 4, items: { type: 'object', additionalProperties: false,
      properties: { route: text, purpose: text, files: texts, expected: text }, required: ['route', 'purpose', 'files', 'expected'] } },
    limitations: texts
  }, required: ['outcome', 'summary', 'effect', 'motion', 'referenceRequired', 'targets', 'limitations']
};

export function needsVisualInspection(task, material = null) {
  return /页面|网页|界面|前端|动效|动画|配色|视差|网站|效果|第.{0,12}页|滚动|交互|\b(?:ui|frontend|front-end|webpage|website|hover|scroll|parallax|animation|page|effect)\b/i.test(task.message || '') || (!task.message?.trim() && !!material?.visualEvidence?.videos?.length);
}

export class VisualIntentError extends Error {
  constructor(detail) { super('DropRun 无法校验 Codex 的页面定位结果：' + detail + '。尚未修改项目；这是内部定位问题，不要求你提供代码文件或技术参数。'); }
}

export function parseVisualIntent(raw) {
  let data;
  try { data = JSON.parse(raw); } catch { throw new VisualIntentError('结果不是有效 JSON'); }
  if (!data || !['ready', 'blocked', 'not_applicable'].includes(data.outcome) || typeof data.summary !== 'string' || typeof data.motion !== 'boolean' || typeof data.referenceRequired !== 'boolean' || typeof data.effect !== 'string' || !Array.isArray(data.targets) || data.targets.length > 4 || !Array.isArray(data.limitations) || data.limitations.some(value => typeof value !== 'string')) throw new VisualIntentError('结果字段不完整');
  // A blocked inspection can contain incomplete candidates; preserve its actual explanation.
  if (data.outcome !== 'ready') return data;
  if (!data.targets.length) throw new VisualIntentError('Codex 声明可以执行，但没有列出目标页面');
  for (const [index, target] of data.targets.entries()) {
    try { target.route = targetRoute(target?.route); } catch (error) { throw new VisualIntentError('第 ' + (index + 1) + ' 个目标的地址无效：' + error.message); }
    for (const field of ['purpose', 'expected']) if (typeof target[field] !== 'string' || !target[field].trim()) throw new VisualIntentError('第 ' + (index + 1) + ' 个目标缺少 ' + (field === 'purpose' ? '页面用途' : '效果验收说明'));
    if (!Array.isArray(target.files) || !target.files.length || target.files.some(value => typeof value !== 'string' || !value.trim())) throw new VisualIntentError('第 ' + (index + 1) + ' 个目标没有列出可修改的文件');
  }
  return data;
}

export async function captureTargetFiles(cwd, intent) {
  const files = {};
  for (const path of new Set(intent.targets.flatMap(target => target.files))) {
    const info = await artifactInfo(cwd, path);
    files[path] = info?.sha256 ?? null;
  }
  return files;
}

export async function assertTargetFilesUnchanged(cwd, baseline) {
  for (const [path, hash] of Object.entries(baseline)) if (((await artifactInfo(cwd, path))?.sha256 ?? null) !== hash) throw new Error('准备执行时目标文件已被其他操作修改，请核对后继续：' + path);
}

export async function validateVisualDelivery(intent, manifest, preview, { cwd, artifacts = [], events = [] } = {}) {
  const errors = [];
  if (!intent || intent.outcome !== 'ready') return { passed: false, errors: ['缺少已核对的目标页面与验收要求'] };
  if (!preview || !preview.url || preview.expiresAt <= Date.now()) errors.push('预览不可用或已过期');
  const checks = manifest?.checks || [];
  for (const target of intent.targets) {
    const check = checks.findLast(value => value.expectedPath === target.route && value.passed && value.previewVersion === (preview?.revision || preview?.version));
    if (!check) { errors.push('缺少目标页面的成功浏览器验证：' + target.route); continue; }
    if (intent.motion && !check.steps?.some(step => ['hover', 'click', 'scroll', 'wait'].includes(step.action))) errors.push('动效未经过实际触发或时序检查：' + target.route);
    if (!check.steps?.some(step => step.action === 'assert' && (step.text?.trim() || Object.keys(step.computedStyle || {}).length))) errors.push('缺少页面效果断言：' + target.route);
    const shots = (manifest.screenshots || []).filter(shot => shot.checkId === check.id);
    if (shots.length < (intent.motion ? 2 : 1)) errors.push('缺少目标页面的截图证据：' + target.route);
    for (const shot of shots) {
      try {
        const bytes = await readFile(shot.path);
        if (createHash('sha256').update(bytes).digest('hex') !== shot.sha256) errors.push('截图已改变：' + shot.name);
      } catch { errors.push('截图文件不可读：' + shot.name); }
    }
    for (const artifact of artifacts) {
      try { if ((await stat(join(cwd, artifact.path))).mtimeMs > check.startedAt) errors.push('浏览器验证期间或之后仍有文件修改，请重新验证：' + artifact.path); } catch { /* Normal artifact validation reports unreadable paths. */ }
    }
    if (check.eventCount < events.length && events.slice(check.eventCount).some(event => event.type === 'files' && event.status === 'completed')) errors.push('页面截图后仍发生文件修改，请重新验证');
  }
  return { passed: !errors.length, errors: [...new Set(errors)], checkedAt: Date.now() };
}
