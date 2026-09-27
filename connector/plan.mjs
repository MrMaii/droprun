import { parseReport } from './report.mjs';

/** The plan is the model's own markdown; the phone renders it and asks the user to approve or reject. */
export function renderPlan(raw) {
  const { markdown } = parseReport(raw);
  const text = markdown.trim();
  if (!text) throw new Error('Codex 没有给出理解与计划，未开始执行。请补充留言后重新转发。');
  if (text.length > 100000) throw new Error('理解与计划过长，未开始执行；请缩小范围后重试。');
  return text;
}

export function projectTurnPolicy(cwd, planning) {
  return planning ? { type: 'readOnly', networkAccess: false }
    : { type: 'workspaceWrite', writableRoots: [cwd], networkAccess: true, excludeTmpdirEnvVar: true, excludeSlashTmp: true };
}

export function assertExecutionApproved(task, permission) {
  if (task.execution_mode === 'direct' && permission.executionMode === 'direct') return;
  if (task.execution_mode !== 'review' || permission.executionMode !== 'review' || task.plan_decision !== 'approved' || permission.planDecision !== 'approved' || !task.thread_id || !task.plan_turn_id || !task.plan_report || !task.plan_version || permission.planVersion !== task.plan_version) throw new Error('当前计划尚未获手机批准，未启动项目修改。');
}
