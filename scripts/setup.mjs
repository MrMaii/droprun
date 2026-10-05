import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { resolve, join } from 'node:path';
import { randomBytes } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { spawn } from 'node:child_process';
import { defaultDataDir, atomicJson, loadRuntimeConfig, protectSecret } from '../connector/config.mjs';
import { trustedLocalRequest, matchesLocalToken } from '../connector/local-boundary.mjs';
import { findBrowser } from '../connector/browser.mjs';
import { Codex } from '../connector/codex.mjs';
import { provision, setupState, command, redact, wranglerPath } from './setup-core.mjs';
import { installMediaTools } from './setup-tools.mjs';
import { stopConnector } from '../connector/shutdown.mjs';
export { stopConnector } from '../connector/shutdown.mjs';

const root = resolve(import.meta.dirname, '..');
export async function diagnoseCodex({ create = () => new Codex(), timeout = 10000, includeProjects = false } = {}) {
  let client, timer, stage = 'start';
  const result = { installed: false, signedIn: false, projectApi: false, projectCount: 0, ready: false, status: 'missing' };
  try {
    client = create();
    const inventory = await Promise.race([
      (async () => {
        await client.start(); result.installed = true;
        stage = 'account';
        const account = await client.call('account/read', {}, timeout);
        result.signedIn = !!account.account;
        stage = 'projects';
        const projects = [], cursors = new Set(); let cursor;
        do {
          const page = await client.call('project/list', { limit: 100, cursor }, timeout);
          if (!Array.isArray(page.data)) throw new Error('Unsupported project list');
          projects.push(...page.data); cursor = page.nextCursor;
          if (cursor && cursors.has(cursor)) throw new Error('Repeated project cursor');
          if (cursor) cursors.add(cursor);
        } while (cursor);
        const ready = result.signedIn || account.requiresOpenaiAuth === false;
        return { projectApi: true, projectCount: projects.length, ready, status: ready ? 'ready' : 'login-required',
          ...(includeProjects && ready ? { projects: projects.map(project => {
            const roots = (project.roots || []).map(root => ({ path: root.path, available: existsSync(root.path) }));
            return { id: project.id, name: project.name, roots, available: roots.some(root => root.available) };
          }) } : {}) };
      })(),
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error('diagnostic-timeout')), timeout); })
    ]);
    Object.assign(result, inventory);
  } catch (error) {
    result.ready = false;
    result.status = error.message === 'diagnostic-timeout' ? 'timeout' : error.code === 'ENOENT' ? 'missing' : stage === 'start' ? 'app-server-failed' : 'unsupported';
  } finally { clearTimeout(timer); client?.close(); }
  return result;
}

export async function diagnose(installRoot = root, dataDir = defaultDataDir(), { includeProjects = false } = {}) {
  let git = false;
  try { await command('git', ['--version'], { timeout: 10000 }); git = true; } catch {}
  const codexStatus = await diagnoseCodex({ includeProjects });
  let connection = null;
  try { const response = await fetch('http://127.0.0.1:47493', { signal: AbortSignal.timeout(1500) }); connection = await response.json(); } catch {}
  return { node: process.version, git, codex: codexStatus.ready, codexStatus, browser: !!findBrowser({}), wrangler: existsSync(wranglerPath(installRoot)), configured: existsSync(join(dataDir, 'config.json')), connection: connection ? { version: connection.version, online: connection.online, active: !!connection.activeTask, instanceId: connection.instanceId } : null, media: { ytDlp: existsSync(join(dataDir, 'tools/yt-dlp.exe')), ffmpeg: existsSync(join(dataDir, 'tools/ffmpeg.exe')), ffprobe: existsSync(join(dataDir, 'tools/ffprobe.exe')) } };
}

function openBrowser(url) {
  if (process.platform === 'win32') spawn('rundll32.exe', ['url.dll,FileProtocolHandler', url], { windowsHide: true, stdio: 'ignore' }).unref();
  else spawn(process.platform === 'darwin' ? 'open' : 'xdg-open', [url], { stdio: 'ignore' }).unref();
}

export async function serveSetup({ installRoot = root, dataDir = defaultDataDir(), port = 47494, open = true } = {}) {
  const sessionFile = join(dataDir, 'setup-session.json');
  if (open && process.platform === 'win32') {
    try {
      const saved = JSON.parse(await readFile(sessionFile, 'utf8'));
      if (Number.isInteger(saved.port) && saved.port > 0 && saved.port < 65536) {
        const savedToken = await protectSecret(saved.tokenProtected, true);
        const response = await fetch(`http://127.0.0.1:${saved.port}/api/status`, { headers: { 'X-DropRun-Setup': savedToken }, signal: AbortSignal.timeout(1500) });
        if (response.ok) { openBrowser(`http://127.0.0.1:${saved.port}/#${savedToken}`); return { server: null, port: saved.port, reused: true }; }
      }
    } catch {}
  }
  let listenPort = port;
  const token = randomBytes(32).toString('hex');
  const state = { busy: false, step: '', messages: [], error: null, accounts: [], completed: null };
  const push = event => { if (event.step) state.step = event.step; if (event.message) state.messages = [...state.messages, redact(event.message).slice(-1000)].slice(-12); };
  const json = (res, status, data) => { res.writeHead(status, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(data)); };
  const server = createServer(async (req, res) => {
    res.setHeader('Cache-Control', 'no-store'); res.setHeader('Referrer-Policy', 'no-referrer'); res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Security-Policy', "default-src 'self'; style-src 'self'; script-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'");
    if (!trustedLocalRequest(req, listenPort)) return json(res, 403, { error: 'Local requests only.' });
    const path = new URL(req.url, 'http://127.0.0.1').pathname;
    try {
      const assets = { '/': ['setup.html', 'text/html;charset=utf-8'], '/setup.css': ['setup.css', 'text/css'], '/setup-ui.js': ['setup-ui.js', 'text/javascript'] };
      if (req.method === 'GET' && assets[path]) { res.writeHead(200, { 'Content-Type': assets[path][1] }); return res.end(await readFile(join(installRoot, 'installer', assets[path][0]))); }
      if (!matchesLocalToken(req.headers['x-droprun-setup'], token)) return json(res, 403, { error: 'Open setup from the DropRun shortcut to continue.' });
      if (path === '/api/status' && req.method === 'GET') {
        const saved = await setupState(dataDir);
        return json(res, 200, { ...state, saved: saved ? { name: saved.name, accountId: saved.accountId, completed: saved.completed, relay: saved.relay } : null });
      }
      if (path === '/api/doctor' && req.method === 'GET') return json(res, 200, await diagnose(installRoot, dataDir, { includeProjects: true }));
      if (path === '/api/updates' && req.method === 'GET') {
        const current = JSON.parse(await readFile(join(installRoot, 'package.json'), 'utf8')).version;
        const response = await fetch('https://api.github.com/repos/MrMaii/droprun/releases?per_page=5', { headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'DropRun/' + current }, signal: AbortSignal.timeout(15000) });
        if (!response.ok) throw new Error('Release information is unavailable. Your installed version is unchanged.');
        const releases = await response.json(), latest = releases.find(item => !item.draft);
        return json(res, 200, { current, latest: latest ? { tag: latest.tag_name, prerelease: latest.prerelease, url: latest.html_url } : null, downloadUrl: 'https://github.com/MrMaii/droprun/releases' });
      }
      if (req.method !== 'POST' || !['/api/login', '/api/deploy', '/api/start', '/api/tools'].includes(path)) return json(res, 404, { error: 'Not found.' });
      if (state.busy) return json(res, 409, { error: 'The current step is still running.' });
      let body = '';
      for await (const chunk of req) { body += chunk; if (body.length > 10000) return json(res, 413, { error: 'Request too large.' }); }
      const input = JSON.parse(body || '{}');
      state.busy = true; state.error = null; state.messages = [];
      json(res, 202, { accepted: true });
      const work = async () => {
        if (path === '/api/login') {
          state.step = 'Cloudflare login';
          await command(process.execPath, [wranglerPath(installRoot), 'login'], { cwd: installRoot, onOutput: message => push({ message }), env: { ...process.env, WRANGLER_SEND_METRICS: 'false' } });
          const output = await command(process.execPath, [wranglerPath(installRoot), 'whoami'], { cwd: installRoot });
          state.accounts = [...new Set(output.match(/\b[a-f0-9]{32}\b/gi) || [])];
          state.step = 'Cloudflare connected';
        } else if (path === '/api/tools') {
          state.step = 'Installing upstream media tools';
          const paths = await installMediaTools(join(dataDir, 'tools'), push);
          const configFile = join(dataDir, 'config.json');
          if (existsSync(configFile)) await atomicJson(configFile, { ...JSON.parse(await readFile(configFile, 'utf8')), ...paths });
          state.step = 'Media tools ready';
        } else if (path === '/api/deploy') {
          const doctor = await diagnose(installRoot, dataDir);
          if (!doctor.codex) throw new Error('Codex is not ready (' + doctor.codexStatus.status + '). Sign in to a current Codex desktop app and check again.');
          if (!doctor.git || !doctor.browser) throw new Error('Install Git and Chrome or Edge before creating cloud resources.');
          state.completed = await provision({ root: installRoot, dataDir, input, progress: push });
          state.step = 'Relay ready — start your Connector';
        } else {
          state.step = 'Starting Connector';
          await command('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', join(installRoot, 'scripts/install-connector-service.ps1'), '-DataDir', dataDir, '-InstallRoot', installRoot], { cwd: installRoot });
          state.step = 'Connector started';
        }
      };
      work().catch(error => { state.error = redact(error.message); }).finally(() => { state.busy = false; });
    } catch (error) { if (!res.headersSent) json(res, 400, { error: redact(error.message) }); }
  });
  await new Promise((accept, reject) => { server.once('error', reject); server.listen(port, '127.0.0.1', accept); });
  listenPort = server.address().port;
  if (open && process.platform === 'win32') await atomicJson(sessionFile, { port: listenPort, tokenProtected: await protectSecret(token) });
  if (open) openBrowser(`http://127.0.0.1:${listenPort}/#${token}`);
  return { server, token, port: listenPort };
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const action = process.argv[2] || 'open';
  const get = name => { const index = process.argv.indexOf(name); return index < 0 ? undefined : process.argv[index + 1]; };
  const dataDir = resolve(get('--data-dir') || defaultDataDir());
  try {
    if (action === 'doctor') console.log(JSON.stringify(await diagnose(root, dataDir), null, 2));
    else if (action === 'login') await command(process.execPath, [wranglerPath(root), 'login'], { cwd: root, onOutput: value => process.stdout.write(redact(value)) });
    else if (action === 'deploy') console.log(JSON.stringify(await provision({ root, dataDir, input: { accountId: get('--account'), name: get('--name'), costAccepted: process.argv.includes('--accept-cloud-costs') }, progress: event => console.log(event.step ? event.step + ': ' + event.status : event.message) })));
    else if (action === 'stop') {
      const { config } = await loadRuntimeConfig(root, { ...process.env, DROPRUN_DATA_DIR: dataDir });
      await stopConnector(config);
    }
    else if (action === 'open' || action === 'serve') await serveSetup({ dataDir, open: action === 'open' });
    else throw new Error('Use setup.mjs open, doctor, login, or deploy --account ID --name NAME --accept-cloud-costs.');
  } catch (error) { console.error(redact(error.message)); process.exitCode = 1; }
}
