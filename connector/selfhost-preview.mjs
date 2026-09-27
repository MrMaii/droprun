import { readdir, readFile, lstat } from 'node:fs/promises';
import { join, relative } from 'node:path';
import { createHash } from 'node:crypto';

const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.mjs': 'text/javascript', '.json': 'application/json', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.svg': 'image/svg+xml', '.webp': 'image/webp', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.woff': 'font/woff' };

export async function snapshotManifest(directory) {
  const files = [];
  async function walk(dir) {
    for (const name of (await readdir(dir)).sort()) {
      const file = join(dir, name), info = await lstat(file);
      if (info.isSymbolicLink()) throw new Error('Snapshot cannot contain symbolic links.');
      if (info.isDirectory()) await walk(file);
      else if (info.isFile()) {
        if (info.size > 50 * 1024 * 1024) throw new Error('A preview file exceeds 50 MiB.');
        const path = relative(directory, file).replaceAll('\\', '/');
        files.push({ path, sha256: createHash('sha256').update(await readFile(file)).digest('hex'), size: info.size, contentType: types[path.slice(path.lastIndexOf('.')).toLowerCase()] || 'application/octet-stream' });
      }
    }
  }
  await walk(directory);
  if (files.length > 10000 || files.reduce((n, f) => n + f.size, 0) > 250 * 1024 * 1024) throw new Error('Preview exceeds 10,000 files or 250 MiB.');
  return files;
}

export async function publishSnapshot(config, entry, fetchImpl = fetch) {
  const files = await snapshotManifest(entry.snapshot);
  const path = '/connector/tasks/' + encodeURIComponent(entry.taskId) + '/snapshots';
  const request = async (route, method, body, binary = false) => {
    const response = await fetchImpl(config.relay + route, { method, headers: { Authorization: 'Bearer ' + config.connectorToken, 'Content-Type': binary ? 'application/octet-stream' : 'application/json' }, body: binary ? body : JSON.stringify(body), signal: AbortSignal.timeout(120000) });
    if (!response.ok) throw new Error('Snapshot delivery failed (HTTP ' + response.status + '). Retry from the task.');
    return response.json();
  };
  const route = new URL(entry.path || '/', 'https://preview.invalid').pathname.replace(/^\/|\/$/g, '');
  const entryPath = [route, (route ? route + '/' : '') + 'index.html', route + '.html', 'index.html'].find(path => files.some(file => file.path === path && file.contentType === 'text/html'));
  if (!entryPath) throw new Error('A static preview needs an HTML entry page for its requested route.');
  const start = await request(path, 'POST', { version: entry.revision, files, entryPath, spa: entry.spa === true });
  if (typeof start.snapshotId !== 'string' || !/^[A-Za-z0-9-]{20,80}$/.test(start.snapshotId) || !Array.isArray(start.missing)) throw new Error('Invalid snapshot response.');
  const base = path + '/' + start.snapshotId;
  for (const name of start.missing) {
    const item = files.find(f => f.path === name);
    if (!item) throw new Error('Relay requested a file outside this snapshot.');
    const bytes = await readFile(join(entry.snapshot, item.path));
    if (createHash('sha256').update(bytes).digest('hex') !== item.sha256) throw new Error('Snapshot changed during upload.');
    await request(base + '/files?path=' + encodeURIComponent(item.path), 'PUT', bytes, true);
  }
  return request(base + '/publish', 'POST', { path: entry.path || '/' });
}
