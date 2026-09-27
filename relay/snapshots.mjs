import { digest, retention } from './selfhost.mjs';

const json = (data, status = 200) => Response.json(data, { status, headers: { 'Cache-Control': 'no-store' } });
const uuid = value => /^[a-zA-Z0-9-]{20,64}$/.test(value || '');
const sha = value => /^[a-f0-9]{64}$/.test(value || '');
const types = new Set(['text/html','text/css','text/javascript','application/javascript','application/json','image/png','image/jpeg','image/webp','image/gif','image/svg+xml','image/avif','image/x-icon','font/woff','font/woff2','font/ttf','application/wasm','text/plain','video/mp4','video/webm','audio/mpeg']);
export function validSnapshotPath(value) {
  return typeof value === 'string' && value.length > 0 && value.length <= 500 && !/[\\:\x00-\x1f?#%]/.test(value) && value.split('/').every(p => p && p !== '.' && p !== '..' && !/^(\.git|\.env(?:\..*)?|node_modules)$/i.test(p) && !/\.(pem|key|keystore|jks)$/i.test(p));
}
async function eligible(db, id) {
  return db.prepare("SELECT t.* FROM tasks t JOIN devices d ON d.id=t.device_id JOIN project_permissions p ON p.device_id=t.device_id AND p.project_id=t.project_id WHERE t.id=? AND t.cancel_requested=0 AND p.enabled=1 AND p.version=t.permission_version AND (t.execution_mode<>'review' OR t.plan_decision='approved')").bind(id).first();
}
async function body(request) {
  const raw = await request.text();
  if (raw.length > 4000000) throw new Error('Snapshot manifest too large');
  return JSON.parse(raw);
}
export async function issuePreview(env, snapshot, requestedAt = null, route = null) {
  const task = await eligible(env.DB,snapshot.task_id);
  if (!task || !snapshot.ready || snapshot.expires_at <= Date.now()) return null;
  const origin = new URL(env.PREVIEW_ORIGIN);
  if (origin.protocol !== 'https:' || origin.username || origin.password || origin.pathname !== '/') throw new Error('Configure an HTTPS preview origin');
  const token = [...crypto.getRandomValues(new Uint8Array(32))].map(b => b.toString(16).padStart(2,'0')).join('');
  const expiresAt = Math.min(Date.now()+30*60000,snapshot.expires_at), manifest = JSON.parse(snapshot.manifest);
  const path = route || '/' + manifest.entryPath.split('/').map(encodeURIComponent).join('/');
  if (typeof path !== 'string' || !path.startsWith('/') || path.startsWith('//') || /[\\\x00-\x1f]/.test(path) || path.length > 2000 || new URL(path,origin).origin !== origin.origin) throw new Error('Invalid preview route');
  const url = origin.origin + '/s/' + token + path;
  const tokenHash = await digest(token);
  await env.DB.prepare('INSERT INTO preview_sessions(token_hash,snapshot_id,expires_at) VALUES(?,?,?)').bind(tokenHash,snapshot.id,expiresAt).run();
  const result = await env.DB.prepare("UPDATE tasks SET preview_status='ready',preview_kind='snapshot',preview_version=?,preview_url=?,preview_expires_at=?,preview_requested_at=NULL,updated_at=? WHERE id=? AND cancel_requested=0 AND ((preview_requested_at IS NULL AND ? IS NULL) OR preview_requested_at=?) AND EXISTS (SELECT 1 FROM project_permissions p JOIN devices d ON d.id=p.device_id WHERE p.device_id=tasks.device_id AND p.project_id=tasks.project_id AND p.enabled=1 AND p.version=tasks.permission_version)").bind(snapshot.version,url,expiresAt,Date.now(),snapshot.task_id,requestedAt,requestedAt).run();
  if (!result.meta.changes) { await env.DB.prepare('DELETE FROM preview_sessions WHERE token_hash=?').bind(tokenHash).run(); return null; }
  return { url, expiresAt, kind: 'snapshot', version: snapshot.version };
}

export async function snapshotRoute(request, env) {
  const url = new URL(request.url), parts = url.pathname.split('/'), taskId = parts[3], snapshotId = parts[5], action = parts[6];
  if (!uuid(taskId)) return json({ error: 'Invalid task' },400);
  if (!env.PREVIEW_ORIGIN || new URL(env.PREVIEW_ORIGIN).origin === url.origin) return json({ error: 'Configure a separate PREVIEW_ORIGIN before publishing snapshots' },409);
  const task = await eligible(env.DB,taskId);
  if (!task) return json({ error: 'Task is no longer authorized for a preview' },409);
  if (!snapshotId && request.method === 'POST') {
    const b = await body(request);
    if (typeof b.version !== 'string' || !b.version || b.version.length > 128 || !Array.isArray(b.files) || !b.files.length || b.files.length > 10000 || !validSnapshotPath(b.entryPath || 'index.html')) return json({ error: 'Invalid snapshot manifest' },400);
    const files = b.files.map(f => ({ path: f.path, sha256: f.sha256, size: f.size, contentType: f.contentType || 'application/octet-stream' })).sort((a,b) => a.path.localeCompare(b.path));
    if (files.some(f => !validSnapshotPath(f.path) || !sha(f.sha256) || !Number.isSafeInteger(f.size) || f.size < 0 || f.size > 50*1024*1024 || (!types.has(f.contentType) && f.contentType !== 'application/octet-stream')) || new Set(files.map(f => f.path)).size !== files.length || files.reduce((n,f) => n+f.size,0) > 250*1024*1024 || !files.some(f => f.path === (b.entryPath || 'index.html') && f.contentType === 'text/html')) return json({ error: 'Invalid files, entry page or snapshot quota' },400);
    const manifest = JSON.stringify({ files, entryPath: b.entryPath || 'index.html', spa: b.spa === true });
    const existing = await env.DB.prepare('SELECT * FROM preview_snapshots WHERE task_id=? AND version=?').bind(taskId,b.version).first();
    if (existing && existing.manifest !== manifest) return json({ error: 'Snapshot version already describes different bytes' },409);
    if (existing?.expires_at <= Date.now()) return json({ error: 'Snapshot retention expired; create a new delivery' },410);
    const id = existing?.id || crypto.randomUUID();
    if (!existing) {
      const budget = await env.DB.prepare('SELECT COALESCE(SUM(size),0) AS size FROM preview_files').first();
      if (budget.size + files.reduce((n,f) => n+f.size,0) > 1024*1024*1024) return json({ error: 'Snapshot storage budget reached; wait for retention cleanup' },409);
      await env.DB.prepare('INSERT INTO preview_snapshots(id,task_id,version,manifest,created_at,expires_at) VALUES(?,?,?,?,?,?) ON CONFLICT(task_id,version) DO NOTHING').bind(id,taskId,b.version,manifest,Date.now(),Date.now()+retention(env).artifactDays*86400000).run();
    }
    const snapshot = await env.DB.prepare('SELECT * FROM preview_snapshots WHERE task_id=? AND version=?').bind(taskId,b.version).first();
    if (snapshot.manifest !== manifest) return json({ error: 'Snapshot version conflict' },409);
    for (let i = 0; i < files.length; i += 50) await env.DB.batch(files.slice(i,i+50).map(f => env.DB.prepare('INSERT INTO preview_files(snapshot_id,path,sha256,size,content_type) VALUES(?,?,?,?,?) ON CONFLICT(snapshot_id,path) DO NOTHING').bind(snapshot.id,f.path,f.sha256,f.size,f.contentType)));
    const missing = (await env.DB.prepare('SELECT path FROM preview_files WHERE snapshot_id=? AND ready=0 ORDER BY path').bind(snapshot.id).all()).results.map(f => f.path);
    return json({ snapshotId: snapshot.id, missing });
  }
  if (!uuid(snapshotId)) return json({ error: 'Invalid snapshot' },400);
  const snapshot = await env.DB.prepare('SELECT * FROM preview_snapshots WHERE id=? AND task_id=?').bind(snapshotId,taskId).first();
  if (!snapshot || snapshot.expires_at <= Date.now()) return json({ error: 'Snapshot is missing or expired' },410);
  if (action === 'files' && request.method === 'PUT') {
    const path = url.searchParams.get('path');
    if (!validSnapshotPath(path)) return json({ error: 'Invalid snapshot path' },400);
    const file = await env.DB.prepare('SELECT * FROM preview_files WHERE snapshot_id=? AND path=?').bind(snapshotId,path).first();
    if (!file) return json({ error: 'File is not in the immutable manifest' },404);
    const bytes = await request.arrayBuffer();
    if (bytes.byteLength !== file.size || await digest(bytes) !== file.sha256) return json({ error: 'Snapshot bytes do not match the manifest' },400);
    if (!file.ready) {
      const key = 'snapshots/' + snapshotId + '/' + path;
      await env.FILES.put(key,bytes,{sha256:file.sha256,httpMetadata:{contentType:file.content_type}});
      if (!await eligible(env.DB,taskId)) { await env.FILES.delete(key); return json({ error: 'Task authorization was revoked' },409); }
      await env.DB.prepare('UPDATE preview_files SET ready=1 WHERE snapshot_id=? AND path=?').bind(snapshotId,path).run();
    }
    return json({ ok: true });
  }
  if (action === 'publish' && request.method === 'POST') {
    const b = await body(request);
    if (b.requestedAt != null && (!Number.isSafeInteger(b.requestedAt) || b.requestedAt < 1)) return json({ error: 'Invalid preview request generation' },400);
    const expected = JSON.parse(snapshot.manifest).files.length;
    const count = await env.DB.prepare('SELECT COUNT(*) AS count FROM preview_files WHERE snapshot_id=? AND ready=1').bind(snapshotId).first();
    if (count.count !== expected) return json({ error: 'Snapshot upload is incomplete' },409);
    await env.DB.prepare('UPDATE preview_snapshots SET ready=1 WHERE id=?').bind(snapshotId).run();
    const preview = await issuePreview(env,{...snapshot,ready:1},b.requestedAt ?? null,b.path ?? null);
    return preview ? json(preview) : json({ error: 'Preview request or task permission changed' },409);
  }
  return json({ error: 'Snapshot route not found' },404);
}
