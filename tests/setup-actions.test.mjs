import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { mkdtemp, readFile, rm, mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { findBrowser, launchBrowser, firstPage } from '../connector/browser.mjs';

const executable = findBrowser(), options = { skip: !executable, timeout: 45000 };
const gate = () => { let release; const promise = new Promise(resolve => { release = resolve; }); return { promise, release }; };
async function fixture(t) {
  const profileDir = await mkdtemp(join(tmpdir(), 'droprun-actions-'));
  const queues = new Map(), calls = [], holds = [];
  const state = { step: '', messages: [], accounts: [], busy: false, error: null };
  const queue = (path, value) => { if (!queues.has(path)) queues.set(path, []); queues.get(path).push(value); if (value.hold) holds.push(value.hold); };
  const server = createServer(async (req, res) => {
    calls.push({ path: req.url, method: req.method });
    const assets = { '/': ['setup.html', 'text/html'], '/setup.css': ['setup.css', 'text/css'], '/setup-ui.js': ['setup-ui.js', 'text/javascript'] };
    if (assets[req.url]) { res.setHeader('Content-Type', assets[req.url][1]); return res.end(await readFile(new URL('../installer/' + assets[req.url][0], import.meta.url))); }
    res.setHeader('Content-Type', 'application/json');
    if (req.headers['x-droprun-setup'] !== 'local-actions-fixture') { res.writeHead(403); return res.end('{}'); }
    const reply = queues.get(req.url)?.shift();
    const payload = JSON.stringify(reply?.body ?? (req.url === '/api/status' ? state : req.url === '/api/doctor' ? { git: true, codex: true, browser: true, wrangler: true, media: {}, codexStatus: { ready: true, projectCount: 0, projects: [] } } : { accepted: true }));
    if (reply?.hold) await reply.hold.promise;
    res.writeHead(reply?.code ?? 200); res.end(payload);
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser, page;
  t.after(async () => {
    holds.forEach(hold => hold.release()); page?.close(); browser?.close(); server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
    await rm(profileDir, { recursive: true, force: true, maxRetries: 20, retryDelay: 100 });
  });
  browser = await launchBrowser({ executable, profileDir }); page = await firstPage(browser.port);
  await page.send('Page.enable');
  // Drive polling explicitly so delayed-response ordering is deterministic.
  await page.send('Page.addScriptToEvaluateOnNewDocument', { source: 'window.setInterval=callback=>{window.pollSetup=callback;return 0;};' });
  const run = async expression => {
    const result = await page.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
    return result.result.value;
  };
  const until = expression => run(`(async()=>{const end=Date.now()+5000;while(!(${expression})){if(Date.now()>end)throw Error('UI state timeout');await new Promise(r=>setTimeout(r,20));}})()`);
  await page.send('Page.navigate', { url: `http://127.0.0.1:${server.address().port}/#local-actions-fixture` });
  await until('document.querySelector("#doctor-note")?.textContent.includes("0")');
  await run('if(document.documentElement.lang!=="en")document.getElementById("language").click();refresh()');
  const capture = async (name, selector) => {
    if (!process.env.DROPRUN_SETUP_ACTION_SCREENSHOTS) return;
    const dir = resolve(process.env.DROPRUN_SETUP_ACTION_SCREENSHOTS); await mkdir(dir, { recursive: true });
    await run(`document.querySelector(${JSON.stringify(selector)}).scrollIntoView({block:'center'})`);
    const shot = await page.send('Page.captureScreenshot', { format: 'png' });
    await writeFile(join(dir, name + '.png'), Buffer.from(shot.data, 'base64'));
  };
  return { state, calls, queue, run, until, capture };
}
const allDisabled = '["login","deploy","start","tools"].every(id=>document.getElementById(id).disabled)';

test('setup locks all mutations before acknowledgement and keeps errors across polls', options, async t => {
  const f = await fixture(t), hold = gate();
  f.queue('/api/tools', { hold, code: 400, body: { error: 'Synthetic checksum mismatch. Try again.' } });
  await f.run('document.getElementById("tools").click()');
  assert.equal(await f.run(allDisabled), true);
  assert.match(await f.run('document.getElementById("tools").textContent'), /…/);
  await f.capture('tools-waiting', '#tools');
  await f.run('document.getElementById("tools").click();document.getElementById("login").click()');
  hold.release();
  await f.until('document.getElementById("error").textContent.includes("checksum")');
  await f.run('refresh()');
  assert.match(await f.run('document.getElementById("error").textContent'), /checksum mismatch/);
  assert.equal(await f.run('document.getElementById("error").previousElementSibling.id'), 'tools');
  assert.equal(await f.run('document.getElementById("error").hidden'), false);
  f.queue('/api/status', { code: 503, body: { error: 'Synthetic offline state' } });
  await f.run('refresh()');
  assert.match(await f.run('document.getElementById("error").textContent'), /checksum mismatch.*offline state/);
  await f.capture('action-error-and-offline', '#error');
  await f.run('refresh()');
  assert.equal(f.calls.filter(call => call.path === '/api/tools').length, 1);
  assert.equal(f.calls.filter(call => call.path === '/api/login').length, 0);
  f.state.busy = true; f.state.step = 'Installing upstream media tools';
  await f.run('act("tools")');
  assert.equal(await f.run(allDisabled), true);
  assert.equal(await f.run('document.getElementById("error").textContent'), '');
  assert.equal(await f.run('document.getElementById("error").hidden'), true);
  await f.run('document.getElementById("language").click()');
  assert.match(await f.run('document.getElementById("tools").textContent'), /安装中/);
  f.state.busy = false; f.state.step = 'Media tools ready';
  await f.run('refresh()');
  assert.equal(await f.run('document.getElementById("tools").disabled'), false);
  assert.equal(await f.run('document.getElementById("tools").textContent'), '安装媒体工具');
});

test('late status responses cannot unlock accepted work; unknown status prevents another mutation', options, async t => {
  const f = await fixture(t), hold = gate();
  f.queue('/api/status', { hold, body: { ...f.state } });
  await f.run('window.oldPoll=refresh();void 0');
  f.state.busy = true; f.state.step = 'Starting Connector';
  await f.run('act("start")');
  assert.equal(await f.run(allDisabled), true);
  hold.release(); await f.run('oldPoll');
  assert.equal(await f.run(allDisabled), true);
  f.queue('/api/status', { code: 503, body: { error: 'Synthetic connection loss' } });
  await f.run('refresh()');
  assert.equal(await f.run(allDisabled), true);
  assert.match(await f.run('document.getElementById("error").textContent'), /connection loss/);
  f.state.busy = false; await f.run('refresh()');
  assert.equal(await f.run('document.getElementById("start").disabled'), false);
  assert.equal(await f.run('document.getElementById("error").textContent'), '');
  f.queue('/api/status', { code: 503, body: { error: 'Synthetic offline while idle' } });
  await f.run('refresh();void 0');
  await f.until('document.getElementById("error").textContent.includes("offline")');
  assert.equal(await f.run(allDisabled), true);
  await f.run('act("tools")');
  assert.equal(f.calls.filter(call => call.method === 'POST').length, 1);
  await f.run('refresh()');
  assert.equal(await f.run('document.getElementById("tools").disabled'), false);
});

test('lost acknowledgement never retries a mutation and slow polls are not overtaken by timer reads', options, async t => {
  const f = await fixture(t), hold = gate();
  f.queue('/api/status', { hold });
  const before = f.calls.filter(call => call.path === '/api/status').length;
  await f.run('window.pendingPoll=refresh();void 0');
  await f.run('pollSetup();pollSetup()');
  hold.release(); await f.run('pendingPoll');
  assert.equal(f.calls.filter(call => call.path === '/api/status').length, before + 1);
  f.state.busy = true;
  f.queue('/api/login', { code: 503, body: { error: 'Synthetic acknowledgement lost' } });
  await f.run('act("login")');
  assert.equal(await f.run(allDisabled), true);
  assert.match(await f.run('document.getElementById("error").textContent'), /acknowledgement lost/);
  await f.run('refresh();void 0');
  await f.run('act("login")');
  assert.equal(f.calls.filter(call => call.method === 'POST').length, 1);
});

test('release checks show waiting and retry states and retain localized results', options, async t => {
  const f = await fixture(t), hold = gate();
  f.queue('/api/updates', { hold, code: 503, body: { error: 'Synthetic release lookup failed' } });
  await f.run('document.getElementById("updates").click()');
  assert.equal(await f.run('document.getElementById("updates").disabled'), true);
  assert.match(await f.run('document.getElementById("release").textContent'), /Checking/);
  await f.run('document.getElementById("language").click()');
  assert.match(await f.run('document.getElementById("updates").textContent'), /检查中/);
  await f.capture('release-waiting-zh', '#updates');
  await f.run('document.getElementById("language").click()');
  await f.run('document.getElementById("updates").click()'); hold.release();
  await f.until('!document.getElementById("updates").disabled');
  assert.match(await f.run('document.getElementById("release").textContent'), /lookup failed/);
  assert.equal(f.calls.filter(call => call.path === '/api/updates').length, 1);
  f.queue('/api/updates', { body: { current: '0.5.1', latest: { tag: 'v0.5.2' }, downloadUrl: 'https://github.com/MrMaii/droprun/releases' } });
  await f.run('document.getElementById("updates").click()');
  await f.until('!!document.querySelector("#release a")');
  await f.run('document.getElementById("language").click()');
  assert.match(await f.run('document.getElementById("release").textContent'), /0.5.1.*v0.5.2/);
  assert.match(await f.run('document.querySelector("#release a").textContent'), /发布说明与下载/);
  assert.equal(f.calls.filter(call => call.method === 'POST').length, 0);
});
