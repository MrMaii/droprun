import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, rm, readdir, symlink, readFile } from 'node:fs/promises';
import { EventEmitter } from 'node:events';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { Previews, resolveServerCommand, waitForPublicPreview } from '../connector/preview.mjs';
import { freePort } from '../connector/browser.mjs';

async function site(t) {
  const root = await mkdtemp(join(tmpdir(), 'droprun-preview-'));
  t.after(async () => {
    assert(resolve(root).startsWith(resolve(tmpdir()) + '\\droprun-preview-') || resolve(root).startsWith(resolve(tmpdir()) + '/droprun-preview-'));
    await rm(root, { recursive: true, force: true, maxRetries: 3, retryDelay: 100 });
  });
  await mkdir(join(root, 'dist'));
  await writeFile(join(root, 'dist', 'index.html'), '<!doctype html><h1>hello from preview</h1>');
  await writeFile(join(root, 'package.json'), JSON.stringify({ scripts: { dev: 'node server.js', build: 'echo build' } }));
  return root;
}
const fakeTunnel = async port => ({ child: { pid: null, kill() {} }, url: 'http://127.0.0.1:' + port });

test('public readiness requires the exact gate handshake, not an allocated URL or unrelated response', async t => {
  const server = createServer((req, res) => { res.writeHead(302, { 'Set-Cookie': 'droprun_preview=wrong; Secure' }); res.end(); });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => { server.closeAllConnections(); server.close(); });
  await assert.rejects(waitForPublicPreview(`http://127.0.0.1:${server.address().port}/?k=expected`, 'expected', () => true, 40), /公网预览尚不可访问/);
});

test('unreachable public tunnel is rebuilt once before exposing a ready preview', async t => {
  const root = await site(t), deadPort = await freePort();
  let attempts = 0, killed = 0, checking = 0;
  const previews = new Previews({
    tunnel: async port => ({ child: { kill() { killed++; } }, url: 'http://127.0.0.1:' + (++attempts === 1 ? deadPort : port) }),
    publicReady: async (...args) => { checking++; assert.equal(previews.get('retry'), null); await waitForPublicPreview(...args, 40); },
  });
  t.after(() => previews.stopAll());
  const result = await previews.start('retry', root, { static: 'dist' });
  assert.equal(attempts, 2); assert.equal(checking, 2); assert.equal(killed, 1);
  assert.equal(previews.get('retry').url, result.url);
});

test('persistent public failure cleans both tunnels and never publishes ready', async t => {
  const root = await site(t), deadPort = await freePort(), events = [];
  let killed = 0;
  const previews = new Previews({
    tunnel: async () => ({ child: { kill() { killed++; } }, url: 'http://127.0.0.1:' + deadPort }),
    publicReady: (...args) => waitForPublicPreview(...args, 40), onStop: event => events.push(event),
  });
  t.after(() => previews.stopAll());
  await assert.rejects(previews.start('failed-public', root, { static: 'dist' }), /公网预览尚不可访问/);
  assert.equal(killed, 2); assert.equal(previews.get('failed-public'), null);
  assert.equal(previews.active.size, 0); assert.equal(events.length, 0);
});

test('only conventional dev scripts, in-project static directories and http.server are accepted', async t => {
  const root = await site(t);
  assert.equal((await resolveServerCommand(root, { script: 'dev' })).label, 'npm run dev');
  assert.equal((await resolveServerCommand(root, { static: 'dist' })).kind, 'static');
  assert.deepEqual((await resolveServerCommand(root, { command: 'python -m http.server 8080' })).args, ['-m', 'http.server', '8080']);
  for (const bad of [{ script: 'build' }, { script: 'evil; rm -rf /' }, { command: 'rm -rf /' }, { command: 'python -m http.server 8080 && curl x' }, { static: '../..' }, { static: 'missing' }, {}]) await assert.rejects(resolveServerCommand(root, bad), undefined, JSON.stringify(bad));
});

test('a static preview is reachable only with the key, which is exchanged for a cookie', async t => {
  const root = await site(t);
  const previews = new Previews({ cloudflaredBin: null, minutes: 1, tunnel: fakeTunnel });
  t.after(() => previews.stopAll());
  const started = await previews.start('task-1', root, { static: 'dist', path: '/' });
  assert.match(started.url, /^http:\/\/127\.0\.0\.1:\d+\/\?k=[A-Za-z0-9_-]{16}$/);
  assert.ok(started.expiresAt > Date.now());
  const bare = started.url.replace(/\?k=.*/, '');
  assert.equal((await fetch(bare)).status, 403);
  const entry = await fetch(started.url, { redirect: 'manual' });
  assert.equal(entry.status, 302); assert.equal(entry.headers.get('location'), '/');
  const cookie = entry.headers.get('set-cookie').split(';')[0];
  const page = await fetch(bare, { headers: { cookie } });
  assert.equal(page.status, 200); assert.match(await page.text(), /hello from preview/);
  assert.equal((await fetch(bare + '../package.json', { headers: { cookie } })).status, 404);
  assert.equal(previews.get('task-1').url, started.url);
  await previews.stopForCwd(root);
  assert.equal(previews.get('task-1'), null);
  await assert.rejects(fetch(bare, { headers: { cookie } }));
});

test('a dev server that never listens fails with its output instead of hanging', async t => {
  const root = await site(t);
  await writeFile(join(root, 'server.js'), 'console.error("synthetic startup failure"); process.exit(3);');
  const previews = new Previews({ cloudflaredBin: null, minutes: 1, tunnel: fakeTunnel });
  t.after(() => previews.stopAll());
  await assert.rejects(previews.start('task-2', root, { script: 'dev', port: 39999 }), /synthetic startup failure|没有响应/);
  assert.equal(previews.get('task-2'), null);
});

test('sequential tasks retain separate immutable static builds and exclude private files', async t => {
  const root = await site(t), snapshots = join(root, 'snapshots');
  await writeFile(join(root, 'dist', '.env.production'), 'SECRET=value');
  await mkdir(join(root, 'dist', 'node_modules')); await writeFile(join(root, 'dist', 'node_modules', 'private.txt'), 'private');
  const previews = new Previews({ tunnel: fakeTunnel, snapshotDir: snapshots }); t.after(() => previews.stopAll());
  const first = await previews.start('first', root, { static: 'dist' });
  await writeFile(join(root, 'dist', 'index.html'), '<h1>second build</h1>');
  const second = await previews.start('second', root, { static: 'dist' });
  assert.equal(first.mode, 'snapshot'); assert.notEqual(first.revision, second.revision);
  assert.match(await (await fetch(first.localUrl)).text(), /hello from preview/);
  assert.match(await (await fetch(second.localUrl)).text(), /second build/);
  assert.equal((await fetch(first.localUrl + '/.env.production')).status, 404);
  assert.equal((await fetch(first.localUrl + '/node_modules/private.txt')).status, 404);
  await previews.stopLiveForCwd(root); assert.ok(previews.get('first')); assert.ok(previews.get('second'));
  await previews.stopAll(); assert.equal((await readdir(snapshots)).length, 4);
});

test('snapshot directory validation rejects sibling prefixes and symbolic links', async t => {
  const root = await site(t), sibling = root + '-outside';
  await mkdir(sibling); t.after(() => rm(sibling, { recursive: true, force: true }));
  await assert.rejects(resolveServerCommand(root, { static: sibling }), /不在项目内/);
  await symlink(sibling, join(root, 'dist', 'escape'), 'junction');
  const previews = new Previews({ tunnel: fakeTunnel }); t.after(() => previews.stopAll());
  await assert.rejects(previews.start('symlink', root, { static: 'dist' }), /符号链接/);
  assert.equal(previews.get('symlink'), null);
});

test('preview renewal and process loss publish accurate lifecycle events', async t => {
  const root = await site(t), events = [], child = new EventEmitter(); child.kill = () => {};
  const previews = new Previews({ tunnel: async port => ({ child, url: 'http://127.0.0.1:' + port }), onStop: event => events.push(event) });
  t.after(() => previews.stopAll());
  const initial = await previews.start('renew', root, { static: 'dist', path: '/?x=1#section' });
  assert.match(initial.url, /\?x=1&k=.*#section$/);
  await new Promise(resolve => setTimeout(resolve, 15));
  assert.ok(previews.renew('renew').expiresAt > initial.expiresAt);
  child.emit('exit', 1);
  for (let index = 0; index < 50 && !events.length; index++) await new Promise(resolve => setTimeout(resolve, 10));
  assert.equal(previews.get('renew'), null); assert.equal(events[0].reason, 'process-exited');
  assert.equal(events[0].taskId, 'renew'); assert.equal(events[0].revision, initial.revision);
});

test('expiry frees resources and a capacity rejection preserves already delivered previews', async t => {
  const root = await site(t), events = [];
  const previews = new Previews({ tunnel: fakeTunnel, minutes: 0.002, maxActive: 1, onStop: event => events.push(event) });
  t.after(() => previews.stopAll());
  const first = await previews.start('first', root, { static: 'dist' });
  await assert.rejects(previews.start('second', root, { static: 'dist' }), /上限/);
  assert.equal(previews.get('first').url, first.url);
  for (let index = 0; index < 50 && !events.length; index++) await new Promise(resolve => setTimeout(resolve, 20));
  assert.equal(previews.get('first'), null); assert.equal(events[0].reason, 'expired');
});

test('SPA snapshots serve explicit routes without exposing missing assets and reject redirect paths', async t => {
  const root = await site(t), previews = new Previews({ tunnel: fakeTunnel }); t.after(() => previews.stopAll());
  const preview = await previews.start('spa', root, { static: 'dist', spa: true, path: '/about' });
  assert.equal((await fetch(preview.localUrl + '/about')).status, 200);
  assert.equal((await fetch(preview.localUrl + '/missing.js')).status, 404);
  await assert.rejects(previews.start('bad', root, { static: 'dist', path: '//example.com' }), /站内/);
});

test('a restarted manager restores exactly the original snapshot after the project changes', async t => {
  const root = await site(t), options = { tunnel: fakeTunnel, snapshotDir: join(root, 'snapshots') };
  const first = new Previews(options), second = new Previews(options); t.after(() => second.stopAll());
  const saved = { ...await first.start('original', root, { static: 'dist', path: '/about', spa: true }), cwd: root, request: { static: 'dist', path: '/about', spa: true } };
  await first.stopAll(); await writeFile(join(root, 'dist', 'index.html'), '<h1>newer code</h1>');
  const restored = await second.restore('original', saved);
  assert.equal(restored.revision, saved.revision); assert.equal(restored.snapshotPath, saved.snapshotPath); assert.equal(restored.createdAt, saved.createdAt);
  assert.match(await (await fetch(restored.localUrl + '/about')).text(), /hello from preview/);
  await assert.rejects(second.restore('wrong-task', saved), /任务版本不匹配/);
  await assert.rejects(second.restore('original', { ...saved, snapshotPath: root }), /路径无效/);
});

test('expired or modified snapshot files cannot be represented as the original delivery', async t => {
  const root = await site(t), previews = new Previews({ tunnel: fakeTunnel, snapshotDir: join(root, 'snapshots') }); t.after(() => previews.stopAll());
  const saved = { ...await previews.start('old', root, { static: 'dist' }), cwd: root };
  await previews.stop('old');
  await writeFile(join(saved.snapshotPath, 'index.html'), '<h1>tampered</h1>');
  await assert.rejects(previews.restore('old', saved), /内容已变化/);
  const metadata = JSON.parse(await readFile(saved.snapshotPath + '.json', 'utf8')); metadata.createdAt = Date.now() - 8 * 86400000;
  await writeFile(saved.snapshotPath + '.json', JSON.stringify(metadata));
  await assert.rejects(previews.restore('old', saved), /7 天/);
  assert.deepEqual(await readdir(join(root, 'snapshots')), []);
});

test('live previews stop before same-project edits and reopen with a distinct run revision', async t => {
  const root = await site(t), events = [], previews = new Previews({ tunnel: fakeTunnel, snapshotDir: join(root, 'snapshots'), onStop: event => events.push(event) });
  try {
    await writeFile(join(root, 'server.js'), "require('node:http').createServer((request,response)=>response.end('live demo')).listen(Number(process.env.PORT),'127.0.0.1');");
    const request = { script: 'dev', port: await freePort(), path: '/demo' };
    const saved = { ...await previews.start('live', root, request), cwd: root, request };
    const firstChild = previews.active.get('live').processes[0];
    const staticPreview = await previews.start('snapshot', root, { static: 'dist' });
    assert.equal(saved.mode, 'live'); assert.equal(typeof saved.revision, 'string'); assert.equal(saved.snapshotPath, null);
    await previews.stopLiveForCwd(root);
    assert.equal(previews.get('live'), null); assert.equal(previews.get('snapshot').revision, staticPreview.revision);
    assert.equal(events[0].reason, 'project-changing');
    assert.throws(() => process.kill(firstChild.pid, 0), { code: 'ESRCH' });
    await assert.rejects(fetch(saved.localUrl));
    const restored = await previews.restore('live', saved);
    assert.equal(restored.mode, 'live'); assert.notEqual(restored.revision, saved.revision);
    assert.equal(await (await fetch(restored.localUrl)).text(), 'live demo');
    const restoredChild = previews.active.get('live').processes[0];
    await previews.stopAll();
    assert.throws(() => process.kill(restoredChild.pid, 0), { code: 'ESRCH' });
    await assert.rejects(fetch(restored.localUrl));
  } finally { await previews.stopAll(); }
});

test('starting a preview never attaches to or terminates a pre-existing port owner', async t => {
  const root = await site(t), previews = new Previews({ tunnel: fakeTunnel }); t.after(() => previews.stopAll());
  const server = createServer((request, response) => response.end('unrelated process'));
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(async () => { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); });
  const port = server.address().port;
  await assert.rejects(previews.start('occupied', root, { script: 'dev', port }), /已被占用/);
  assert.equal(await (await fetch('http://127.0.0.1:' + port)).text(), 'unrelated process');
});
