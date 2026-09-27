import { test } from 'node:test';
import assert from 'node:assert/strict';
import { requireTaskPermission } from '../connector/permissions.mjs';
const task={id:'task',project_id:'project',permission_version:'version'};
const permission={taskId:'task',projectId:'project',enabled:true,version:'version',profile:'isolated-workspace'};
test('executor requires fresh authorization for this exact task, project and permission generation',async()=>{
  assert.deepEqual(await requireTaskPermission(task,async route=>{assert.equal(route,'/connector/tasks/task/permission');return permission;}),{version:'version',profile:'isolated-workspace'});
  for(const patch of [{enabled:false},{taskId:'other'},{projectId:'other'},{version:'new'},{profile:'unrestricted'}])await assert.rejects(requireTaskPermission(task,async()=>({...permission,...patch})),/授权已失效/);
  await assert.rejects(requireTaskPermission({...task,permission_version:null},async()=>permission),/授权已失效/);
  await assert.rejects(requireTaskPermission(task,async()=>{throw new Error('offline');}),/offline/);
});
