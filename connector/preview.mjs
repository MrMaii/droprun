import { spawn } from 'node:child_process';
import { readFile, writeFile, mkdir, mkdtemp, readdir, lstat, realpath, copyFile, rm } from 'node:fs/promises';
import { existsSync, statSync, createReadStream } from 'node:fs';
import { createServer, request as httpRequest } from 'node:http';
import { join, resolve, extname, normalize, relative, isAbsolute } from 'node:path';
import { tmpdir } from 'node:os';
import { randomBytes, createHash } from 'node:crypto';
import { freePort } from './browser.mjs';

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const scripts = new Set(['dev', 'start', 'preview', 'serve', 'storybook', 'docs', 'docs:dev']);
const mime = { '.html': 'text/html;charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.mjs': 'text/javascript', '.json': 'application/json', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.svg': 'image/svg+xml', '.webp': 'image/webp', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.woff': 'font/woff', '.txt': 'text/plain;charset=utf-8', '.map': 'application/json' };
const inside = (root, path) => { const part = relative(root, path); return !isAbsolute(part) && part !== '..' && !part.startsWith('..\\') && !part.startsWith('../'); };
const privateFile = name => /^(?:\.git|node_modules|\.env(?:\..*)?|\.npmrc|\.yarnrc.*|\.netrc|\.local|\.ssh|\.aws|cookies.*|credentials.*|secrets?.*|service[-_]account.*|id_rsa.*|id_ed25519.*)$/i.test(name) || /\.(?:pem|key|p12|pfx)$/i.test(name);
const processClosures = new WeakMap();
function spawnOwned(...args) {
  const child = spawn(...args), state = { closed: false };
  state.promise = new Promise(resolve => child.once('close', () => { state.closed = true; resolve(); }));
  processClosures.set(child, state);
  return child;
}

async function stopOwned(child) {
  if (!child.pid) { child.kill(); return; }
  const state = processClosures.get(child);
  if (!state) throw new Error('无法确认预览进程的退出状态');
  if (state.closed) return;
  let timer;
  try {
    await Promise.race([
      (async () => {
        if (child.exitCode === null && child.signalCode === null) {
          if (process.platform === 'win32') {
            const killer = spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], { windowsHide: true, stdio: 'ignore' });
            await new Promise((resolve, reject) => { killer.once('error', reject); killer.once('close', code => code === 0 || state.closed ? resolve() : reject(new Error('预览进程终止失败（' + code + '）'))); });
          } else if (!child.kill() && !state.closed) throw new Error('预览进程终止失败');
        }
        await state.promise;
      })(),
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error('预览进程 5 秒内未确认退出，请重试关闭')), 5000); }),
    ]);
  } finally { clearTimeout(timer); }
}

async function snapshot(source, destination) {
  let files = 0, bytes = 0;
  const hash = createHash('sha256');
  async function copy(dir, target) {
    for (const name of (await readdir(dir)).sort()) {
      if (privateFile(name)) continue;
      const file = join(dir, name), out = target ? join(target, name) : null, info = await lstat(file);
      if (info.isSymbolicLink()) throw new Error('静态预览不接受符号链接：' + relative(source, file));
      if (info.isDirectory()) { if (out) await mkdir(out); await copy(file, out); }
      else if (info.isFile()) {
        if (++files > 10000 || (bytes += info.size) > 250 * 1024 * 1024) throw new Error('静态预览超过 10000 个文件或 250 MB；请选择构建产物目录');
        if (out) await copyFile(file, out);
        hash.update(relative(source, file).replaceAll('\\', '/') + '\0');
        hash.update(await readFile(out || file));
      }
    }
  }
  await copy(source, destination);
  return { revision: hash.digest('hex'), files, bytes };
}

/**
 * Only package.json scripts with conventional dev/preview names, a static directory, or python's http.server are
 * accepted: the Connector runs this outside Codex's sandbox, so the choice of command stays with the user's project.
 */
export async function resolveServerCommand(cwd, request) {
  if (request.static) {
    const dir = resolve(cwd, String(request.static));
    if (!inside(resolve(cwd), dir) || !existsSync(dir) || !statSync(dir).isDirectory() || !inside(await realpath(cwd), await realpath(dir))) throw new Error('静态目录不存在或不在项目内：' + request.static);
    return { kind: 'static', dir };
  }
  if (request.script) {
    const name = String(request.script);
    if (!scripts.has(name)) throw new Error(`只允许 ${[...scripts].join(' / ')} 这些脚本名，收到：${name}`);
    let pkg; try { pkg = JSON.parse(await readFile(join(cwd, 'package.json'), 'utf8')); } catch { throw new Error('项目没有 package.json，改用 static 指定目录'); }
    if (!pkg.scripts?.[name]) throw new Error(`package.json 里没有 "${name}" 脚本`);
    const manager = existsSync(join(cwd, 'pnpm-lock.yaml')) ? 'pnpm' : existsSync(join(cwd, 'yarn.lock')) ? 'yarn' : existsSync(join(cwd, 'bun.lockb')) || existsSync(join(cwd, 'bun.lock')) ? 'bun' : 'npm';
    const extra = Array.isArray(request.args) ? request.args.filter(a => typeof a === 'string' && /^[-A-Za-z0-9_=.:/]+$/.test(a)).slice(0, 8) : [];
    return { kind: 'script', executable: process.platform === 'win32' ? manager + '.cmd' : manager, args: [manager === 'npm' ? 'run' : null, name, ...(extra.length ? (manager === 'npm' ? ['--', ...extra] : extra) : [])].filter(Boolean), label: `${manager} run ${name}` };
  }
  if (request.command) {
    const match = String(request.command).trim().match(/^python(?:3)?\s+-m\s+http\.server\s+(\d{2,5})(?:\s+(?:-d|--directory)\s+([\w./-]+))?$/);
    if (!match) throw new Error('command 只接受 "python -m http.server <port> [-d dir]"；其他项目请用 script 或 static');
    return { kind: 'command', executable: 'python', args: ['-m', 'http.server', match[1], ...(match[2] ? ['-d', match[2]] : [])], label: request.command };
  }
  throw new Error('请提供 script（package.json 脚本名）、static（目录）或 command');
}

function serveStatic(dir, spa = false) {
  return createServer((req, res) => {
    const url = new URL(req.url, 'http://x');
    let path; try { path = normalize(decodeURIComponent(url.pathname)).replace(/^([.][.][\\/])+/, ''); } catch { res.statusCode = 400; return res.end('Invalid path'); }
    if (path.split(/[\\/]/).some(privateFile)) { res.statusCode = 404; return res.end('Not found'); }
    let file = resolve(dir, '.' + path);
    if (!inside(dir, file)) { res.statusCode = 403; return res.end(); }
    if (existsSync(file) && statSync(file).isDirectory()) file = join(file, 'index.html');
    if (!existsSync(file) && spa && !extname(file)) file = join(dir, 'index.html');
    if (!existsSync(file) || !statSync(file).isFile()) { if (existsSync(join(dir, '404.html'))) { file = join(dir, '404.html'); res.statusCode = 404; } else { res.statusCode = 404; return res.end('Not found'); } }
    res.setHeader('Content-Type', mime[extname(file).toLowerCase()] || 'application/octet-stream');
    res.setHeader('Cache-Control', 'no-store');
    const stream = createReadStream(file); stream.on('error', () => { res.destroy(); }); stream.pipe(res);
  });
}

async function waitForPort(port, deadlineMs, isAlive) {
  const deadline = Date.now() + deadlineMs;
  while (Date.now() < deadline) {
    try { const response = await fetch(`http://127.0.0.1:${port}/`, { signal: AbortSignal.timeout(2500), redirect: 'manual' }); if (response.status < 500 || response.status === 503) return true; } catch {}
    if (!isAlive()) return false;
    await sleep(700);
  }
  return false;
}

/** Token-gated reverse proxy: the public tunnel URL only works with the key the report carries. */
function gatedProxy(targetPort, token) {
  return createServer((req, res) => {
    const url = new URL(req.url, 'http://x');
    const cookie = (req.headers.cookie || '').split(';').map(s => s.trim()).find(s => s.startsWith('droprun_preview='))?.split('=')[1];
    if (url.searchParams.get('k') === token) {
      url.searchParams.delete('k');
      res.writeHead(302, { 'Set-Cookie': `droprun_preview=${token}; Path=/; HttpOnly; SameSite=Lax; Secure`, Location: url.pathname + (url.search || '') });
      return res.end();
    }
    if (cookie !== token) { res.writeHead(403, { 'Content-Type': 'text/html;charset=utf-8' }); return res.end('<!doctype html><meta charset="utf-8"><title>DropRun 预览</title><p style="font:16px system-ui;padding:24px">这个预览链接需要带上钥匙。请从手机上的任务详情打开，或使用报告里的完整链接。</p>'); }
    const upstream = httpRequest({ host: '127.0.0.1', port: targetPort, method: req.method, path: req.url, headers: { ...req.headers, host: `127.0.0.1:${targetPort}` } }, response => {
      const headers = { ...response.headers }; delete headers['content-security-policy'];
      res.writeHead(response.statusCode, headers); response.pipe(res);
    });
    upstream.on('error', () => { res.statusCode = 502; res.end('预览服务器没有响应'); });
    req.pipe(upstream);
  });
}

function startTunnel(port, bin, onSpawn = () => {}) {
  return new Promise((resolve, reject) => {
    const child = spawnOwned(bin, ['tunnel', '--url', `http://127.0.0.1:${port}`, '--no-autoupdate'], { windowsHide: true });
    onSpawn(child);
    let output = '', settled = false;
    const timer = setTimeout(() => { if (!settled) { settled = true; child.kill(); reject(new Error('隧道 60 秒内没有就绪：' + output.slice(-300))); } }, 60000);
    const look = chunk => {
      output = (output + chunk).slice(-4000);
      const match = output.match(/https:\/\/[a-z0-9-]+\.trycloudflare\.com/);
      if (match && !settled) { settled = true; clearTimeout(timer); resolve({ child, url: match[0] }); }
    };
    child.stdout.on('data', look); child.stderr.on('data', look);
    child.on('error', error => { if (!settled) { settled = true; clearTimeout(timer); reject(error); } });
    child.on('exit', code => { if (!settled) { settled = true; clearTimeout(timer); reject(new Error('隧道进程退出（' + code + '）：' + output.slice(-300))); } });
  });
}

export async function waitForPublicPreview(url, token, isAlive, timeoutMs = 45000) {
  const deadline = Date.now() + timeoutMs;
  let reason = '没有响应';
  while (Date.now() < deadline && isAlive()) {
    try {
      const response = await fetch(url, { redirect: 'manual', signal: AbortSignal.timeout(Math.max(1, Math.min(5000, deadline - Date.now()))) });
      const cookie = response.headers.get('set-cookie')?.split(';')[0];
      await response.body?.cancel();
      if (response.status === 302 && cookie === `droprun_preview=${token}`) return;
      reason = `HTTP ${response.status}，未收到预览访问凭据`;
    } catch (error) { reason = error.cause?.code || error.code || error.message; }
    await sleep(Math.max(0, Math.min(1000, deadline - Date.now())));
  }
  throw new Error('公网预览尚不可访问：' + reason);
}

/** Keeps at most one running preview per task; a new request replaces the previous one. */
export class Previews {
  constructor({ cloudflaredBin, minutes = 30, log = () => {}, tunnel = startTunnel, publicReady = waitForPublicPreview, snapshotDir = join(tmpdir(), 'droprun-preview-snapshots'), onStop = async () => {}, maxActive = 8, publishSnapshot = null, allowQuickTunnels = true, livePreviewOrigin = null, livePreviewPort = 47495, tunnelToken = null } = {}) {
    this.bin = cloudflaredBin; this.minutes = Math.max(0.001, Math.min(Number(minutes) || 30, 120)); this.log = log; this.tunnel = tunnel; this.active = new Map();
    this.snapshotDir = snapshotDir; this.onStop = onStop; this.maxActive = maxActive; this.publicReady = publicReady;
    this.publishSnapshot = publishSnapshot; this.allowQuickTunnels = allowQuickTunnels; this.livePreviewOrigin = livePreviewOrigin; this.livePreviewPort = livePreviewPort; this.tunnelToken = tunnelToken;
  }

  async start(taskId, cwd, request, restored = null) {
    await this.stop(taskId, 'replaced');
    if (this.active.size >= this.maxActive) throw new Error('预览已达到同时运行上限；请等待现有预览到期');
    await this.pruneSnapshots(restored?.snapshotPath);
    const plan = restored ? { kind: 'static', dir: restored.snapshotPath } : await resolveServerCommand(cwd, request);
    if (plan.kind !== 'static' && !this.allowQuickTunnels && !this.livePreviewOrigin) throw new Error('Live preview needs your own managed Cloudflare Tunnel. Export a static build, or configure live preview in computer settings.');
    const path = typeof request.path === 'string' ? request.path : '/';
    if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\') || new URL(path, 'http://preview').origin !== 'http://preview') throw new Error('预览 path 必须是站内绝对路径');
    const entry = { taskId, cwd, processes: [], servers: [], output: '', mode: plan.kind === 'static' ? 'snapshot' : 'live', revision: plan.kind === 'static' ? null : randomBytes(16).toString('hex') };
    this.active.set(taskId, entry);
    try {
      let port = Number(request.port) || null;
      if (plan.kind === 'static') {
        if (restored) { entry.snapshot = restored.snapshotPath; entry.revision = restored.revision; entry.createdAt = restored.createdAt; }
        else {
          await mkdir(this.snapshotDir, { recursive: true });
          entry.snapshot = await mkdtemp(join(this.snapshotDir, 'result-')); entry.createdSnapshot = true;
          const copied = await snapshot(plan.dir, entry.snapshot);
          entry.revision = copied.revision; entry.createdAt = Date.now();
          await writeFile(entry.snapshot + '.json', JSON.stringify({ ...copied, taskId, createdAt: entry.createdAt }));
          await this.pruneSnapshots();
        }
        port = await freePort(); const server = serveStatic(entry.snapshot, request.spa === true); entry.servers.push(server);
        await new Promise((resolve, reject) => { server.once('error', reject); server.listen(port, '127.0.0.1', resolve); });
      }
      else {
        if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error('请提供 dev server 监听的 port（1024–65535）');
        const probe = createServer();
        await new Promise((resolve, reject) => { probe.once('error', () => reject(new Error('预览端口已被占用，请选择其他 port：' + port))); probe.listen(port, '127.0.0.1', () => probe.close(resolve)); });
        const child = spawnOwned(plan.executable, plan.args, { cwd, windowsHide: true, env: { ...process.env, BROWSER: 'none', CI: '1', PORT: String(port), FORCE_COLOR: '0' }, shell: process.platform === 'win32' });
        child.stdout.on('data', chunk => { entry.output = (entry.output + chunk).slice(-3000); }); child.stderr.on('data', chunk => { entry.output = (entry.output + chunk).slice(-3000); });
        child.on('error', error => { entry.output += '\n' + error.message; });
        entry.processes.push(child);
        if (!await waitForPort(port, 90000, () => child.exitCode === null)) throw new Error(`端口 ${port} 在 90 秒内没有响应。服务器输出：${entry.output.trim().slice(-600) || '（无输出）'}`);
      }
      entry.localUrl = `http://127.0.0.1:${port}`;
      entry.path = path; entry.spa = request.spa === true;
      if (plan.kind === 'static' && this.publishSnapshot) {
        const published = await this.publishSnapshot(entry);
        if (!published.url || published.version !== entry.revision || published.expiresAt <= Date.now()) throw new Error('Relay did not confirm the exact snapshot version.');
        entry.url = published.url;
        this.renew(taskId);
        entry.expiresAt = published.expiresAt;
        return { ...this.get(taskId), minutes: this.minutes };
      }
      const token = randomBytes(12).toString('base64url');
      const gatePort = this.livePreviewOrigin ? this.livePreviewPort : await freePort();
      const gate = gatedProxy(port, token); entry.servers.push(gate); await new Promise((resolve, reject) => { gate.once('error', reject); gate.listen(gatePort, '127.0.0.1', resolve); });
      let tunnel;
      for (let attempt = 0; attempt < 2; attempt++) {
        if (this.livePreviewOrigin) {
          const origin = new URL(this.livePreviewOrigin);
          if (origin.protocol !== 'https:' || origin.pathname !== '/' || origin.search || origin.hash || !this.tunnelToken || !this.bin) throw new Error('Managed live preview configuration is incomplete.');
          const child = spawnOwned(this.bin, ['tunnel', '--no-autoupdate', 'run'], { windowsHide: true, env: { ...process.env, TUNNEL_TOKEN: this.tunnelToken }, stdio: 'ignore' });
          child.on('error', error => this.log('Managed tunnel: ' + error.message));
          tunnel = { child, url: origin.origin };
        } else {
          if (!this.allowQuickTunnels) throw new Error('Quick Tunnels are disabled for installed instances.');
          tunnel = await this.tunnel(gatePort, this.bin, child => entry.processes.push(child));
        }
        if (!entry.processes.includes(tunnel.child)) entry.processes.push(tunnel.child);
        const publicUrl = new URL(path, tunnel.url); publicUrl.searchParams.set('k', token);
        const alive = () => this.active.get(taskId) === entry && !entry.stopping && (tunnel.child.exitCode === undefined || tunnel.child.exitCode === null);
        try {
          await this.publicReady(publicUrl.href, token, alive);
          if (!alive()) throw new Error('预览启动已停止');
          entry.url = publicUrl.href;
          break;
        } catch (error) {
          await stopOwned(tunnel.child); entry.processes.pop();
          if (attempt === 1 || this.active.get(taskId) !== entry) throw error;
          this.log('公网预览未就绪，自动重建隧道一次：' + error.message);
        }
      }
      entry.path = path;
      if (entry.processes.some(child => child.exitCode !== undefined && child.exitCode !== null)) throw new Error('预览进程已退出');
      for (const child of entry.processes) child.once?.('exit', () => { if (this.active.get(taskId) === entry) this.stop(taskId, 'process-exited').catch(error => this.log(error.message)); });
      this.renew(taskId);
      this.log(`preview ${taskId} ${plan.label || plan.kind} -> ${tunnel.url} (${this.minutes} min)`);
      return { ...this.get(taskId), minutes: this.minutes };
    } catch (error) { await this.stop(taskId, 'startup-failed'); throw error; }
  }

  get(taskId) { const entry = this.active.get(taskId); return entry?.url && !entry.stopping ? { url: entry.url, localUrl: entry.localUrl, path: entry.path, expiresAt: entry.expiresAt, mode: entry.mode, revision: entry.revision || null, snapshotPath: entry.snapshot || null, createdAt: entry.createdAt || null } : null; }

  async restore(taskId, saved) {
    if (saved?.mode === 'live') {
      if (typeof saved.cwd !== 'string' || !saved.request) throw new Error('动态预览没有保存运行配置');
      return this.start(taskId, saved.cwd, saved.request);
    }
    if (saved?.mode !== 'snapshot' || typeof saved.snapshotPath !== 'string') throw new Error('没有可重开的预览快照');
    const path = resolve(saved.snapshotPath), base = resolve(this.snapshotDir);
    if (path === base || !inside(base, path) || !/^result-[A-Za-z0-9]+$/.test(relative(base, path))) throw new Error('预览快照路径无效');
    let metadata;
    try {
      if ((await lstat(path)).isSymbolicLink() || !inside(await realpath(base), await realpath(path))) throw new Error('invalid snapshot');
      metadata = JSON.parse(await readFile(path + '.json', 'utf8'));
    } catch { throw new Error('这份预览快照已清理，无法重开；报告截图仍可查看'); }
    if (metadata.taskId !== taskId || metadata.revision !== saved.revision) throw new Error('预览快照与任务版本不匹配');
    if (!Number.isFinite(metadata.createdAt) || metadata.createdAt < Date.now() - 7 * 86400000) { await this.pruneSnapshots(); throw new Error('预览快照已超过 7 天保留期'); }
    if ((await snapshot(path, null)).revision !== metadata.revision) throw new Error('预览快照内容已变化，无法作为原交付重开');
    return this.start(taskId, saved.cwd, { static: '.', path: saved.path || '/', spa: saved.request?.spa === true }, { snapshotPath: path, revision: metadata.revision, createdAt: metadata.createdAt });
  }

  async pruneSnapshots(protectedPath = null) {
    await mkdir(this.snapshotDir, { recursive: true });
    const retained = [], inUse = new Set([...this.active.values()].map(entry => entry.snapshot));
    if (protectedPath) inUse.add(protectedPath);
    for (const item of await readdir(this.snapshotDir, { withFileTypes: true })) {
      if (!item.isDirectory() || !/^result-[A-Za-z0-9]+$/.test(item.name)) continue;
      const path = resolve(this.snapshotDir, item.name);
      try {
        const metadata = JSON.parse(await readFile(path + '.json', 'utf8'));
        if (Number.isFinite(metadata.createdAt) && Number.isFinite(metadata.bytes) && metadata.bytes >= 0) retained.push({ ...metadata, path });
      } catch {}
    }
    retained.sort((a, b) => a.createdAt - b.createdAt);
    let bytes = retained.reduce((sum, item) => sum + item.bytes, 0);
    for (const item of retained) {
      if (inUse.has(item.path) || (item.createdAt >= Date.now() - 7 * 86400000 && bytes <= 1024 * 1024 * 1024)) continue;
      await this.removeSnapshot(item.path); bytes -= item.bytes;
    }
    if (bytes > 1024 * 1024 * 1024) throw new Error('活动预览快照已达到 1 GB；请等待现有预览关闭');
  }

  async removeSnapshot(path) {
    const target = resolve(path), base = resolve(this.snapshotDir);
    if (target === base || !inside(base, target) || !/^result-[A-Za-z0-9]+$/.test(relative(base, target))) throw new Error('拒绝清理预览目录以外的文件');
    if (existsSync(target) && ((await lstat(target)).isSymbolicLink() || !inside(await realpath(base), await realpath(target)))) throw new Error('拒绝清理指向预览目录以外的链接');
    await rm(target, { recursive: true, force: true, maxRetries: 3, retryDelay: 100 });
    await rm(target + '.json', { force: true });
  }

  renew(taskId) {
    const entry = this.active.get(taskId); if (!entry?.url || entry.stopping) return null;
    clearTimeout(entry.timer);
    entry.expiresAt = Date.now() + this.minutes * 60000;
    entry.timer = setTimeout(() => this.stop(taskId, 'expired').catch(error => this.log(error.message)), this.minutes * 60000); entry.timer.unref?.();
    return this.get(taskId);
  }

  async renewPublished(taskId) {
    const entry = this.active.get(taskId);
    if (!entry || entry.stopping) return null;
    if (!this.publishSnapshot || entry.mode !== 'snapshot') return this.renew(taskId);
    const published = await this.publishSnapshot(entry);
    if (!published.url || published.version !== entry.revision || published.expiresAt <= Date.now()) throw new Error('The Relay did not renew the delivered snapshot.');
    entry.url = published.url;
    this.renew(taskId);
    entry.expiresAt = published.expiresAt;
    return this.get(taskId);
  }

  async stop(taskId, reason = 'stopped') {
    const entry = this.active.get(taskId); if (!entry) return;
    if (entry.stopPromise) return entry.stopPromise;
    entry.stopping = true;
    clearTimeout(entry.timer);
    const stopping = entry.stopPromise = Promise.resolve().then(async () => {
      for (const server of entry.servers) { server.closeAllConnections?.(); await new Promise(r => server.close(r)); }
      for (const child of entry.processes) await stopOwned(child);
      if (reason === 'startup-failed' && entry.createdSnapshot) await this.removeSnapshot(entry.snapshot);
      this.active.delete(taskId);
      if (entry.url) await this.onStop({ taskId, url: entry.url, mode: entry.mode, revision: entry.revision || null, expiresAt: Date.now(), reason });
    });
    try { await stopping; } finally { if (entry.stopPromise === stopping) entry.stopPromise = null; }
  }

  async stopAll() { for (const id of [...this.active.keys()]) await this.stop(id); }
  async stopForCwd(cwd) { for (const [id, entry] of [...this.active]) if (entry.cwd === cwd) await this.stop(id); }
  async stopLiveForCwd(cwd) { for (const [id, entry] of [...this.active]) if (entry.cwd === cwd && entry.mode === 'live') await this.stop(id, 'project-changing'); }
}
