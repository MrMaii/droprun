import { randomUUID } from 'node:crypto';

export class TaskApprovals {
  constructor(taskId, api, send, now = Date.now) { this.taskId=taskId; this.api=api; this.send=send; this.now=now; this.pending=new Map(); this.seen=new Set(); }
  receive(message) {
    const p=message.params;
    if (message.method !== 'item/commandExecution/requestApproval' || (p.kind && p.kind !== 'command') || !p.command?.trim() || !p.cwd || p.networkApprovalContext || (p.availableDecisions && (!p.availableDecisions.includes('accept') || !p.availableDecisions.some(d=>d==='decline'||d==='cancel')))) return false;
    if (this.seen.has(message.id)) return true;
    const details={ kind:'command', command:p.command, cwd:p.cwd, reason:p.reason || '', additionalPermissions:p.additionalPermissions || null };
    if (JSON.stringify(details).length>40000) return false; // Never approve an operation whose full details cannot be displayed.
    this.seen.add(message.id);
    this.pending.set(message.id,{ id:randomUUID(),taskId:this.taskId,threadId:p.threadId,turnId:p.turnId,itemId:p.itemId,details,deadline:this.now()+15*60*1000,refusal:p.availableDecisions && !p.availableDecisions.includes('decline')?'cancel':'decline' });
    return true;
  }
  async poll() {
    for (const [requestId, request] of this.pending) {
      let decision;
      if (this.now()>=request.deadline) decision=request.refusal;
      else {
        if (!request.registered) {
          const { deadline, registered, refusal, ...body }=request;
          const row=await this.api('/connector/approvals',body);
          request.deadline=Math.min(request.deadline,row.expires_at); request.registered=true;
        }
        const row=await this.api('/connector/approvals/'+request.id);
        if (row.status==='pending') continue;
        decision=row.status==='approved' && this.now()<request.deadline ? 'accept' : request.refusal;
      }
      if (this.pending.get(requestId)!==request) continue;
      this.pending.delete(requestId); // A response is never replayed after a lost network acknowledgement or process restart.
      if (decision==='cancel') this.cancelled=true;
      this.send({ id:requestId,result:{ decision } });
    }
  }
  resolved(requestId) { this.pending.delete(requestId); }
  get waiting() { return this.pending.size>0; }
}
