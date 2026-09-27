export async function requireTaskPermission(task, api) {
  const permission=await api('/connector/tasks/'+task.id+'/permission');
  const original = ['review','direct'].includes(task.execution_mode);
  if (!task.permission_version || !permission.enabled || permission.taskId!==task.id || permission.projectId!==task.project_id || permission.version!==task.permission_version || permission.profile!==(original?'original-project':'isolated-workspace') || (original && permission.executionMode!==task.execution_mode)) throw new Error('此任务的项目授权已失效。请在手机核对项目授权后重新交办。');
  return {version:permission.version,profile:permission.profile, ...(original ? {executionMode:permission.executionMode,planDecision:permission.planDecision,planVersion:permission.planVersion} : {})};
}
