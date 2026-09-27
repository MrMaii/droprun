import { health, identity, projectActivity, projectHistory, presentTasks, retention, cleanupRetention, deleteSnapshot, validPreviewUrl } from './selfhost.mjs';
import { snapshotRoute, issuePreview } from './snapshots.mjs';
const json = (data, status = 200) => Response.json(data, { status, headers: { 'Cache-Control': 'no-store' } });
const hash = async text => [...new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text)))].map(b => b.toString(16).padStart(2, '0')).join('');
const uuid = value => typeof value === 'string' && /^[a-zA-Z0-9-]{20,64}$/.test(value);
const terminal = new Set(['completed', 'blocked', 'failed', 'cancelled']);
const allowedStatus = new Set(['queued', 'reading', 'planning', 'running', 'waiting_for_approval', ...terminal]);
const validPlanVersion = value => typeof value === 'string' && !!value.trim() && value.length <= 200;
const validModelId = value => typeof value === 'string' && /^[a-zA-Z0-9._:-]{1,64}$/.test(value);
const validEffort = value => typeof value === 'string' && /^[a-z]{1,16}$/.test(value);
// A model choice must exist in the catalog the computer last announced; the effort must be one that model supports.
function resolveModelChoice(state, model, effort) {
  if (model == null && effort == null) return { model: null, effort: null };
  if (model != null && !validModelId(model)) throw new Error('模型无效');
  if (effort != null && !validEffort(effort)) throw new Error('推理强度无效');
  const catalog = state?.models || [];
  if (catalog.length) {
    const chosen = model ? catalog.find(m => m.id === model) : (catalog.find(m => m.isDefault) || catalog[0]);
    if (!chosen) throw new Error('模型不可用，请刷新后重新选择');
    if (effort && chosen.efforts.length && !chosen.efforts.includes(effort)) throw new Error('该模型不支持所选推理强度');
  }
  return { model: model || null, effort: effort || null };
}
const validPermission = "EXISTS (SELECT 1 FROM project_permissions p JOIN devices d ON d.id=p.device_id WHERE p.device_id=tasks.device_id AND p.project_id=tasks.project_id AND p.enabled=1 AND p.version=tasks.permission_version)";
async function readBody(request, max = 200000) {
  const text = await request.text();
  if (text.length > max) throw new Error('Request too large');
  return JSON.parse(text);
}
export default {
  async scheduled(_controller, env) { await cleanupRetention(env); },
  async fetch(request, env) {
    try { return await route(request, env); }
    catch (e) { return json({ error: e.message || 'Request failed' }, 400); }
  }
};
async function route(request, env) {
  const path = new URL(request.url).pathname;
  const method = request.method;
  const db = env.DB;
  if (path === '/health') return health(request, env);
  if (path === '/pair' && method === 'GET') return new Response(pairPage(), { headers: { 'Content-Type': 'text/html;charset=utf-8', 'Cache-Control': 'no-store' } });
  if (path === '/.well-known/assetlinks.json') return Response.json(env.ANDROID_CERT_SHA256 ? [{ relation: ['delegate_permission/common.handle_all_urls'], target: { namespace: 'android_app', package_name: 'app.droprun.mobile', sha256_cert_fingerprints: [env.ANDROID_CERT_SHA256] } }] : [], { headers: { 'Cache-Control': 'public,max-age=3600' } });
  if (path === '/download.apk') {
    if (env.INSTANCE_ID) return Response.redirect('https://github.com/MrMaii/droprun/releases', 302);
    const object = await env.FILES.get('releases/droprun.apk');
    return object ? new Response(object.body, { headers: { 'Content-Type': 'application/vnd.android.package-archive', 'Content-Disposition': 'attachment; filename="DropRun.apk"', 'Cache-Control': 'no-store' } }) : json({ error: 'Build not published yet' }, 404);
  }
  if (path === '/brand/droprun-v5.png' && method === 'GET') {
    const object = await env.FILES.get('brand/droprun-v5.png');
    return object ? new Response(object.body, { headers: { 'Content-Type': 'image/png', 'Cache-Control': 'public,max-age=3600', 'X-Content-Type-Options': 'nosniff', ETag: object.httpEtag } }) : json({ error: 'Brand not published yet' }, 404);
  }
  if (path === '/') return new Response(publicPage(), { headers: { 'Content-Type': 'text/html;charset=utf-8', 'Cache-Control': 'no-cache' } });
  if (path === '/pair' && method === 'POST') {
    const body = await readBody(request, 1000);
    if (env.INSTANCE_ID && body.instanceId !== env.INSTANCE_ID) return json({ error: 'Relay instance does not match. Scan the pairing code again.' }, 409);
    const code = typeof body.code === 'string' ? body.code.trim().toUpperCase().replace(/[\s-]/g, '') : '';
    const invalid = () => json({ error: '配对码无效、已使用或已过期，请在电脑重新生成' }, 403);
    if (!/^[0-9A-F]{20}$/.test(code)) return invalid();
    const token = crypto.randomUUID() + crypto.randomUUID();
    const id = crypto.randomUUID(), now = Date.now(), codeHash = await hash(code);
    const result = await db.batch([
      db.prepare('UPDATE pairing_codes SET consumed_at=?,device_id=? WHERE id=1 AND code_hash=? AND consumed_at IS NULL AND expires_at>?').bind(now,id,codeHash,now),
      // New phones start in direct mode: share, submit, then follow along in Codex. Plan review stays available in settings.
      db.prepare('INSERT INTO devices (id,token_hash,created_at,direct_execution) SELECT device_id,?,?,1 FROM pairing_codes WHERE id=1 AND code_hash=? AND device_id=? AND consumed_at=?').bind(await hash(token),now,codeHash,id,now)
    ]);
    if (result[1].meta.changes !== 1) return invalid();
    return json({ token, deviceId: id, ...identity(request, env) });
  }
  const bearer = request.headers.get('Authorization')?.replace(/^Bearer /, '') || '';
  const admin = bearer.length > 20 && !!env.ADMIN_HASH && env.ADMIN_HASH !== env.CONNECTOR_HASH && await hash(bearer) === env.ADMIN_HASH;
  const connector = bearer.length > 20 && await hash(bearer) === env.CONNECTOR_HASH;
  const device = !connector && !admin && bearer ? await db.prepare('SELECT id FROM devices WHERE token_hash = ?').bind(await hash(bearer)).first() : null;
  if (!connector && !device && !admin) return json({ error: '请先配对电脑' }, 401);
  const privileged = path.startsWith('/admin/') || ['/connector/migrate','/connector/release','/connector/brand-logo'].includes(path);
  if (privileged && !admin) return json({ error: 'Installation administrator credential required' }, 403);
  if (path.startsWith('/connector/') && !privileged && !connector) return json({ error: 'Connector only' }, 403);
  if (admin && !privileged) return json({ error: 'Administrator credential cannot execute runtime requests' }, 403);
  if (path === '/admin/retention' && method === 'POST') return json(await cleanupRetention(env));
  if (path === '/device/retention' && method === 'GET' && device) return json(retention(env));
  if (connector && /^\/connector\/tasks\/[^/]+\/snapshots/.test(path)) return snapshotRoute(request,env);
  if (path === '/connector/brand-logo' && method === 'PUT') {
    const size = Number(request.headers.get('Content-Length'));
    if (!size || size > 2 * 1024 * 1024) return json({ error: 'PNG must be at most 2 MiB' }, 400);
    const bytes = new Uint8Array(await request.arrayBuffer());
    const sha256 = [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))].map(b => b.toString(16).padStart(2, '0')).join('');
    if (bytes.length !== size || sha256 !== '5b53d5fba0abb6693006409f347e95e1dc1c82b557b027eb6b556c2adf7e72a1') return json({ error: 'Expected approved v5 logo bytes' }, 400);
    await env.FILES.put('brand/droprun-v5.png', bytes, { httpMetadata: { contentType: 'image/png' } });
    return json({ ok: true, sha256 });
  }
  if (path === '/device/settings' && device) {
    if (method === 'POST') {
      const b=await readBody(request,1000);
      if(typeof b.directExecution!=='boolean')return json({error:'请选择执行模式'},400);
      if(b.directExecution && b.riskAccepted!==true)return json({error:'请先确认直接在项目中执行的风险'},400);
      await db.prepare('UPDATE devices SET direct_execution=? WHERE id=?').bind(b.directExecution?1:0,device.id).run();
    }
    if(method==='GET'||method==='POST') {
      const row=await db.prepare('SELECT direct_execution FROM devices WHERE id=?').bind(device.id).first();
      return row?json({directExecution:row.direct_execution===1}):json({error:'请先配对电脑'},401);
    }
  }
  if (/^\/connector\/tasks\/[a-zA-Z0-9-]+\/deliverables\/[a-f0-9]{64}$/.test(path) && method === 'PUT') {
    const taskId=path.split('/')[3],id=path.split('/')[5],name=decodeURIComponent(request.headers.get('X-Filename')||''),kind=request.headers.get('X-Kind'),sha256=request.headers.get('X-Sha256'),size=Number(request.headers.get('X-Size'));
    const parts=name.split(/[\\/]/);
    if(!name || name.length>500 || /[:\x00-\x1f]/.test(name) || parts.some(p=>!p||p==='.'||p==='..'||p.toLowerCase()==='.git'||/^\.env($|\.)/i.test(p)||/\.(pem|key|jks|keystore)$/i.test(p)) || !['artifact','diff'].includes(kind) || !/^[a-f0-9]{64}$/.test(sha256||'') || !request.headers.has('X-Size') || !Number.isSafeInteger(size) || size<0 || size>50*1024*1024) return json({error:'无效产物；单文件最多50MiB，不允许敏感或越界路径'},400);
    let row=await db.prepare('SELECT * FROM deliverables WHERE task_id=? AND id=?').bind(taskId,id).first();
    const same=r=>r.name===name&&r.kind===kind&&r.sha256===sha256&&r.size===size;
    if(row&&!same(row))return json({error:'产物ID冲突，不能覆盖已有交付'},409);
    if(row?.ready===1)return json(row);
    const active="SELECT 1 FROM tasks t JOIN devices d ON d.id=t.device_id JOIN project_permissions p ON p.device_id=t.device_id AND p.project_id=t.project_id WHERE t.id=? AND t.status IN ('reading','running') AND (t.execution_mode<>'review' OR t.plan_decision='approved') AND t.cancel_requested=0 AND p.enabled=1 AND p.version=t.permission_version";
    if(!await db.prepare(active).bind(taskId).first())return json({error:'任务不再允许上传产物'},409);
    await db.prepare("INSERT INTO deliverables (task_id,id,name,kind,sha256,size,created_at) SELECT ?,?,?,?,?,?,? WHERE EXISTS ("+active+") AND (SELECT COUNT(*) FROM deliverables WHERE task_id=?)<64 AND (SELECT COALESCE(SUM(size),0) FROM deliverables WHERE task_id=?)+?<=104857600 ON CONFLICT(task_id,id) DO NOTHING").bind(taskId,id,name,kind,sha256,size,Date.now(),taskId,taskId,taskId,size).run();
    row=await db.prepare('SELECT * FROM deliverables WHERE task_id=? AND id=?').bind(taskId,id).first();
    if(!row||!same(row))return json({error:'产物冲突、超出配额或任务已停止'},409);
    const key='deliverables/'+taskId+'/'+id+'/'+sha256;
    try {
      const stream=new FixedLengthStream(size);
      const body=request.body || new Response(new Uint8Array()).body;
      await Promise.all([env.FILES.put(key,stream.readable,{sha256,httpMetadata:{contentType:'application/octet-stream'}}),body.pipeTo(stream.writable)]);
      await db.prepare('UPDATE deliverables SET ready=1 WHERE task_id=? AND id=? AND sha256=? AND EXISTS ('+active+')').bind(taskId,id,sha256,taskId).run();
      row=await db.prepare('SELECT * FROM deliverables WHERE task_id=? AND id=? AND sha256=? AND ready=1').bind(taskId,id,sha256).first();
      if(!row){await env.FILES.delete(key);return json({error:'任务已停止，未发布产物'},409);}
      return json(row);
    } catch(error) {
      await db.prepare('DELETE FROM deliverables WHERE task_id=? AND id=? AND sha256=? AND ready=0').bind(taskId,id,sha256).run();
      throw error;
    }
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+\/deliverables(?:\/[a-f0-9]{64})?$/.test(path) && method==='GET' && device) {
    const taskId=path.split('/')[2],id=path.split('/')[4];
    if(!await db.prepare('SELECT 1 FROM tasks WHERE id=? AND device_id=?').bind(taskId,device.id).first())return json({error:'任务不存在'},404);
    if(!id)return json({deliverables:(await db.prepare('SELECT id,name,kind,sha256,size FROM deliverables WHERE task_id=? AND ready=1 ORDER BY kind,name').bind(taskId).all()).results});
    const row=await db.prepare('SELECT * FROM deliverables WHERE task_id=? AND id=? AND ready=1').bind(taskId,id).first();
    const object=row&&await env.FILES.get('deliverables/'+taskId+'/'+id+'/'+row.sha256);
    if(!object)return json({error:'产物不存在或已删除'},404);
    return new Response(object.body,{headers:{'Content-Type':'application/octet-stream','Content-Length':String(row.size),'Content-Disposition':"attachment; filename*=UTF-8''"+encodeURIComponent(row.name.split(/[\\/]/).at(-1)),'X-Sha256':row.sha256,'X-Content-Type-Options':'nosniff','Cache-Control':'no-store'}});
  }
  if (path === '/connector/pairing-code') {
    if (method === 'POST') {
      await readBody(request, 1000);
      const code = [...crypto.getRandomValues(new Uint8Array(10))].map(b => b.toString(16).padStart(2,'0')).join('').toUpperCase();
      const now = Date.now(), expiresAt = now + 10*60*1000;
      await db.prepare('INSERT INTO pairing_codes (id,code_hash,created_at,expires_at) VALUES (1,?,?,?) ON CONFLICT(id) DO UPDATE SET code_hash=excluded.code_hash,created_at=excluded.created_at,expires_at=excluded.expires_at,consumed_at=NULL,device_id=NULL').bind(await hash(code),now,expiresAt).run();
      return json({ code: code.match(/.{5}/g).join('-'), expiresAt, ...identity(request, env) });
    }
    if (method === 'DELETE') {
      await db.prepare('UPDATE pairing_codes SET consumed_at=? WHERE id=1 AND consumed_at IS NULL').bind(Date.now()).run();
      return json({ ok:true });
    }
    if (method === 'GET') {
      const row = await db.prepare('SELECT expires_at,consumed_at FROM pairing_codes WHERE id=1').first();
      return json({ available:!!row && row.consumed_at===null && row.expires_at>Date.now(), expiresAt:row?.expires_at || null });
    }
  }
  if (/^\/connector\/tasks\/[a-zA-Z0-9-]+\/permission$/.test(path) && method === 'GET') {
    const id=path.split('/')[3];
    const row=await db.prepare("SELECT t.project_id,p.version,t.execution_mode,t.plan_decision,t.plan_version FROM tasks t JOIN project_permissions p ON p.device_id=t.device_id AND p.project_id=t.project_id JOIN devices d ON d.id=t.device_id WHERE t.id=? AND p.enabled=1 AND p.version=t.permission_version AND t.cancel_requested=0 AND t.status NOT IN ('completed','blocked','failed','cancelled')").bind(id).first();
    return json({taskId:id,projectId:row?.project_id || null,enabled:!!row,version:row?.version || null,profile:row && row.execution_mode!=='legacy-isolated'?'original-project':'isolated-workspace',executionMode:row?.execution_mode || null,planDecision:row?.plan_decision || null,planVersion:row?.plan_version || null});
  }
  if (/^\/connector\/tasks\/[a-zA-Z0-9-]+\/plan$/.test(path) && method === 'POST') {
    const id=path.split('/')[3],b=await readBody(request);
    if(![b.threadId,b.turnId].every(uuid)||!validPlanVersion(b.planVersion)||typeof b.report!=='string'||!b.report.trim()||b.report.length>100000)return json({error:'无效理解报告'},400);
    await db.prepare("UPDATE tasks SET plan_report=?,plan_turn_id=?,plan_version=?,status='awaiting_plan_approval',updated_at=? WHERE id=? AND execution_mode='review' AND status='planning' AND thread_id=? AND turn_id=? AND plan_version IS NULL AND plan_decision IS NULL AND cancel_requested=0 AND "+validPermission).bind(b.report,b.turnId,b.planVersion,Date.now(),id,b.threadId,b.turnId).run();
    const row=await db.prepare('SELECT * FROM tasks WHERE id=? AND cancel_requested=0 AND '+validPermission).bind(id).first();
    return row?.execution_mode==='review' && row.thread_id===b.threadId && row.plan_turn_id===b.turnId && row.plan_version===b.planVersion && row.plan_report===b.report ? json(row) : json({error:'理解报告已失效或与已保存版本冲突'},409);
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+\/plan-decision$/.test(path) && method === 'POST' && device) {
    const id=path.split('/')[2],b=await readBody(request,1000);
    if(!validPlanVersion(b.planVersion)||!['approved','rejected'].includes(b.decision))return json({error:'请选择批准或拒绝当前理解报告'},400);
    if(!await db.prepare('SELECT 1 FROM tasks WHERE id=? AND device_id=?').bind(id,device.id).first())return json({error:'任务不存在'},404);
    await db.prepare("UPDATE tasks SET plan_decision=?,plan_decided_at=?,status=?,cancel_requested=?,updated_at=? WHERE id=? AND device_id=? AND execution_mode='review' AND status='awaiting_plan_approval' AND plan_version=? AND plan_report IS NOT NULL AND plan_turn_id=turn_id AND plan_decision IS NULL AND cancel_requested=0 AND "+validPermission).bind(b.decision,Date.now(),b.decision==='approved'?'queued_execution':'cancelled',b.decision==='rejected'?1:0,Date.now(),id,device.id,b.planVersion).run();
    const row=await db.prepare('SELECT * FROM tasks WHERE id=? AND device_id=? AND '+validPermission).bind(id,device.id).first();
    return row?.plan_version===b.planVersion && row.plan_decision===b.decision && (b.decision==='rejected'||!row.cancel_requested) ? json(row) : json({error:'报告已失效、取消或已作出其他决定，请刷新'},409);
  }
  if (/^\/projects\/[a-zA-Z0-9-]+\/permission$/.test(path) && method === 'POST' && device) {
    const id=path.split('/')[2],b=await readBody(request);
    if(typeof b.enabled!=='boolean')return json({error:'请选择允许或停用'},400);
    const catalog=await db.prepare("SELECT value FROM state WHERE key='connector'").first();
    if(!catalog || !JSON.parse(catalog.value).projects.some(p=>p.id===id))return json({error:'项目不存在，请刷新'},404);
    const now=Date.now();
    await db.batch([
      db.prepare('INSERT INTO project_permissions (device_id,project_id,enabled,version,updated_at) VALUES (?,?,?,?,?) ON CONFLICT(device_id,project_id) DO UPDATE SET enabled=excluded.enabled,version=excluded.version,updated_at=excluded.updated_at WHERE project_permissions.enabled<>excluded.enabled').bind(device.id,id,b.enabled?1:0,crypto.randomUUID(),now),
      db.prepare("UPDATE tasks SET cancel_requested=1,status=CASE WHEN status IN ('queued','awaiting_plan_approval','queued_execution') THEN 'cancelled' ELSE status END,updated_at=? WHERE device_id=? AND project_id=? AND status NOT IN ('completed','blocked','failed','cancelled') AND NOT EXISTS (SELECT 1 FROM project_permissions p WHERE p.device_id=tasks.device_id AND p.project_id=tasks.project_id AND p.enabled=1 AND p.version=tasks.permission_version)").bind(now,device.id,id)
    ]);
    const row=await db.prepare('SELECT enabled,version FROM project_permissions WHERE device_id=? AND project_id=?').bind(device.id,id).first();
    return json({enabled:row.enabled===1,version:row.version,profile:'project-access'});
  }
  if (path === '/connector/approvals' && method === 'POST') {
    const b = await readBody(request, 50000), details = JSON.stringify(b.details);
    if (![b.id,b.taskId,b.threadId,b.turnId].every(uuid) || typeof b.itemId !== 'string' || !b.itemId || b.itemId.length > 200 || !b.details || b.details.kind !== 'command' || typeof b.details.command !== 'string' || !b.details.command.trim() || details.length > 40000) return json({ error: 'Invalid approval' }, 400);
    const task = await db.prepare('SELECT * FROM tasks WHERE id=?').bind(b.taskId).first();
    if (!task || !['running','waiting_for_approval'].includes(task.status) || (task.execution_mode==='review' && task.plan_decision!=='approved') || task.cancel_requested || task.thread_id !== b.threadId || task.turn_id !== b.turnId) return json({ error: 'Approval task is not active' }, 409);
    const now = Date.now();
    await db.prepare('INSERT INTO approvals (id,task_id,thread_id,turn_id,item_id,details,created_at,expires_at) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING').bind(b.id,b.taskId,b.threadId,b.turnId,b.itemId,details,now,now+15*60*1000).run();
    const row = await db.prepare('SELECT * FROM approvals WHERE id=?').bind(b.id).first();
    if (row.task_id !== b.taskId || row.thread_id !== b.threadId || row.turn_id !== b.turnId || row.item_id !== b.itemId || row.details !== details) return json({ error: 'Approval ID conflict' }, 409);
    return json(row);
  }
  if (path === '/connector/approvals/invalidate' && method === 'POST') {
    const b = await readBody(request); if (!uuid(b.taskId)) return json({ error: 'Invalid task' }, 400);
    await db.prepare("UPDATE approvals SET status='invalidated' WHERE task_id=? AND status IN ('pending','approved','denied')").bind(b.taskId).run();
    return json({ ok: true });
  }
  if (/^\/connector\/approvals\/[a-zA-Z0-9-]+$/.test(path) && method === 'GET') {
    const id = path.split('/')[3];
    // Re-check expiry, revocation, cancellation and the exact live turn before returning a decision.
    await db.prepare("UPDATE approvals SET status='expired' WHERE id=? AND status IN ('pending','approved','denied') AND expires_at<=?").bind(id,Date.now()).run();
    await db.prepare("UPDATE approvals SET status='invalidated' WHERE id=? AND status IN ('pending','approved','denied') AND NOT EXISTS (SELECT 1 FROM tasks t JOIN devices d ON d.id=t.device_id WHERE t.id=approvals.task_id AND t.thread_id=approvals.thread_id AND t.turn_id=approvals.turn_id AND t.cancel_requested=0 AND t.status IN ('running','waiting_for_approval'))").bind(id).run();
    const row = await db.prepare('SELECT * FROM approvals WHERE id=?').bind(id).first();
    return row ? json(row) : json({ error: 'Approval missing' },404);
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+\/approvals\/[a-zA-Z0-9-]+$/.test(path) && method === 'POST' && device) {
    const taskId = path.split('/')[2], id = path.split('/')[4], b = await readBody(request);
    if (!['approved','denied'].includes(b.decision)) return json({ error: '请选择仅本次批准或拒绝' },400);
    const row = await db.prepare('SELECT a.* FROM approvals a JOIN tasks t ON t.id=a.task_id WHERE a.id=? AND a.task_id=? AND t.device_id=?').bind(id,taskId,device.id).first();
    if (!row) return json({ error: '审批不存在' },404);
    const now=Date.now();
    await db.prepare("UPDATE approvals SET status=?,decided_at=? WHERE id=? AND status='pending' AND expires_at>? AND EXISTS (SELECT 1 FROM tasks t WHERE t.id=approvals.task_id AND t.thread_id=approvals.thread_id AND t.turn_id=approvals.turn_id AND t.device_id=? AND t.cancel_requested=0 AND t.status IN ('running','waiting_for_approval'))").bind(b.decision,now,id,now,device.id).run();
    const updated = await db.prepare("SELECT a.* FROM approvals a JOIN tasks t ON t.id=a.task_id WHERE a.id=? AND t.device_id=? AND t.thread_id=a.thread_id AND t.turn_id=a.turn_id AND t.cancel_requested=0 AND t.status IN ('running','waiting_for_approval')").bind(id,device.id).first();
    return updated?.status === b.decision && updated.expires_at>now ? json({ ok:true, decision:updated.status }) : json({ error:'审批已过期、取消或已作出其他决定，请刷新' },409);
  }
  if (path === '/device/revoke' && method === 'POST' && device) {
    await db.batch([
      db.prepare("UPDATE tasks SET cancel_requested=1,status=CASE WHEN status IN ('queued','awaiting_plan_approval','queued_execution') THEN 'cancelled' ELSE status END,updated_at=? WHERE device_id=? AND status NOT IN ('completed','blocked','failed','cancelled')").bind(Date.now(), device.id),
      db.prepare('DELETE FROM project_permissions WHERE device_id=?').bind(device.id),
      db.prepare('DELETE FROM devices WHERE id=?').bind(device.id)
    ]);
    return json({ ok: true });
  }
  if (path === '/connector/transcribe' && method === 'POST') {
    const b = await readBody(request, 18000000);
    if (typeof b.audio !== 'string' || !b.audio.length) return json({ error: 'Audio required' }, 400);
    const result = await env.AI.run('@cf/openai/whisper-large-v3-turbo', { audio: b.audio, task: 'transcribe', vad_filter: true });
    return json(result);
  }
  if ((path === '/admin/migrate' || path === '/connector/migrate') && method === 'POST') {
    // Adds only the columns this Worker version needs and that are still missing; never drops or rewrites data.
    const wanted = { model: 'TEXT', effort: 'TEXT', title: 'TEXT', material_summary: 'TEXT', preview_url: 'TEXT', preview_expires_at: 'INTEGER', preview_status: 'TEXT', preview_kind: 'TEXT', preview_version: 'TEXT', preview_requested_at: 'INTEGER', sync_version: 'INTEGER NOT NULL DEFAULT 0' };
    const present = new Set((await db.prepare("SELECT name FROM pragma_table_info('tasks')").all()).results.map(row => row.name));
    const added = [];
    for (const [column, type] of Object.entries(wanted)) if (!present.has(column)) { await db.prepare(`ALTER TABLE tasks ADD COLUMN ${column} ${type}`).run(); added.push(column); }
    await db.batch([
      db.prepare("INSERT INTO state(key,value) VALUES ('task_sync_version',(SELECT COALESCE(MAX(sync_version),0) FROM tasks)) ON CONFLICT(key) DO UPDATE SET value=MAX(CAST(state.value AS INTEGER),CAST(excluded.value AS INTEGER))"),
      db.prepare("CREATE TRIGGER IF NOT EXISTS tasks_sync_insert AFTER INSERT ON tasks BEGIN UPDATE state SET value=CAST(value AS INTEGER)+1 WHERE key='task_sync_version'; UPDATE tasks SET sync_version=(SELECT CAST(value AS INTEGER) FROM state WHERE key='task_sync_version') WHERE id=NEW.id; END"),
      db.prepare("CREATE TRIGGER IF NOT EXISTS tasks_sync_update AFTER UPDATE ON tasks WHEN NEW.sync_version=OLD.sync_version BEGIN UPDATE state SET value=CAST(value AS INTEGER)+1 WHERE key='task_sync_version'; UPDATE tasks SET sync_version=(SELECT CAST(value AS INTEGER) FROM state WHERE key='task_sync_version') WHERE id=NEW.id; END"),
      db.prepare('UPDATE tasks SET sync_version=sync_version WHERE sync_version=0')
    ]);
    return json({ ok: true, added });
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+\/preview\/reopen$/.test(path) && method === 'POST' && device) {
    const id = path.split('/')[2];
    const task = await db.prepare('SELECT * FROM tasks WHERE id=? AND device_id=? AND ' + validPermission).bind(id, device.id).first();
    if (!task) return json({ error: '任务不可用' }, 404);
    if (!terminal.has(task.status) || task.cancel_requested || !task.preview_url) return json({ error: '这个任务当前不能重开预览' }, 409);
    if (task.preview_kind === 'snapshot' && env.PREVIEW_ORIGIN) {
      const snapshot = await db.prepare('SELECT * FROM preview_snapshots WHERE task_id=? AND version=? AND ready=1').bind(id,task.preview_version).first();
      if (snapshot) {
        const route = new URL(task.preview_url);
        const path = route.pathname.replace(/^\/s\/[a-f0-9]{64}/,'') + route.search + route.hash;
        const preview = await issuePreview(env,snapshot,task.preview_requested_at,path);
        return preview ? json({ requested: true, ...preview }) : json({ error: 'Snapshot has expired or permission changed' },410);
      }
    }
    await db.prepare("UPDATE tasks SET preview_status='reopening',preview_requested_at=COALESCE(preview_requested_at,?),updated_at=? WHERE id=?").bind(Date.now(), Date.now(), id).run();
    return json({ requested: true });
  }
  if (path === '/connector/previews' && method === 'GET') {
    const tasks = (await db.prepare("SELECT id,preview_status,preview_requested_at FROM tasks WHERE preview_status IN ('ready','reopening') AND cancel_requested=0 AND " + validPermission + " AND NOT EXISTS (SELECT 1 FROM preview_snapshots s WHERE s.task_id=tasks.id AND s.version=tasks.preview_version AND s.ready=1) ORDER BY CASE WHEN preview_status='reopening' THEN 0 ELSE 1 END,updated_at DESC LIMIT 32").all()).results;
    return json({ tasks });
  }
  if (/^\/connector\/tasks\/[a-zA-Z0-9-]+\/preview$/.test(path) && method === 'POST') {
    const id = path.split('/')[3], b = await readBody(request, 6000);
    // A stored snapshot no longer depends on the desktop process or its timer.
    // In particular a late local stop must not erase a link reopened by the phone.
    if (env.PREVIEW_ORIGIN && await db.prepare('SELECT 1 FROM preview_snapshots s JOIN tasks t ON t.id=s.task_id WHERE t.id=? AND t.preview_kind=\'snapshot\' AND t.preview_version=s.version AND s.ready=1').bind(id).first()) return json({ ok: true, cloudManaged: true });
    if (!['ready','expired','stopped','unavailable'].includes(b.status)) return json({ error: '无效预览状态' }, 400);
    if (b.kind != null && !['snapshot','live'].includes(b.kind)) return json({ error: '无效预览类型' }, 400);
    if (b.version != null && (typeof b.version !== 'string' || b.version.length > 128)) return json({ error: '无效预览版本' }, 400);
    if (b.requestedAt != null && (!Number.isSafeInteger(b.requestedAt) || b.requestedAt <= 0)) return json({ error: '无效预览请求版本' }, 400);
    if (b.url != null) {
      let link; try { link = new URL(b.url); } catch { return json({ error: '无效预览链接' }, 400); }
      if (!validPreviewUrl(b.url, env)) return json({ error: '无效预览链接' }, 400);
    }
    if (b.status === 'ready' && (!b.url || !Number.isSafeInteger(b.expiresAt) || b.expiresAt <= Date.now() || !b.kind || !b.version)) return json({ error: '缺少有效预览信息' }, 400);
    const requestedAt = b.requestedAt ?? null;
    const updated = await db.prepare('UPDATE tasks SET preview_status=?,preview_url=COALESCE(?,preview_url),preview_expires_at=COALESCE(?,preview_expires_at),preview_kind=COALESCE(?,preview_kind),preview_version=COALESCE(?,preview_version),preview_requested_at=NULL,updated_at=? WHERE id=? AND cancel_requested=0 AND ' + validPermission + ' AND ((preview_requested_at IS NULL AND ? IS NULL) OR preview_requested_at=?)').bind(b.status,b.url || null,b.expiresAt || null,b.kind || null,b.version || null,Date.now(),id,requestedAt,requestedAt).run();
    if (updated.meta.changes) return json({ ok: true });
    const allowed = await db.prepare('SELECT id FROM tasks WHERE id=? AND cancel_requested=0 AND ' + validPermission).bind(id).first();
    return allowed ? json({ ok: true, superseded: true }) : json({ error: '任务不再允许预览' }, 409);
  }
  if (path === '/connector/sync' && method === 'POST') {
    const b = await readBody(request);
    if (!Array.isArray(b.projects)) return json({ error: 'Invalid projects' }, 400);
    const projects = b.projects.map(p => ({ id: p.id, name: p.name, available: p.available !== false }));
    const models = (Array.isArray(b.models) ? b.models : []).filter(m => m && validModelId(m.id)).slice(0, 30).map(m => {
      const efforts = (Array.isArray(m.efforts) ? m.efforts : []).filter(validEffort).slice(0, 10);
      return { id: m.id, displayName: String(m.displayName || m.id).slice(0, 64), efforts, defaultEffort: validEffort(m.defaultEffort) && (!efforts.length || efforts.includes(m.defaultEffort)) ? m.defaultEffort : null, isDefault: m.isDefault === true };
    });
    await db.prepare('INSERT INTO state VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value=excluded.value').bind('connector', JSON.stringify({ projects, models, lastSeen: Date.now(), name: b.name || '我的电脑' })).run();
    return json({ ok: true });
  }
  if (path === '/projects/activity' && method === 'GET' && device) return projectActivity(db,device.id);
  if (path === '/projects' && method === 'GET') {
    const row = await db.prepare('SELECT value FROM state WHERE key = ?').bind('connector').first();
    const state = row ? JSON.parse(row.value) : { projects: [], lastSeen: 0 };
    if(device){
      const permissions=(await db.prepare('SELECT project_id,enabled,version FROM project_permissions WHERE device_id=?').bind(device.id).all()).results;
      state.projects=state.projects.map(project=>{const p=permissions.find(p=>p.project_id===project.id);return {...project,permission:{enabled:p?.enabled===1,version:p?.version || null,profile:'project-access'}};});
    }
    return json({ ...state, online: Date.now() - state.lastSeen < 90000 });
  }
  if (path === '/uploads' && method === 'POST' && device) {
    const bytes = await request.arrayBuffer();
    if (!bytes.byteLength || bytes.byteLength > 50 * 1024 * 1024) return json({ error: '附件需要在 50 MB 以内' }, 413);
    const mime = request.headers.get('Content-Type') || 'application/octet-stream';
    if (!/^(image\/|video\/|text\/|application\/pdf)/.test(mime)) return json({ error: 'Unsupported attachment' }, 415);
    const name = decodeURIComponent(request.headers.get('X-Filename') || 'attachment').slice(0, 200);
    const id = crypto.randomUUID();
    await env.FILES.put('uploads/' + id, bytes, { httpMetadata: { contentType: mime } });
    await db.prepare('INSERT INTO uploads VALUES (?, ?, ?, ?, ?)').bind(id, device.id, name, mime, bytes.byteLength).run();
    await db.prepare('INSERT INTO upload_lifecycle (upload_id,created_at) VALUES (?,?)').bind(id,Date.now()).run();
    return json({ id, name, mime, size: bytes.byteLength });
  }
  if (path.startsWith('/uploads/') && method === 'GET' && connector) {
    const id = path.split('/')[2];
    if (!uuid(id)) return json({ error: 'Invalid id' }, 400);
    const o = await env.FILES.get('uploads/' + id);
    return o ? new Response(o.body, { headers: { 'Content-Type': o.httpMetadata?.contentType || 'application/octet-stream' } }) : json({ error: 'Missing file' }, 404);
  }
  if (path === '/tasks' && method === 'POST' && device) {
    const b = await readBody(request);
    if (!uuid(b.id) || !uuid(b.projectId) || typeof b.content !== 'string' || typeof b.message !== 'string' || b.content.length > 30000 || b.message.length > 15000) return json({ error: 'Invalid task' }, 400);
    const existing=await db.prepare('SELECT * FROM tasks WHERE id=?').bind(b.id).first();
    if(existing)return existing.device_id===device.id && existing.project_id===b.projectId && existing.content===b.content && existing.message===b.message && JSON.stringify(JSON.parse(existing.assets).map(a=>a.id))===JSON.stringify((b.assets || []).slice(0,8)) ? json(existing) : json({error:'Task ID conflict'},409);
    const stateRow = await db.prepare('SELECT value FROM state WHERE key = ?').bind('connector').first();
    const state = stateRow ? JSON.parse(stateRow.value) : null;
    const p = state && state.projects.find(p => p.id === b.projectId);
    if (!p || p.available === false) return json({ error: '项目不存在或已不可用，请刷新列表' }, 400);
    let choice; try { choice = resolveModelChoice(state, b.model ?? null, b.effort ?? null); } catch (error) { return json({ error: error.message }, 400); }
    const title = typeof b.title === 'string' ? b.title.trim().slice(0, 120) : '';
    const assets = [];
    for (const id of (b.assets || []).slice(0, 8)) {
      const asset = await db.prepare('SELECT * FROM uploads WHERE id=? AND device_id=?').bind(id, device.id).first();
      if (!asset) return json({ error: '附件不可用' }, 400);
      assets.push({ id: asset.id, name: asset.name, mime: asset.mime, size: asset.size });
    }
    if (!b.content.trim() && !assets.length) return json({ error: '请分享内容或附件' }, 400);
    await db.prepare("INSERT INTO tasks (id,device_id,project_id,project_name,content,message,assets,created_at,updated_at,permission_version,execution_mode,model,effort,title) SELECT ?,?,?,?,?,?,?,?,?,p.version,CASE WHEN d.direct_execution=1 THEN 'direct' ELSE 'review' END,?,?,? FROM project_permissions p JOIN devices d ON d.id=p.device_id WHERE p.device_id=? AND p.project_id=? AND p.enabled=1 ON CONFLICT(id) DO NOTHING").bind(b.id, device.id, b.projectId, p.name, b.content, b.message, JSON.stringify(assets), Date.now(), Date.now(), choice.model, choice.effort, title || null, device.id, b.projectId).run();
    const row = await db.prepare('SELECT * FROM tasks WHERE id=? AND device_id=?').bind(b.id, device.id).first();
    return row ? json(row) : json({ error: '请先在新版 App 中允许此项目的手机交办' }, 403);
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+\/followup$/.test(path) && method === 'POST' && device) {
    const parentId = path.split('/')[2], b = await readBody(request);
    if (!uuid(b.id) || typeof b.message !== 'string' || !b.message.trim() || b.message.length > 15000) return json({ error: '请填写有效追问（最多 15000 字符）' }, 400);
    const existing = await db.prepare('SELECT * FROM tasks WHERE id=?').bind(b.id).first();
    if (existing) return existing.device_id === device.id && existing.parent_task_id === parentId && existing.message === b.message ? json(existing) : json({ error: 'Task ID conflict' }, 409);
    const parent = await db.prepare('SELECT * FROM tasks WHERE id=? AND device_id=?').bind(parentId, device.id).first();
    if (!parent) return json({ error: '原任务不存在' }, 404);
    if (!parent.thread_id || !terminal.has(parent.status)) return json({ error: '请等待原任务结束并创建会话后再追问' }, 409);
    const permission=await db.prepare('SELECT version FROM project_permissions WHERE device_id=? AND project_id=? AND enabled=1').bind(device.id,parent.project_id).first();
    if(!permission)return json({error:'此项目未授权手机交办，请先允许'},403);
    const stateRow=await db.prepare('SELECT value FROM state WHERE key = ?').bind('connector').first();
    let choice; try { choice = resolveModelChoice(stateRow ? JSON.parse(stateRow.value) : null, b.model ?? parent.model ?? null, b.effort ?? parent.effort ?? null); } catch (error) { return json({ error: error.message }, 400); }
    // Single statement prevents two phones/retries from scheduling overlapping turns.
    await db.prepare("INSERT INTO tasks (id,device_id,project_id,project_name,content,message,assets,created_at,updated_at,thread_id,parent_task_id,permission_version,execution_mode,model,effort,title) SELECT ?,p.device_id,p.project_id,p.project_name,p.content,?,p.assets,?,?,p.thread_id,p.id,a.version,CASE WHEN d.direct_execution=1 THEN 'direct' ELSE 'review' END,?,?,p.title FROM tasks p JOIN project_permissions a ON a.device_id=p.device_id AND a.project_id=p.project_id JOIN devices d ON d.id=p.device_id WHERE p.id=? AND p.device_id=? AND a.enabled=1 AND p.status IN ('completed','blocked','failed','cancelled') AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.thread_id=p.thread_id AND t.status NOT IN ('completed','blocked','failed','cancelled')) ON CONFLICT(id) DO NOTHING").bind(b.id, b.message, Date.now(), Date.now(), choice.model, choice.effort, parentId, device.id).run();
    const row = await db.prepare('SELECT * FROM tasks WHERE id=? AND device_id=? AND parent_task_id=?').bind(b.id, device.id, parentId).first();
    return row ? json(row) : json({ error: '同一会话仍有任务等待或执行，完成后自动重试' }, 409);
  }
  if (path === '/tasks' && method === 'GET') {
    if (device && new URL(request.url).searchParams.has('project_id')) return projectHistory(request,db,device.id);
    const now = Date.now(), sinceValue = new URL(request.url).searchParams.get('since');
    let since = sinceValue === null ? null : Math.max(0, Number(sinceValue) || 0);
    const columns = 'tasks.*, (SELECT COUNT(*) FROM deliverables d WHERE d.task_id=tasks.id AND d.ready=1) AS deliverable_count';
    const ordering = "ORDER BY CASE WHEN status IN ('completed','blocked','failed','cancelled') THEN 1 ELSE 0 END,created_at DESC LIMIT 200";
    const query = connector ? db.prepare(`SELECT ${columns} FROM tasks ${ordering}`) : db.prepare(`SELECT ${columns} FROM tasks WHERE device_id=? ${ordering}`).bind(device.id);
    const tasks=(await query.all()).results;
    // Versions are assigned by SQLite in the write transaction, never before a delayed D1 write.
    const syncCursor = tasks.reduce((max, task) => Math.max(max, task.sync_version), 0);
    if (since > syncCursor) since = null; // Reconcile old millisecond cursors or a deleted newest row.
    const approvals=(await db.prepare("SELECT a.* FROM approvals a JOIN tasks t ON t.id=a.task_id WHERE a.status='pending' AND a.expires_at>? AND t.cancel_requested=0 AND t.status IN ('running','waiting_for_approval') AND t.thread_id=a.thread_id AND t.turn_id=a.turn_id").bind(Date.now()).all()).results;
    const stats = device ? await db.prepare("SELECT COUNT(*) AS total, SUM(CASE WHEN status NOT IN ('completed','blocked','failed','cancelled') THEN 1 ELSE 0 END) AS active, SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END) AS completed FROM tasks WHERE device_id=?").bind(device.id).first() : null;
    return json({ tasks: tasks.filter(task => since === null || task.sync_version > since || task.status === 'waiting_for_approval' || (task.preview_status === 'ready' && task.preview_expires_at <= now)).map(task=>({ ...task, preview_status: task.preview_status === 'ready' && task.preview_expires_at <= now ? 'expired' : task.preview_status, approvals: approvals.filter(a=>a.task_id===task.id).map(a=>({ id:a.id,details:JSON.parse(a.details),expiresAt:a.expires_at })) })), taskIds: tasks.map(task => task.id), syncCursor, ...(stats ? { stats: { total: stats.total || 0, active: stats.active || 0, completed: stats.completed || 0 } } : {}) });
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+$/.test(path) && method === 'GET' && device) {
    const task = await db.prepare('SELECT tasks.*, (SELECT COUNT(*) FROM deliverables d WHERE d.task_id=tasks.id AND d.ready=1) AS deliverable_count FROM tasks WHERE id=? AND device_id=?').bind(path.split('/')[2],device.id).first();
    return task ? json((await presentTasks(db,[task]))[0]) : json({ error: '任务不存在' },404);
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+$/.test(path) && method === 'DELETE' && device) {
    const id = path.split('/')[2];
    const task = await db.prepare('SELECT * FROM tasks WHERE id=? AND device_id=?').bind(id, device.id).first();
    if (!task) return json({ error: '任务不存在' }, 404);
    if (!terminal.has(task.status)) return json({ error: '请先停止任务，再删除记录' }, 409);
    // Keep the task until object cleanup succeeds, so deletion can be retried.
    for (const asset of JSON.parse(task.assets)) {
      const referenced = await db.prepare("SELECT 1 FROM tasks,json_each(tasks.assets) a WHERE tasks.id<>? AND json_extract(a.value,'$.id')=? LIMIT 1").bind(id, asset.id).first();
      if (!referenced) {
        await env.FILES.delete('uploads/' + asset.id);
        await db.prepare('DELETE FROM uploads WHERE id=? AND device_id=?').bind(asset.id, device.id).run();
        await db.prepare('DELETE FROM upload_lifecycle WHERE upload_id=?').bind(asset.id).run();
      }
    }
    const deliverables=(await db.prepare('SELECT id,sha256 FROM deliverables WHERE task_id=?').bind(id).all()).results;
    for(const file of deliverables)await env.FILES.delete('deliverables/'+id+'/'+file.id+'/'+file.sha256);
    for (const snapshot of (await db.prepare('SELECT id FROM preview_snapshots WHERE task_id=?').bind(id).all()).results) await deleteSnapshot(env,snapshot.id);
    await db.batch([db.prepare('DELETE FROM deliverables WHERE task_id=?').bind(id),db.prepare('DELETE FROM approvals WHERE task_id=?').bind(id),db.prepare('DELETE FROM tasks WHERE id=? AND device_id=?').bind(id, device.id)]);
    return json({ ok: true });
  }
  if (/^\/tasks\/[a-zA-Z0-9-]+\/cancel$/.test(path) && method === 'POST' && device) {
    const id = path.split('/')[2];
    await db.prepare("UPDATE tasks SET cancel_requested=1,status=CASE WHEN status IN ('queued','awaiting_plan_approval','queued_execution') THEN 'cancelled' ELSE status END,updated_at=? WHERE id=? AND device_id=? AND status NOT IN ('completed','blocked','failed','cancelled')").bind(Date.now(), id, device.id).run();
    return json({ ok: true });
  }
  if (path === '/connector/claim' && method === 'POST') {
    // A running task is reconciled after a crash, never blindly leased to another execution.
    await db.prepare("UPDATE tasks SET status='cancelled',cancel_requested=1,updated_at=? WHERE status IN ('queued','awaiting_plan_approval','queued_execution') AND NOT "+validPermission).bind(Date.now()).run();
    const task = await db.prepare("UPDATE tasks SET status='reading',updated_at=? WHERE id=(SELECT t.id FROM tasks t JOIN project_permissions p ON p.device_id=t.device_id AND p.project_id=t.project_id JOIN devices d ON d.id=t.device_id WHERE (t.status='queued' OR (t.status='queued_execution' AND t.execution_mode='review' AND t.plan_decision='approved')) AND t.cancel_requested=0 AND p.enabled=1 AND p.version=t.permission_version AND NOT EXISTS (SELECT 1 FROM tasks active WHERE active.status IN ('reading','planning','running','waiting_for_approval')) ORDER BY t.created_at,t.id LIMIT 1) RETURNING *").bind(Date.now()).first();
    return json({ task });
  }
  if (path === '/connector/recover' && method === 'GET') return json({ tasks: (await db.prepare("SELECT * FROM tasks WHERE status IN ('reading','planning','running','waiting_for_approval') ORDER BY created_at").all()).results });
  if (path === '/connector/task' && method === 'POST') {
    const b = await readBody(request);
    if (!uuid(b.id) || !allowedStatus.has(b.status)) return json({ error: 'Invalid state' }, 400);
    const task=await db.prepare('SELECT * FROM tasks WHERE id=?').bind(b.id).first();
    if(!task)return json({error:'任务不存在'},404);
    if(terminal.has(task.status))return json(task);
    if(['awaiting_plan_approval','queued_execution'].includes(task.status))return json(task);
    if(task.cancel_requested && ['reading','planning','running','waiting_for_approval','completed'].includes(b.status))return json(task);
    if((b.status==='planning' && (task.execution_mode!=='review'||task.plan_version)) || (task.execution_mode==='review' && task.plan_decision!=='approved' && ['running','waiting_for_approval','completed'].includes(b.status)))return json({error:'请先发布理解报告并等待手机批准'},409);
    if(task.plan_version && b.threadId && b.threadId!==task.thread_id)return json({error:'已批准的计划不能切换会话'},409);
    if(task.execution_mode==='review' && task.plan_decision==='approved' && (b.turnId||task.turn_id)===task.plan_turn_id && (['running','waiting_for_approval','completed'].includes(b.status)||task.turn_id!==task.plan_turn_id))return json({error:'规划回合已结束，不能覆盖执行回合'},409);
    const executing=['reading','planning','running','waiting_for_approval','completed'].includes(b.status);
    const title = typeof b.title === 'string' && b.title.trim() ? b.title.trim().slice(0, 120) : null;
    const materialSummary = typeof b.materialSummary === 'string' && b.materialSummary.trim() ? b.materialSummary.trim().slice(0, 2000) : null;
    const previewUrl = typeof b.previewUrl === 'string' && validPreviewUrl(b.previewUrl,env) ? b.previewUrl : null;
    const previewExpiresAt = previewUrl && Number.isSafeInteger(b.previewExpiresAt) ? b.previewExpiresAt : null;
    const result=await db.prepare("UPDATE tasks SET status=?,thread_id=COALESCE(?,thread_id),turn_id=COALESCE(?,turn_id),report=COALESCE(?,report),error=?,events=?,title=COALESCE(?,title),material_summary=COALESCE(?,material_summary),preview_url=COALESCE(?,preview_url),preview_expires_at=COALESCE(?,preview_expires_at),updated_at=? WHERE id=? AND status=? AND COALESCE(turn_id,'')=?"+(executing?' AND cancel_requested=0 AND '+validPermission:'')).bind(b.status,b.threadId||null,b.turnId||null,b.report||null,b.error||null,JSON.stringify((b.events||[]).slice(-25)),title,materialSummary,previewUrl,previewExpiresAt,Date.now(),b.id,task.status,task.turn_id||'').run();
    const row=await db.prepare('SELECT * FROM tasks WHERE id=?').bind(b.id).first();
    return result.meta.changes>0 || row.cancel_requested || terminal.has(row.status) || ['awaiting_plan_approval','queued_execution'].includes(row.status) ? json(row) : json({error:'任务状态或授权已变化，请刷新'},409);
  }
  if ((path === '/admin/release' || path === '/connector/release') && method === 'PUT') {
    await env.FILES.put('releases/droprun.apk', request.body, { httpMetadata: { contentType: 'application/vnd.android.package-archive' } });
    return json({ ok: true });
  }
  return json({ error: 'Not found' }, 404);
}

function publicPage() {
  return `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>DropRun Relay</title>
<style>body{margin:0;background:#f6f7f2;color:#18231b;font:17px/1.65 system-ui,sans-serif}main{max-width:640px;margin:auto;padding:72px 24px}a{color:#285533}h1{font-size:44px;letter-spacing:-.05em}a:focus-visible{outline:3px solid #6b8e40;outline-offset:4px}@media(prefers-reduced-motion:reduce){*{scroll-behavior:auto}}</style>
<main><h1>Your DropRun Relay.</h1><p>This is a private, self-hosted connection between your Android phone and Windows computer. Pair using the QR code displayed by your Connector.</p><p><a href="https://github.com/MrMaii/droprun/releases">Download Android and Windows</a> · <a href="https://github.com/MrMaii/droprun">Setup and source code</a></p><p>Keep your pairing code private. This server has no public account registration. Android release builds install alongside old debug builds; pair again to connect to this instance.</p><hr><p lang="zh-CN">这是你的自部署中转服务。请扫描电脑上的配对二维码。正式 Android 版与旧 debug 版并行安装，重新配对后连接到此实例。电脑需保持开机和联网。</p></main></html>`;
}

function pairPage() {
  return `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="referrer" content="no-referrer"><title>Connect DropRun</title>
<style>body{margin:0;background:#f6f7f2;color:#18231b;font:17px/1.7 system-ui,sans-serif}main{max-width:520px;margin:auto;padding:48px 24px}code{display:block;margin:18px 0;padding:16px;border-radius:16px;background:#e7eddf;font-size:22px;overflow-wrap:anywhere}a{display:block;padding:14px;color:#163620}button,a{min-height:48px}a:focus-visible{outline:3px solid #537d2a}</style>
<main><h1>Connect your phone.</h1><p id="lead">Scan the QR code shown by your computer.</p><p id="relay"></p><code id="code" hidden></code><a id="open" hidden>Open in DropRun / 在 DropRun 中打开</a><a href="/download.apk">Download Android / 下载 Android</a><p>Single-use code, valid for 10 minutes. Confirm the server address in the app before pairing.</p><p lang="zh-CN">配对码只能使用一次，10 分钟内有效。请在 App 中确认服务器地址。</p></main>
<script>
const params=new URLSearchParams(location.hash.slice(1)),code=params.get('code'),instance=params.get('instance');
if(code&&instance&&/^[0-9a-f-]+$/i.test(code)&&/^[0-9a-f-]{36}$/i.test(instance)){document.getElementById('lead').textContent='Connect to the computer that showed this QR code.';document.getElementById('relay').textContent=location.origin;const c=document.getElementById('code');c.textContent=code;c.hidden=false;const a=document.getElementById('open');a.href='intent://pair?relay='+encodeURIComponent(location.origin)+'&instance='+encodeURIComponent(instance)+'&code='+encodeURIComponent(code)+'#Intent;scheme=droprun;package=app.droprun.mobile;end';a.hidden=false;history.replaceState(null,'',location.pathname);}
</script></html>`;
}
