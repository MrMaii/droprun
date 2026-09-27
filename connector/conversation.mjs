export async function openConversation(codex, task, project, cwd, { planning = false, resumeThreadId = task.parent_task_id ? task.thread_id : null, developerInstructions = null, model = null } = {}) {
  const options = { cwd, runtimeWorkspaceRoots: [cwd], approvalPolicy: planning ? 'never' : 'on-request', approvalsReviewer: 'user', sandbox: planning ? 'read-only' : 'workspace-write', config: { 'sandbox_workspace_write.network_access': !planning }, ...(developerInstructions ? { developerInstructions } : {}), ...(model ? { model } : {}) };
  if (!task.parent_task_id && !resumeThreadId) return (await codex.call('thread/start', { ...options, projectId: project.id, serviceName: 'droprun' })).thread;
  if (!resumeThreadId) throw new Error('追问缺少原 Codex 会话，未创建替代会话。');
  const { thread: original } = await codex.call('thread/read', { threadId: resumeThreadId, includeTurns: false });
  if (original.projectId !== project.id) throw new Error('原会话项目归属不一致，追问未投递。');
  if (original.status?.type === 'active') throw new Error('原 Codex 会话正在执行其他任务，请结束后再追问。');
  const { thread } = await codex.call('thread/resume', { ...options, threadId: resumeThreadId, excludeTurns: true });
  if (thread.id !== resumeThreadId || thread.status?.type === 'active') throw new Error('原会话当前不能开始新一轮任务。');
  return thread;
}

export function recoveryTurn(thread, state, task) {
  const id = state.turnId || task.turn_id;
  if (!id) throw new Error('上次执行没有保存回合编号，无法确认本轮是否开始。请在电脑核对；不会重跑或把旧报告当作本轮结果。');
  const turn = thread.turns?.find(turn => turn.id === id);
  if (!turn) throw new Error('找不到本轮执行记录。已保留原会话，不会自动重复执行。');
  return turn;
}
