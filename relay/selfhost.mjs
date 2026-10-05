const json = (data, status = 200) => Response.json(data, { status, headers: { 'Cache-Control': 'no-store' } });
export const digest = async value => [...new Uint8Array(await crypto.subtle.digest('SHA-256', typeof value === 'string' ? new TextEncoder().encode(value) : value))].map(b => b.toString(16).padStart(2, '0')).join('');
const ended = "('completed','blocked','failed','cancelled')";
const day = 86400000;
export const identity = (request, env) => ({ instanceId: env.INSTANCE_ID || null, relayUrl: new URL(request.url).origin, protocolVersion: 2 });

export async function health(request, env) {
  let schemaVersion = 0, storage = false;
  try {
    schemaVersion = Number((await env.DB.prepare("SELECT value FROM state WHERE key='schema_version'").first())?.value || 0);
    await env.FILES.head('health-probe');
    storage = true;
  } catch {}
  const ready = schemaVersion === 11 && storage && !!env.CONNECTOR_HASH && !!env.INSTANCE_ID;
  return json({ ok: ready, service: 'DropRun', version: '0.5.4', ...identity(request, env), schemaVersion, ready }, ready ? 200 : 503);
}

export async function presentTasks(db, tasks) {
  if (!tasks.length) return [];
  const ids = tasks.map(t => t.id);
  const approvals = [];
  for (let i = 0; i < ids.length; i += 80) {
    const chunk = ids.slice(i,i+80);
    approvals.push(...(await db.prepare(`SELECT a.* FROM approvals a JOIN tasks t ON t.id=a.task_id WHERE a.task_id IN (${chunk.map(() => '?').join(',')}) AND a.status='pending' AND a.expires_at>? AND t.cancel_requested=0 AND t.status IN ('running','waiting_for_approval') AND t.thread_id=a.thread_id AND t.turn_id=a.turn_id`).bind(...chunk, Date.now()).all()).results);
  }
  return tasks.map(t => ({ ...t, preview_status: t.preview_status === 'ready' && t.preview_expires_at <= Date.now() ? 'expired' : t.preview_status, approvals: approvals.filter(a => a.task_id === t.id).map(a => ({ id: a.id, details: JSON.parse(a.details), expiresAt: a.expires_at })) }));
}

export async function projectActivity(db, deviceId) {
  const saved = await db.prepare("SELECT value FROM state WHERE key='connector'").first();
  const state = saved ? JSON.parse(saved.value) : { projects: [], lastSeen: 0 };
  const rows = (await db.prepare(`WITH ranked AS (SELECT *,ROW_NUMBER() OVER(PARTITION BY project_id ORDER BY created_at DESC,id DESC) AS recent FROM tasks WHERE device_id=?) SELECT project_id, COUNT(DISTINCT COALESCE(root_task_id,id)) AS task_count, COUNT(*) AS dispatch_count, SUM(CASE WHEN status NOT IN ${ended} THEN 1 ELSE 0 END) AS active_count, SUM(CASE WHEN status IN ('waiting_for_approval','awaiting_plan_approval','blocked','failed') THEN 1 ELSE 0 END) AS attention_count, MAX(created_at) AS last_dispatch_at, MAX(history_incomplete) AS history_incomplete, MAX(CASE WHEN recent=1 THEN project_name END) AS saved_name, MAX(CASE WHEN recent=1 THEN status END) AS last_status FROM ranked GROUP BY project_id ORDER BY last_dispatch_at DESC,project_id`).bind(deviceId).all()).results;
  const permissions = (await db.prepare('SELECT project_id,enabled,version FROM project_permissions WHERE device_id=?').bind(deviceId).all()).results;
  const projects = [];
  for (const row of rows) {
    const project = state.projects.find(p => p.id === row.project_id), p = permissions.find(p => p.project_id === row.project_id);
    projects.push({ ...row, id: row.project_id, name: project?.name || row.saved_name, available: !!project && project.available !== false, history_incomplete: !!row.history_incomplete, permission: { enabled: p?.enabled === 1, version: p?.version || null, profile: 'project-access' } });
  }
  return json({ projects, online: Date.now() - state.lastSeen < 90000, heartbeat: state.lastSeen });
}

export async function projectHistory(request, db, deviceId) {
  const params = new URL(request.url).searchParams, projectId = params.get('project_id');
  const limit = Number(params.get('limit') || 50);
  if (!/^[a-zA-Z0-9-]{20,64}$/.test(projectId || '') || !Number.isSafeInteger(limit) || limit < 1 || limit > 100) return json({ error: 'Invalid project history request' }, 400);
  let cursor = null;
  if (params.has('cursor')) {
    try {
      cursor = JSON.parse(atob(params.get('cursor').replace(/-/g, '+').replace(/_/g, '/')));
      if (cursor.projectId !== projectId || !Number.isSafeInteger(cursor.at) || !/^[a-zA-Z0-9-]{20,64}$/.test(cursor.id)) throw new Error();
    } catch { return json({ error: 'Invalid history cursor' }, 400); }
  }
  const sql = `SELECT tasks.*, (SELECT COUNT(*) FROM deliverables d WHERE d.task_id=tasks.id AND d.ready=1) AS deliverable_count FROM tasks WHERE device_id=? AND project_id=? ${cursor ? 'AND (created_at<? OR (created_at=? AND id<?))' : ''} ORDER BY created_at DESC,id DESC LIMIT ?`;
  const rows = (await db.prepare(sql).bind(deviceId,projectId,...(cursor ? [cursor.at,cursor.at,cursor.id] : []),limit + 1).all()).results;
  const hasMore = rows.length > limit, tasks = rows.slice(0,limit), last = tasks.at(-1);
  const nextCursor = hasMore ? btoa(JSON.stringify({ projectId, at: last.created_at, id: last.id })).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'') : null;
  return json({ tasks: await presentTasks(db,tasks), nextCursor, hasMore });
}

export function retention(env) {
  const days = (value, fallback) => Number.isSafeInteger(Number(value)) && Number(value) >= 1 && Number(value) <= 365 ? Number(value) : fallback;
  return { rawDays: days(env.RAW_RETENTION_DAYS,7), artifactDays: days(env.ARTIFACT_RETENTION_DAYS,30) };
}

export async function deleteSnapshot(env, id) {
  const files = (await env.DB.prepare('SELECT path FROM preview_files WHERE snapshot_id=?').bind(id).all()).results;
  for (const file of files) await env.FILES.delete('snapshots/' + id + '/' + file.path);
  await env.DB.batch([env.DB.prepare('DELETE FROM preview_sessions WHERE snapshot_id=?').bind(id),env.DB.prepare('DELETE FROM preview_files WHERE snapshot_id=?').bind(id),env.DB.prepare('DELETE FROM preview_snapshots WHERE id=?').bind(id)]);
}

// Bounded batches; objects are removed before metadata so a failed run can retry safely.
export async function cleanupRetention(env, now = Date.now()) {
  const db = env.DB, policy = retention(env), counts = { uploads: 0, deliverables: 0, snapshots: 0 };
  const uploads = (await db.prepare(`SELECT u.id FROM uploads u LEFT JOIN upload_lifecycle l ON l.upload_id=u.id WHERE NOT EXISTS (SELECT 1 FROM tasks t,json_each(t.assets) a WHERE json_extract(a.value,'$.id')=u.id AND (t.status NOT IN ${ended} OR COALESCE(t.terminal_at,t.updated_at)>?)) AND (l.created_at<? OR EXISTS (SELECT 1 FROM tasks t,json_each(t.assets) a WHERE json_extract(a.value,'$.id')=u.id AND COALESCE(t.terminal_at,t.updated_at)<=?)) LIMIT 100`).bind(now-policy.rawDays*day,now-policy.rawDays*day,now-policy.rawDays*day).all()).results;
  for (const row of uploads) {
    await env.FILES.delete('uploads/' + row.id);
    await db.batch([db.prepare('DELETE FROM uploads WHERE id=?').bind(row.id),db.prepare('DELETE FROM upload_lifecycle WHERE upload_id=?').bind(row.id)]);
    counts.uploads++;
  }
  const files = (await db.prepare(`SELECT d.task_id,d.id,d.sha256 FROM deliverables d JOIN tasks t ON t.id=d.task_id WHERE t.status IN ${ended} AND COALESCE(t.terminal_at,t.updated_at)<? LIMIT 100`).bind(now-policy.artifactDays*day).all()).results;
  for (const row of files) {
    await env.FILES.delete(`deliverables/${row.task_id}/${row.id}/${row.sha256}`);
    await db.prepare('DELETE FROM deliverables WHERE task_id=? AND id=?').bind(row.task_id,row.id).run();
    counts.deliverables++;
  }
  const snapshots = (await db.prepare(`SELECT p.id FROM preview_snapshots p LEFT JOIN tasks t ON t.id=p.task_id WHERE p.expires_at<=? OR t.id IS NULL LIMIT 20`).bind(now).all()).results;
  for (const snapshot of snapshots) { await deleteSnapshot(env,snapshot.id); counts.snapshots++; }
  await db.prepare('DELETE FROM preview_sessions WHERE expires_at<=?').bind(now).run();
  return counts;
}

export function validPreviewUrl(value, env) {
  try {
    const url = new URL(value);
    if (url.protocol !== 'https:' || url.username || url.password || value.length > 2048) return false;
    if (env.PREVIEW_ORIGIN && url.origin === new URL(env.PREVIEW_ORIGIN).origin && /^\/s\/[a-f0-9]{64}\//.test(url.pathname)) return true;
    if (env.LIVE_PREVIEW_ORIGIN && url.origin === new URL(env.LIVE_PREVIEW_ORIGIN).origin && url.searchParams.get('k')) return true;
    // Compatibility only for existing private installs; self-deployment never enables Quick Tunnels.
    return !env.INSTANCE_ID && url.hostname.endsWith('.trycloudflare.com') && !!url.searchParams.get('k');
  } catch { return false; }
}
