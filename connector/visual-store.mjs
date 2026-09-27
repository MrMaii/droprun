import { readFile, writeFile, mkdir, rename } from 'node:fs/promises';
import { join } from 'node:path';
import { createHash, randomUUID } from 'node:crypto';

/** Executor-owned receipts survive task teardown, upload failures and Connector restarts. */
export class VisualStore {
  constructor(stateDir) { this.stateDir = stateDir; this.writes = new Map(); }
  dir(taskId) {
    if (!/^[A-Za-z0-9-]{1,80}$/.test(taskId)) throw new Error('Invalid task ID');
    return join(this.stateDir, 'materials', taskId, 'visual');
  }
  async read(taskId) {
    try { return JSON.parse(await readFile(join(this.dir(taskId), 'manifest.json'), 'utf8')); }
    catch (error) { if (error.code !== 'ENOENT') throw error; return { screenshots: [], checks: [] }; }
  }
  async update(taskId, update) {
    const pending = (this.writes.get(taskId) || Promise.resolve()).catch(() => {}).then(async () => {
      const dir = this.dir(taskId); await mkdir(dir, { recursive: true });
      const data = await this.read(taskId);
      await update(data, dir);
      const file = join(dir, 'manifest.json'); await writeFile(file + '.tmp', JSON.stringify(data)); await rename(file + '.tmp', file);
      return data;
    });
    this.writes.set(taskId, pending);
    try { return await pending; } finally { if (this.writes.get(taskId) === pending) this.writes.delete(taskId); }
  }
  async addScreenshot(taskId, { png, name = 'after', ...metadata }) {
    let shot;
    await this.update(taskId, async (data, dir) => {
      if (data.screenshots.length >= 40) throw new Error('一个任务最多保留 40 张截图');
      const safe = name.replace(/[^A-Za-z0-9_-]/g, '').slice(0, 40) || 'after';
      const filename = safe + '-' + randomUUID().slice(0, 8) + '.png', path = join(dir, filename);
      await writeFile(path, png);
      shot = { ...metadata, name: filename, path, sha256: createHash('sha256').update(png).digest('hex'), capturedAt: Date.now() };
      data.screenshots.push(shot);
    });
    return shot;
  }
}
