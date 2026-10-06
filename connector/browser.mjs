import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { mkdir, readFile, writeFile, mkdtemp, rm } from 'node:fs/promises';
import { join } from 'node:path';
import { createServer } from 'node:net';
import { matchesTargetRoute } from './routes.mjs';

/** Locates a Chromium-based browser for headless screenshots and cookie export. */
export function findBrowser(config = {}) {
  if (config.browser && existsSync(config.browser)) return config.browser;
  const roots = [process.env['PROGRAMFILES'], process.env['PROGRAMFILES(X86)'], process.env.LOCALAPPDATA].filter(Boolean);
  const candidates = roots.flatMap(root => [join(root, 'Google/Chrome/Application/chrome.exe'), join(root, 'Microsoft/Edge/Application/msedge.exe'), join(root, 'Chromium/Application/chrome.exe')]);
  candidates.push('/usr/bin/google-chrome', '/usr/bin/chromium', '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome');
  return candidates.find(path => existsSync(path)) || null;
}

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function connectSocket(url, timeout) {
  const socket = new WebSocket(url);
  try {
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => { socket.close(); reject(new Error('DevTools connection timed out')); }, timeout);
      socket.onopen = () => { clearTimeout(timer); resolve(); };
      socket.onerror = () => { clearTimeout(timer); reject(new Error('DevTools connection failed')); };
      socket.onclose = () => { clearTimeout(timer); reject(new Error('DevTools connection closed')); };
    });
    return socket;
  } catch (error) { socket.close(); throw error; }
}

export function freePort() {
  return new Promise((resolve, reject) => {
    const server = createServer();
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => { const { port } = server.address(); server.close(() => resolve(port)); });
  });
}

/** Minimal Chrome DevTools Protocol client over one page target. */
export class Page {
  constructor(socket) { this.socket = socket; this.serial = 0; this.pending = new Map(); this.listeners = []; }
  static async connect(url) {
    const socket = await connectSocket(url, 20000);
    const page = new Page(socket);
    socket.onmessage = event => {
      const message = JSON.parse(String(event.data));
      if (message.id !== undefined) { const entry = page.pending.get(message.id); if (entry) { page.pending.delete(message.id); clearTimeout(entry.timer); message.error ? entry.reject(new Error(message.error.message)) : entry.resolve(message.result); } }
      else for (const listener of page.listeners) listener(message);
    };
    socket.onclose = () => { for (const entry of page.pending.values()) { clearTimeout(entry.timer); entry.reject(new Error('DevTools connection closed')); } page.pending.clear(); };
    return page;
  }
  send(method, params = {}) {
    const id = ++this.serial;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => { this.pending.delete(id); reject(new Error(method + ' timed out')); }, 30000);
      this.pending.set(id, { resolve, reject, timer });
      try { this.socket.send(JSON.stringify({ id, method, params })); } catch (error) { clearTimeout(timer); this.pending.delete(id); reject(error); }
    });
  }
  waitFor(method, timeout = 20000) {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => { this.listeners = this.listeners.filter(l => l !== listener); reject(new Error(method + ' timed out')); }, timeout);
      const listener = message => { if (message.method === method) { clearTimeout(timer); this.listeners = this.listeners.filter(l => l !== listener); resolve(message.params); } };
      this.listeners.push(listener);
    });
  }
  close() { try { this.socket.close(); } catch {} }
}

/** Starts the browser with a dedicated profile and returns handles; the caller must close it. */
export async function launchBrowser({ executable, profileDir, headless = true, url = 'about:blank', width = 1280, height = 900 }) {
  await mkdir(profileDir, { recursive: true });
  const activePortFile = join(profileDir, 'DevToolsActivePort');
  await rm(activePortFile, { force: true });
  let port = null;
  const args = ['--remote-debugging-port=0', `--user-data-dir=${profileDir}`, '--no-first-run', '--no-default-browser-check', '--disable-background-networking', '--disable-sync', '--disable-extensions', `--window-size=${width},${height}`];
  if (headless) args.push('--headless=new', '--disable-gpu', '--hide-scrollbars');
  args.push(url);
  const child = spawn(executable, args, { windowsHide: headless, stdio: ['ignore', 'ignore', 'pipe'] });
  let diagnostics = '';
  child.stderr.on('data', chunk => { diagnostics = (diagnostics + chunk).slice(-2000); });
  let failed = null;
  child.on('error', error => { failed = error; });
  const deadline = Date.now() + 20000;
  let socket = null;
  while (Date.now() < deadline) {
    try {
      const lines = (await readFile(activePortFile, 'utf8')).trim().split(/\r?\n/);
      if (lines.length === 2 && /^[1-9]\d{0,4}$/.test(lines[0]) && Number(lines[0]) <= 65535 && /^\/devtools\/browser\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(lines[1])) {
        port = Number(lines[0]);
        const expectedSocket = `ws://127.0.0.1:${port}${lines[1]}`;
        const version = await (await fetch(`http://127.0.0.1:${port}/json/version`, { signal: AbortSignal.timeout(Math.max(1, Math.min(2000, deadline - Date.now()))) })).json();
        if (version.webSocketDebuggerUrl === expectedSocket) {
          const response = await fetch(`http://127.0.0.1:${port}/json`, { signal: AbortSignal.timeout(Math.max(1, Math.min(2000, deadline - Date.now()))) });
          if (response.ok && (await response.json()).some(t => t.type === 'page') && Date.now() < deadline) { socket = await connectSocket(expectedSocket, deadline - Date.now()); break; }
        }
      }
    } catch {}
    if (failed) throw new Error('浏览器无法启动：' + failed.message);
    if (child.exitCode !== null && child.exitCode !== 0) throw new Error('浏览器启动后立即退出（' + child.exitCode + '）' + diagnostics.trim().split('\n').at(-1));
    await sleep(Math.max(0, Math.min(300, deadline - Date.now())));
  }
  if (!socket) { child.kill(); throw new Error('浏览器没有在 20 秒内响应 DevTools'); }
  let closed = socket.readyState === WebSocket.CLOSED, closing;
  socket.onclose = () => { closed = true; };
  const close = () => closing ??= (async () => {
    try {
      if (socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify({ id: 1, method: 'Browser.close' }));
      const deadline = Date.now() + 3000;
      while (Date.now() < deadline) {
        try { await fetch(`http://127.0.0.1:${port}/json/version`, { signal: AbortSignal.timeout(Math.max(1, Math.min(200, deadline - Date.now()))) }); }
        catch (error) { if (error.cause?.code === 'ECONNREFUSED') return; }
        await sleep(Math.max(0, Math.min(25, deadline - Date.now())));
      }
      throw new Error('浏览器关闭后 DevTools 仍未停止；保留专用配置目录供检查。');
    } finally { socket.close(); }
  })().catch(error => { closing = null; throw error; });
  return { child, port, get closed() { return closed; }, close };
}

export async function firstPage(port) {
  const targets = await (await fetch(`http://127.0.0.1:${port}/json`, { signal: AbortSignal.timeout(3000) })).json();
  const target = targets.find(t => t.type === 'page');
  if (!target) throw new Error('浏览器没有可用页面');
  return Page.connect(target.webSocketDebuggerUrl);
}

/** Screenshot of a URL as PNG bytes; waits for load plus a settle delay so animations and fonts finish. */
export async function screenshot({ executable, profileDir, url, width = 1280, height = 900, wait = 2500, fullPage = false }) {
  const browser = await launchBrowser({ executable, profileDir, headless: true, width, height });
  try {
    const page = await firstPage(browser.port);
    try {
      await page.send('Page.enable');
      await page.send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
      const loaded = page.waitFor('Page.loadEventFired', 30000);
      await page.send('Page.navigate', { url });
      await loaded.catch(() => {});
      await sleep(wait);
      let clip;
      if (fullPage) {
        const { contentSize } = await page.send('Page.getLayoutMetrics');
        clip = { x: 0, y: 0, width: Math.min(contentSize.width, 4000), height: Math.min(contentSize.height, 8000), scale: 1 };
      }
      const result = await page.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: !!fullPage, ...(clip ? { clip } : {}) });
      return Buffer.from(result.data, 'base64');
    } finally { page.close(); }
  } finally { await browser.close(); }
}

const visualStyles = new Set(['opacity', 'transform', 'background-color', 'color', 'display', 'visibility', 'border-radius', 'box-shadow', 'position', 'width', 'height']);
const bounded = (value, fallback, min, max) => Number.isFinite(Number(value)) ? Math.max(min, Math.min(Number(value), max)) : fallback;

async function evaluate(page, expression) {
  const result = await page.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
  return result.result.value;
}

/** Only structured local-page actions are accepted; no caller-provided JavaScript is executed. */
export async function verifyVisual({ executable, profileDir, url, expectedPath, steps = [], width = 1280, height = 900, wait = 1000, fullPage = false }) {
  const target = new URL(url);
  if (target.protocol !== 'http:' || !['127.0.0.1', 'localhost', '[::1]'].includes(target.hostname) || target.username || target.password || target.port === '47493') throw new Error('视觉验证只接受本机预览 HTTP 地址');
  if (!Array.isArray(steps) || steps.length > 12) throw new Error('视觉验证最多接受 12 个动作');
  for (const step of steps) {
    if (!step || !['hover', 'click', 'scroll', 'wait', 'assert'].includes(step.action)) throw new Error('不支持的视觉验证动作');
    if (['hover', 'click', 'assert'].includes(step.action) && (typeof step.selector !== 'string' || !step.selector || step.selector.length > 500)) throw new Error('动作需要 CSS selector');
    if (step.computedStyle && (typeof step.computedStyle !== 'object' || Array.isArray(step.computedStyle) || Object.keys(step.computedStyle).some(key => !visualStyles.has(key)))) throw new Error('computedStyle 只接受有限的视觉样式字段');
    if (step.action === 'assert' && !(typeof step.text === 'string' && step.text.trim()) && !Object.keys(step.computedStyle || {}).length) throw new Error('assert 需要非空 text 或 computedStyle 效果断言');
  }
  width = Math.round(bounded(width, 1280, 320, 2560)); height = Math.round(bounded(height, 900, 320, 2560));
  const result = { passed: false, url, title: '', path: '', status: null, width, height, errors: [], steps: [], screenshots: [], startedAt: Date.now() };
  await mkdir(profileDir, { recursive: true });
  const profile = await mkdtemp(join(profileDir, 'verify-'));
  let browser, page;
  const error = message => { if (result.errors.length < 30 && !result.errors.includes(message)) result.errors.push(message); };
  try {
    browser = await launchBrowser({ executable, profileDir: profile, headless: true, width, height });
    page = await firstPage(browser.port);
    let mainFrame;
    page.listeners.push(message => {
      const p = message.params;
      if (message.method === 'Network.responseReceived') {
        if (p.type === 'Document' && p.frameId === mainFrame) result.status = p.response.status;
        if (p.response.status >= 400 && !/\/favicon\.ico(?:\?|$)/.test(p.response.url)) error('HTTP ' + p.response.status + ': ' + p.response.url);
      }
      if (message.method === 'Network.loadingFailed' && !p.canceled && p.type !== 'Other') error('加载失败：' + p.errorText);
      if (message.method === 'Runtime.exceptionThrown') error('页面脚本错误：' + (p.exceptionDetails.exception?.description || p.exceptionDetails.text).slice(0, 500));
      if (message.method === 'Runtime.consoleAPICalled' && p.type === 'error') error('页面控制台错误：' + p.args.map(arg => arg.value ?? arg.description ?? '').join(' ').slice(0, 500));
      if (message.method === 'Page.javascriptDialogOpening') page.send('Page.handleJavaScriptDialog', { accept: false }).catch(() => {});
      if (message.method === 'Fetch.requestPaused') {
        const requestUrl = new URL(p.request.url), localDocument = p.resourceType !== 'Document' || requestUrl.origin === target.origin;
        const readOnly = ['GET', 'HEAD', 'OPTIONS'].includes(p.request.method);
        const allowed = localDocument && readOnly;
        if (!allowed) error('已阻止验证中的对外导航或写入请求：' + p.request.method + ' ' + requestUrl.origin + requestUrl.pathname);
        page.send(allowed ? 'Fetch.continueRequest' : 'Fetch.failRequest', { requestId: p.requestId, ...(allowed ? {} : { errorReason: 'BlockedByClient' }) }).catch(() => {});
      }
    });
    await page.send('Page.enable'); await page.send('Runtime.enable'); await page.send('Network.enable');
    await page.send('Fetch.enable', { patterns: [{ urlPattern: '*' }] });
    await page.send('Browser.setDownloadBehavior', { behavior: 'deny' });
    await page.send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
    mainFrame = (await page.send('Page.getFrameTree')).frameTree.frame.id;
    const loaded = page.waitFor('Page.loadEventFired', 30000).then(() => null, e => e);
    const navigation = await page.send('Page.navigate', { url });
    if (navigation.errorText) throw new Error('页面无法加载：' + navigation.errorText);
    const loadError = await loaded; if (loadError) throw loadError;
    await sleep(bounded(wait, 1000, 0, 5000));
    await evaluate(page, 'document.fonts.ready.then(() => true)');
    const describe = () => evaluate(page, '({url:location.href,title:document.title,path:location.pathname,route:location.pathname+location.search+location.hash,scrollX,scrollY})');
    async function capture(name, action) {
      const state = await describe();
      if (new URL(state.url).origin !== target.origin) throw new Error('页面离开了本机预览');
      let clip;
      if (fullPage) { const { contentSize } = await page.send('Page.getLayoutMetrics'); clip = { x: 0, y: 0, width: Math.min(contentSize.width, 4000), height: Math.min(contentSize.height, 8000), scale: 1 }; }
      const shot = await page.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: !!fullPage, ...(clip ? { clip } : {}) });
      result.screenshots.push({ name, png: Buffer.from(shot.data, 'base64'), ...state, action, capturedAt: Date.now() });
      return state;
    }
    const initial = await capture('initial', 'load'); Object.assign(result, initial);
    if (expectedPath && !matchesTargetRoute(initial.url, expectedPath, target.origin)) throw new Error('目标页不匹配：期望 ' + expectedPath + '，实际 ' + initial.route);
    if (result.status === null || result.status >= 400) throw new Error('目标页未成功加载：HTTP ' + result.status);
    for (let index = 0; index < steps.length; index++) {
      const step = steps[index], record = { action: step.action, selector: step.selector || null, label: String(step.label || step.action).slice(0, 100), passed: false };
      for (const field of ['text', 'computedStyle', 'wait', 'x', 'y']) if (step[field] !== undefined) record[field] = step[field];
      record.wait = bounded(step.wait, 400, 0, 5000);
      result.steps.push(record);
      try {
        let element;
        if (step.selector) {
          element = await evaluate(page, `(() => { const elements = document.querySelectorAll(${JSON.stringify(step.selector)}); if (elements.length !== 1) throw new Error('selector 必须匹配唯一元素，实际 ' + elements.length); const e = elements[0]; ${['click', 'hover'].includes(step.action) ? "e.scrollIntoView({block:'center',inline:'center',behavior:'instant'});" : ''} const r = e.getBoundingClientRect(), style = getComputedStyle(e); return {x:r.x+r.width/2,y:r.y+r.height/2,width:r.width,height:r.height,text:e.textContent.slice(0,2000),visible:!!(r.width&&r.height)&&style.visibility!=='hidden'&&style.display!=='none'&&Number(style.opacity)>0,style:Object.fromEntries(${JSON.stringify([...visualStyles])}.map(key=>[key,style.getPropertyValue(key)]))}; })()`);
          record.element = element;
          const expectsHidden = step.action === 'assert' && step.text === undefined && Object.entries(step.computedStyle || {}).some(([key, value]) => (key === 'display' && value === 'none') || (key === 'visibility' && value === 'hidden') || (key === 'opacity' && String(value) === '0'));
          if (!element.visible && !expectsHidden) throw new Error('目标元素不可见');
        }
        if (step.action === 'hover' || step.action === 'click') {
          const covered = await evaluate(page, `(() => { const e=document.querySelector(${JSON.stringify(step.selector)}), hit=document.elementFromPoint(${element.x},${element.y}); return !(hit && (hit === e || e.contains(hit))); })()`);
          if (covered) throw new Error('目标元素被遮挡或不在视口内');
          await page.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: element.x, y: element.y });
          if (step.action === 'click') {
            await page.send('Input.dispatchMouseEvent', { type: 'mousePressed', x: element.x, y: element.y, button: 'left', buttons: 1, clickCount: 1 });
            await page.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: element.x, y: element.y, button: 'left', buttons: 0, clickCount: 1 });
          }
        } else if (step.action === 'scroll') {
          const x = bounded(step.x, 0, -10000, 10000), y = bounded(step.y, 600, -10000, 10000);
          record.x = x; record.y = y;
          await page.send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: element?.x ?? width / 2, y: element?.y ?? height / 2, deltaX: x, deltaY: y });
        } else if (step.action === 'assert') {
          if (step.text !== undefined && !element.text.includes(String(step.text))) throw new Error('目标文本不包含：' + step.text);
          for (const [key, value] of Object.entries(step.computedStyle || {})) if (element.style[key] !== String(value)) throw new Error('样式断言失败：' + key + ' 期望 ' + value + '，实际 ' + element.style[key]);
        }
        await sleep(record.wait);
        record.state = await capture('step-' + (index + 1) + '-' + step.action, step.action);
        record.passed = true;
      } catch (e) { record.error = e.message; throw e; }
    }
    result.passed = result.errors.length === 0 && result.steps.every(step => step.passed);
  } catch (e) { error(e.message); }
  finally {
    page?.close();
    if (browser) {
      try {
        await browser.close();
        await rm(profile, { recursive: true, force: true, maxRetries: 5, retryDelay: 100 }).catch(() => {});
      } catch (e) { error(e.message); result.passed = false; }
    }
    result.finishedAt = Date.now();
  }
  return result;
}

/** Exports cookies for the given domains from the dedicated profile in Netscape format (what yt-dlp reads). */
export async function exportCookies({ executable, profileDir, domains, file, headless = true, port = null }) {
  let browser = null;
  if (port == null) { browser = await launchBrowser({ executable, profileDir, headless }); port = browser.port; }
  try {
    const page = await firstPage(port);
    try {
      await page.send('Network.enable');
      const { cookies } = await page.send('Network.getAllCookies');
      const wanted = cookies.filter(cookie => domains.some(domain => cookie.domain === domain || cookie.domain.endsWith('.' + domain) || cookie.domain === '.' + domain));
      const lines = ['# Netscape HTTP Cookie File', '# Exported by DropRun from its own browser profile; do not share.'];
      for (const cookie of wanted) lines.push([cookie.domain, cookie.domain.startsWith('.') ? 'TRUE' : 'FALSE', cookie.path || '/', cookie.secure ? 'TRUE' : 'FALSE', String(cookie.expires > 0 ? Math.floor(cookie.expires) : 0), cookie.name, cookie.value].join('\t'));
      await writeFile(file, lines.join('\n') + '\n', 'utf8');
      return { count: wanted.length, loggedIn: wanted.some(cookie => ['sessionid', 'ds_user_id', 'auth_token', 'SID', 'SESSDATA'].includes(cookie.name)) };
    } finally { page.close(); }
  } finally { await browser?.close(); }
}
