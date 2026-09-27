import { test } from 'node:test';
import assert from 'node:assert/strict';
import { TaskApprovals } from '../connector/approvals.mjs';
const message = { id:7,method:'item/commandExecution/requestApproval',params:{ kind:'command',threadId:'thread',turnId:'turn',itemId:'item',command:'Write-Output test',cwd:'C:/isolated',reason:'test',availableDecisions:['accept','decline'] } };
test('approval waits, then sends a single operation response; never session or policy approval', async () => {
  let status='pending'; const sent=[],calls=[];
  const approvals=new TaskApprovals('task',async (path,body)=>{ calls.push({path,body}); return {status,expires_at:100000}; },r=>sent.push(r),()=>1);
  assert.equal(approvals.receive(message),true); approvals.receive(message);
  await approvals.poll(); assert.equal(sent.length,0); assert.equal(approvals.pending.size,1);
  status='approved'; await approvals.poll(); await approvals.poll();
  assert.deepEqual(sent,[{id:7,result:{decision:'accept'}}]);
  approvals.receive(message); await approvals.poll(); assert.equal(sent.length,1);
  assert.equal(calls.filter(c=>c.body).length,1); assert.equal(approvals.waiting,false);
});
test('denied, expired, invalidated and resolved requests cannot execute', async () => {
  for(const status of ['denied','expired','invalidated']) {
    const sent=[]; const a=new TaskApprovals('task',async()=>({status,expires_at:100000}),r=>sent.push(r),()=>1);
    a.receive(message); await a.poll(); assert.equal(sent[0].result.decision,'decline');
  }
  let now=0; const sent=[];
  const a=new TaskApprovals('task',async()=>{throw new Error('offline');},r=>sent.push(r),()=>now);
  a.receive(message); await assert.rejects(a.poll(),/offline/); assert.equal(a.waiting,true);
  now=16*60*1000; await a.poll(); assert.equal(sent[0].result.decision,'decline');
  const resolved=new TaskApprovals('task',async()=>({status:'approved',expires_at:100000}),r=>sent.push(r),()=>1);
  resolved.receive(message); resolved.resolved(7); await resolved.poll(); assert.equal(sent.length,1);
});
test('unsupported approval kinds, missing full command, and session-only choices are not offered', () => {
  const a=new TaskApprovals('task',()=>{},()=>{});
  for(const patch of [{command:''},{cwd:null},{kind:'stdin'},{networkApprovalContext:{host:'example.com'}},{availableDecisions:['acceptForSession','decline']},{command:'x'.repeat(40001)}]) assert.equal(a.receive({...message,params:{...message.params,...patch}}),false);
  assert.equal(a.receive({...message,method:'item/fileChange/requestApproval'}),false);
});
test('desktop accept/cancel choices use cancel for refusal, never a policy amendment', async () => {
  for (const status of ['approved','denied','expired','invalidated']) {
    const sent=[],a=new TaskApprovals('task',async()=>({status,expires_at:100000}),r=>sent.push(r),()=>1);
    assert.equal(a.receive({...message,params:{...message.params,availableDecisions:['accept',{acceptWithExecpolicyAmendment:{execpolicy_amendment:['powershell.exe']}},'cancel']}}),true);
    await a.poll(); assert.deepEqual(sent,[{id:7,result:{decision:status==='approved'?'accept':'cancel'}}]);
  }
});
