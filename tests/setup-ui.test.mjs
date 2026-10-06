import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { mkdtemp, readFile, rm, mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { findBrowser, launchBrowser, firstPage } from '../connector/browser.mjs';

const executable = findBrowser();
const evaluate = async (page, expression) => {
  const result = await page.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
  return result.result.value;
};
const until = (page, expression) => evaluate(page, `(async()=>{const end=Date.now()+5000;while(!(${expression})){if(Date.now()>end)throw Error('UI state timeout');await new Promise(r=>setTimeout(r,25));}})()`);

test('setup project inventory is local, readable, keyboard accessible and recovers without stale paths', { skip: !executable, timeout: 60000 }, async t => {
  const profileDir = await mkdtemp(join(tmpdir(), 'droprun-setup-ui-'));
  const projects = Array.from({ length: 101 }, (_, n) => ({ id: `studio-${String(n).padStart(4, '0')}`, name: n === 2 ? '<img src=x onerror=alert(1)>' : 'Studio', available: n !== 100, roots: [{ path: `C:\\Projects\\${'long-folder-'.repeat(14)}\\studio-${n}`, available: n !== 100 }] }));
  let status = { ready: true, projectCount: projects.length, projects }, fail = false, releaseDoctor;
  const calls = [];
  const server = createServer(async (req, res) => {
    calls.push({ path: req.url, method: req.method });
    const assets = { '/': ['setup.html', 'text/html'], '/setup.css': ['setup.css', 'text/css'], '/setup-ui.js': ['setup-ui.js', 'text/javascript'] };
    if (assets[req.url]) { res.setHeader('Content-Type', assets[req.url][1]); return res.end(await readFile(new URL('../installer/' + assets[req.url][0], import.meta.url))); }
    res.setHeader('Content-Type', 'application/json');
    if (req.method !== 'GET' || req.headers['x-droprun-setup'] !== 'local-ui-fixture') { res.writeHead(403); return res.end('{}'); }
    if (req.url === '/api/status') return res.end(JSON.stringify({ step: '', messages: [], accounts: [], busy: false }));
    if (req.url === '/api/doctor') {
      if (releaseDoctor) await new Promise(resolve => { releaseDoctor = resolve; });
      if (fail) { res.writeHead(503); return res.end(JSON.stringify({ error: 'Synthetic local check failed. Try again.' })); }
      return res.end(JSON.stringify({ git: true, codex: status.ready, browser: true, wrangler: true, media: { ffmpeg: true }, codexStatus: status }));
    }
    res.writeHead(404); res.end('{}');
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser, page;
  t.after(async () => {
    if (typeof releaseDoctor === 'function') releaseDoctor();
    page?.close();
    try {
      await browser?.close();
      if (browser) await rm(profileDir, { recursive: true, force: true, maxRetries: 20, retryDelay: 100 });
    } finally { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
  });
  browser = await launchBrowser({ executable, profileDir }); page = await firstPage(browser.port);
  await page.send('Page.enable'); await page.send('Emulation.setLocaleOverride', { locale: 'en-US' });
  await page.send('Page.navigate', { url: `http://127.0.0.1:${server.address().port}/#local-ui-fixture` });
  await until(page, 'document.querySelectorAll("#project-list > li").length===101');
  await evaluate(page, 'language="en";translate();renderDoctor()');
  assert.equal(await evaluate(page, 'location.hash'), '');
  assert.match(await evaluate(page, 'document.querySelector("#doctor-note").textContent'), /101 existing projects/);
  assert.equal(await evaluate(page, 'document.querySelectorAll("#project-list img").length'), 0);
  assert.ok((await evaluate(page, 'document.querySelector("#project-list").textContent')).includes('<img src=x onerror=alert(1)>'));
  await evaluate(page, 'document.querySelector("summary").focus()');
  assert.equal(await evaluate(page, 'document.activeElement.tagName'), 'SUMMARY');
  await page.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter', text: '\r', windowsVirtualKeyCode: 13 });
  await page.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
  assert.equal(await evaluate(page, 'document.querySelector("details").open'), true);
  await page.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 });
  await page.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 });
  assert.equal(await evaluate(page, 'document.activeElement.id'), 'project-list');
  await evaluate(page, 'document.querySelector("#language").click()');
  assert.match(await evaluate(page, 'document.querySelector("#doctor-note").textContent'), /101 个已有项目/);
  assert.equal(await evaluate(page, 'document.querySelector("#project-list").getAttribute("aria-label")'), '本机项目');
  assert.match(await evaluate(page, 'document.querySelector("#project-list > li:last-child").textContent'), /目录不可用/);
  for (const [width, zoom, theme, motion] of [[320, 1, 'light', 'no-preference'], [1280, 2, 'dark', 'reduce']]) {
    await page.send('Emulation.setDeviceMetricsOverride', { width, height: 1000, deviceScaleFactor: 1, mobile: false });
    await page.send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-color-scheme', value: theme }, { name: 'prefers-reduced-motion', value: motion }] });
    await evaluate(page, `document.documentElement.style.zoom=${zoom};document.querySelector('summary').scrollIntoView()`);
    assert.equal(await evaluate(page, 'document.documentElement.scrollWidth<=document.documentElement.clientWidth+1'), true, `${width}/${zoom} horizontal overflow`);
    assert.equal(await evaluate(page, 'document.querySelector("#project-list").scrollWidth<=document.querySelector("#project-list").clientWidth+1'), true);
    if (motion === 'reduce') assert.equal(await evaluate(page, 'getComputedStyle(document.querySelector("#project-list")).animationName'), 'none');
    if (process.env.DROPRUN_SETUP_SCREENSHOTS) {
      await evaluate(page, 'Promise.all(document.getAnimations().map(animation=>animation.finished))');
      const dir = resolve(process.env.DROPRUN_SETUP_SCREENSHOTS); await mkdir(dir, { recursive: true });
      const capture = await page.send('Page.captureScreenshot', { format: 'png' });
      await writeFile(join(dir, `setup-projects-${width}-${zoom}-${theme}.png`), Buffer.from(capture.data, 'base64'));
    }
  }
  releaseDoctor = true;
  await evaluate(page, 'document.querySelector("#check").click()');
  assert.equal(await evaluate(page, 'document.querySelector("#check").disabled'), true);
  assert.equal(await evaluate(page, 'document.querySelector("#project-inventory").hidden'), true);
  assert.equal(await evaluate(page, 'document.querySelector("#project-list").children.length'), 0);
  fail = true;
  const requestDeadline = Date.now() + 5000;
  while (typeof releaseDoctor !== 'function') { assert.ok(Date.now() < requestDeadline, 'diagnostic request did not arrive'); await new Promise(resolve => setTimeout(resolve, 10)); }
  releaseDoctor(); releaseDoctor = null;
  await until(page, '!document.querySelector("#check").disabled');
  await evaluate(page, 'refresh()');
  assert.match(await evaluate(page, 'document.querySelector("#doctor-note").textContent'), /Synthetic local check failed/);
  fail = false; status = { ready: true, projectCount: 0, projects: [] };
  await evaluate(page, 'doctor()');
  assert.equal(await evaluate(page, 'document.querySelector("#project-empty").hidden'), false);
  assert.equal(await evaluate(page, 'document.querySelector("#project-list").hidden'), true);
  status = { ready: false, status: 'login-required' };
  await evaluate(page, 'doctor()');
  assert.equal(await evaluate(page, 'document.querySelector("#project-inventory").hidden'), true);
  assert.match(await evaluate(page, 'document.querySelector("#doctor-note").textContent'), /请登录 Codex/);
  assert.ok(calls.every(call => call.method === 'GET'));
  assert.ok(calls.filter(call => call.path.startsWith('/api/')).every(call => ['/api/doctor', '/api/status'].includes(call.path)));
});
