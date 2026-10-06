import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { mkdtemp, rm, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { findBrowser, verifyVisual, launchBrowser, firstPage, exportCookies } from '../connector/browser.mjs';

const executable = findBrowser();
const demo = `<!doctype html><title>Portfolio effects</title><link rel="icon" href="data:,">
<style>body{margin:0;min-height:2400px}#card{width:200px;height:100px;background:rgb(10,20,30);transition:background-color .05s}#card:hover{background:rgb(220,30,40);box-shadow:0 10px 20px rgba(0,0,0,.5)}#toggle{margin-top:30px}#result{color:black}</style>
<main><div id="card">Hover card</div><button id="toggle" onclick="document.querySelector('#result').textContent='Expanded'">Open</button><p id="result">Closed</p><div id="hidden" style="display:none">Hidden details</div><p id="transparent" style="opacity:0">Invisible text</p><a id="external" href="https://example.com">Leave preview</a><button id="write" onclick="fetch('/write',{method:'POST'})">Write</button></main>`;

async function fixture(t) {
  const profileDir = await mkdtemp(join(tmpdir(), 'droprun-visual-test-'));
  const calls = [];
  const server = createServer((request, response) => {
    calls.push({ method: request.method, url: request.url });
    response.setHeader('content-type', 'text/html');
    if (request.url === '/missing') { response.writeHead(404); return response.end('<h1>Missing</h1>'); }
    response.end(demo);
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(async () => { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); await rm(profileDir, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 }); });
  return { executable, profileDir, url: `http://127.0.0.1:${server.address().port}/portfolio`, expectedPath: '/portfolio', wait: 20, calls };
}

async function endpointClosed(port) {
  const deadline = Date.now() + 3000;
  while (Date.now() < deadline) {
    try { await fetch(`http://127.0.0.1:${port}/json/version`, { signal: AbortSignal.timeout(200) }); }
    catch (error) { if (error.cause?.code === 'ECONNREFUSED') return true; }
    await new Promise(resolve => setTimeout(resolve, 25));
  }
  return false;
}

async function lifetimeFixture(t) {
  const profileDir = await mkdtemp(join(tmpdir(), 'droprun-lifetime-test-'));
  const server = createServer((request, response) => { response.setHeader('content-type', 'text/html'); response.end(demo); });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  t.diagnostic(JSON.stringify({ profileDir, purpose: 'Fresh real-browser lifecycle fixture; no external login.' }));
  t.after(async () => {
    try {
      if (!browser) { t.diagnostic('Startup failed; preserve the dedicated profile for cleanup diagnosis.'); return; }
      await browser.close();
      assert.equal(await endpointClosed(browser.port), true, 'dedicated CDP endpoint stopped before removing its profile');
      await rm(profileDir, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 });
    } finally { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
  });
  browser = await launchBrowser({ executable, profileDir, width: 640, height: 480 });
  t.diagnostic(JSON.stringify({ port: browser.port, launcherPid: browser.child.pid }));
  return { browser, profileDir, url: `http://127.0.0.1:${server.address().port}/` };
}

test('real browser lifetime supports a screenshot and repeated dedicated endpoint closure', { skip: !executable, timeout: 45000 }, async t => {
  const { browser, url } = await lifetimeFixture(t);
  assert.equal(browser.closed, false, 'live browser remains available independently of its initial launcher');
  const page = await firstPage(browser.port);
  try {
    await page.send('Page.enable');
    const loaded = page.waitFor('Page.loadEventFired');
    await page.send('Page.navigate', { url }); await loaded;
    const shot = await page.send('Page.captureScreenshot', { format: 'png' });
    assert.equal(Buffer.from(shot.data, 'base64').subarray(1, 4).toString(), 'PNG');
    await browser.close();
    assert.equal(await endpointClosed(browser.port), true);
    assert.equal(browser.closed, true);
    await browser.close();
  } finally { page.close(); }
});

test('borrowed cookie port keeps its empty-profile browser alive until the owner closes it', { skip: !executable, timeout: 45000 }, async t => {
  const { browser, profileDir } = await lifetimeFixture(t);
  const file = join(profileDir, 'empty-cookies.txt');
  const result = await exportCookies({ executable, profileDir, domains: ['example.invalid'], file, port: browser.port });
  assert.equal(result.count, 0); assert.equal(result.loggedIn, false);
  assert.match(await readFile(file, 'utf8'), /^# Netscape HTTP Cookie File/);
  assert.equal(browser.closed, false, 'borrower does not close the owner browser');
  const page = await firstPage(browser.port);
  try {
    await page.send('Browser.close');
    const deadline = Date.now() + 3000;
    while (!browser.closed && Date.now() < deadline) await new Promise(resolve => setTimeout(resolve, 25));
    assert.equal(browser.closed, true, 'browser-level close event follows a user closing the window');
    await browser.close();
    assert.equal(await endpointClosed(browser.port), true);
  } finally { page.close(); }
});

test('nonzero browser launcher exit still fails before the startup deadline', { timeout: 10000 }, async t => {
  const profileDir = await mkdtemp(join(tmpdir(), 'droprun-nonzero-launch-test-'));
  t.after(() => rm(profileDir, { recursive: true, force: true }));
  await assert.rejects(launchBrowser({ executable: process.execPath, profileDir }), /浏览器启动后立即退出（[1-9]\d*）/);
});

test('closed owner refuses a reused port and recovers after the replacement endpoint stops', { skip: !executable, timeout: 45000 }, async t => {
  const { browser } = await lifetimeFixture(t);
  const page = await firstPage(browser.port), calls = [];
  const replacement = createServer((request, response) => { calls.push({ method: request.method, url: request.url }); response.setHeader('content-type', 'application/json'); response.end('{"fixture":"replacement endpoint, not the owner browser"}'); });
  try {
    await page.send('Browser.close');
    const deadline = Date.now() + 3000;
    while (!browser.closed && Date.now() < deadline) await new Promise(resolve => setTimeout(resolve, 25));
    assert.equal(browser.closed, true); assert.equal(await endpointClosed(browser.port), true);
    await new Promise((resolve, reject) => { replacement.once('error', reject); replacement.listen(browser.port, '127.0.0.1', resolve); });
    await assert.rejects(browser.close(), /DevTools 仍未停止/);
    assert.equal(replacement.listening, true, 'owner cleanup does not stop an unrelated endpoint reusing its port');
    assert.ok(calls.length > 0); assert.ok(calls.every(call => call.method === 'GET' && call.url === '/json/version'));
    replacement.closeAllConnections(); await new Promise(resolve => replacement.close(resolve));
    await browser.close(); assert.equal(await endpointClosed(browser.port), true, 'failed cleanup can recheck the closed original endpoint');
  } finally {
    page.close();
    if (replacement.listening) { replacement.closeAllConnections(); await new Promise(resolve => replacement.close(resolve)); }
  }
});

test('visual verification rejects remote targets and arbitrary evaluation actions before browser launch', async () => {
  await assert.rejects(verifyVisual({ url: 'https://example.com' }), /本机/);
  await assert.rejects(verifyVisual({ url: 'http://127.0.0.1:47493/' }), /本机/);
  await assert.rejects(verifyVisual({ url: 'http://localhost:3000/', steps: [{ action: 'evaluate', script: 'alert(1)' }] }), /动作/);
  await assert.rejects(verifyVisual({ url: 'http://localhost:3000/', steps: [{ action: 'assert', selector: 'body', computedStyle: { secret: 'x' } }] }), /样式/);
  await assert.rejects(verifyVisual({ url: 'http://localhost:3000/', steps: [{ action: 'assert', selector: 'body' }] }), /效果断言/);
  await assert.rejects(verifyVisual({ url: 'http://localhost:3000/', steps: [{ action: 'assert', selector: 'body', text: ' ', computedStyle: {} }] }), /效果断言/);
});

// Cover the existing 20s cold-start and 30s load budgets, six captures and cleanup.
test('real browser verifies target route, hover transition, click state and scrolling with screenshots', { skip: !executable, timeout: 90000 }, async t => {
  const fixtureData = await fixture(t);
  const result = await verifyVisual({ ...fixtureData, steps: [
    { action: 'hover', selector: '#card', wait: 150 },
    { action: 'assert', selector: '#card', computedStyle: { 'background-color': 'rgb(220, 30, 40)', 'box-shadow': 'rgba(0, 0, 0, 0.5) 0px 10px 20px 0px' }, wait: 0 },
    { action: 'click', selector: '#toggle', wait: 20 },
    { action: 'assert', selector: '#result', text: 'Expanded', wait: 0 },
    { action: 'scroll', y: 700, wait: 150 }
  ] });
  t.diagnostic(JSON.stringify({ durationMs: result.finishedAt - result.startedAt, screenshots: result.screenshots.map(shot => ({ name: shot.name, elapsedMs: shot.capturedAt - result.startedAt })) }));
  assert.equal(result.passed, true, JSON.stringify(result.errors));
  assert.equal(result.title, 'Portfolio effects'); assert.equal(result.path, '/portfolio'); assert.equal(result.status, 200);
  assert.equal(result.screenshots.length, 6); assert.equal(result.steps.length, 5);
  assert.deepEqual(result.steps[1].computedStyle, { 'background-color': 'rgb(220, 30, 40)', 'box-shadow': 'rgba(0, 0, 0, 0.5) 0px 10px 20px 0px' }); assert.equal(result.steps[3].text, 'Expanded');
  assert.equal(result.steps[4].y, 700); assert.equal(result.steps[4].wait, 150);
  assert.ok(result.screenshots.every(shot => shot.png.subarray(1, 4).toString() === 'PNG'));
  assert.ok(result.steps[4].state.scrollY > 0);
  assert.notDeepEqual(result.screenshots[0].png, result.screenshots[1].png);
});

test('wrong route and failed interaction assertions never pass visual verification', { skip: !executable, timeout: 45000 }, async t => {
  const fixtureData = await fixture(t);
  const wrongPage = await verifyVisual({ ...fixtureData, expectedPath: '/other' });
  assert.equal(wrongPage.passed, false); assert.match(wrongPage.errors.join(' '), /目标页不匹配/);
  const wrongState = await verifyVisual({ ...fixtureData, steps: [{ action: 'assert', selector: '#result', text: 'Expanded' }] });
  assert.equal(wrongState.passed, false); assert.equal(wrongState.steps[0].passed, false); assert.match(wrongState.errors.join(' '), /目标文本/);
});

test('real browser retains product query and hash routes and rejects redirected view mismatches', { skip: !executable, timeout: 60000 }, async t => {
  const data=await fixture(t),origin=new URL(data.url).origin;
  for(const route of ['/?view=product','/#/search']) {
    const result=await verifyVisual({...data,url:origin+route,expectedPath:route,steps:[{action:'assert',selector:'#card',text:'Hover card',wait:0}]});
    assert.equal(result.passed,true,result.errors.join(' '));
    assert.equal(result.route,route); assert.equal(result.screenshots[0].route,route);
  }
  const wrong=await verifyVisual({...data,url:origin+'/?view=admin',expectedPath:'/?view=product'});
  assert.equal(wrong.passed,false); assert.match(wrong.errors.join(' '),/目标页不匹配/);
});

test('missing page and unsafe click side effects fail without making write requests', { skip: !executable, timeout: 45000 }, async t => {
  const fixtureData = await fixture(t);
  const missing = await verifyVisual({ ...fixtureData, url: fixtureData.url.replace('/portfolio', '/missing'), expectedPath: '/missing' });
  assert.equal(missing.passed, false); assert.equal(missing.status, 404);
  const writes = await verifyVisual({ ...fixtureData, steps: [{ action: 'click', selector: '#write', wait: 100 }] });
  assert.equal(writes.passed, false); assert.match(writes.errors.join(' '), /写入请求/);
  assert.equal(fixtureData.calls.filter(call => call.method === 'POST').length, 0);
  const external = await verifyVisual({ ...fixtureData, steps: [{ action: 'click', selector: '#external', wait: 100 }] });
  assert.equal(external.passed, false); assert.match(external.errors.join(' '), /对外导航/);
});

test('explicit hidden-state assertions pass without counting invisible text as rendered evidence', { skip: !executable, timeout: 45000 }, async t => {
  const fixtureData = await fixture(t);
  const hidden = await verifyVisual({ ...fixtureData, steps: [{ action: 'assert', selector: '#hidden', computedStyle: { display: 'none' }, wait: 0 }, { action: 'assert', selector: '#transparent', computedStyle: { opacity: '0' }, wait: 0 }] });
  assert.equal(hidden.passed, true, hidden.errors.join(' '));
  const hiddenText = await verifyVisual({ ...fixtureData, steps: [{ action: 'assert', selector: '#hidden', text: 'Hidden details', computedStyle: { display: 'none' } }] });
  assert.equal(hiddenText.passed, false); assert.match(hiddenText.errors.join(' '), /不可见/);
  const transparentText = await verifyVisual({ ...fixtureData, steps: [{ action: 'assert', selector: '#transparent', text: 'Invisible text' }] });
  assert.equal(transparentText.passed, false); assert.match(transparentText.errors.join(' '), /不可见/);
});
