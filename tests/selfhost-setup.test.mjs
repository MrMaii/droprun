import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { existsSync } from 'node:fs';
import { configLocation, pairingLink, relayOrigin, protectSecret } from '../connector/config.mjs';
import { trustedLocalRequest } from '../connector/local-boundary.mjs';
import { provision, validateSetup } from '../scripts/setup-core.mjs';
import { serveSetup, diagnoseCodex } from '../scripts/setup.mjs';
import { snapshotManifest, publishSnapshot } from '../connector/selfhost-preview.mjs';
import { installMediaTools } from '../scripts/setup-tools.mjs';
import { Previews } from '../connector/preview.mjs';

const fixture = async t => { const dir = await mkdtemp(join(tmpdir(), 'droprun-selfhost-')); t.after(() => rm(dir, { recursive: true, force: true })); return dir; };
test('installed data is separate; existing developer config remains an explicit compatibility path', async t => {
  const root = await fixture(t), appdata = join(root, 'appdata');
  assert.equal(configLocation(root, { LOCALAPPDATA: appdata }).dataDir, join(appdata, 'DropRun'));
  await mkdir(join(root, '.local')); await writeFile(join(root, '.local/config.json'), '{}');
  assert.equal(configLocation(root, { LOCALAPPDATA: appdata }).legacy, true);
  assert.equal(configLocation(root, { LOCALAPPDATA: appdata, DROPRUN_DATA_DIR: join(root, 'chosen') }).dataDir, join(root, 'chosen'));
});
test('pairing carries one exact HTTPS instance, never embedded credentials or arbitrary paths', () => {
  const instance = '11111111-1111-4111-8111-111111111111';
  const link = new URL(pairingLink('https://my-relay.example', instance, '0123456789ABCDEF0123'));
  assert.equal(link.searchParams.get('relay'), 'https://my-relay.example');
  assert.equal(link.searchParams.get('instance'), instance);
  assert.equal(new URL(pairingLink('https://my-relay.example', instance, '01234-56789-ABCDE-F0123')).searchParams.get('code'), '0123456789ABCDEF0123');
  for (const value of ['http://localhost', 'https://user:pass@example.com', 'https://example.com/path', 'https://example.com?key=x']) assert.throws(() => relayOrigin(value));
  assert.throws(() => pairingLink('https://example.com', null, '0123456789ABCDEF0123'));
});
test('local management rejects DNS rebinding and cross-site requests even from loopback', () => {
  const req = { socket: { remoteAddress: '127.0.0.1' }, headers: { host: '127.0.0.1:47494' } };
  assert.equal(trustedLocalRequest(req, 47494), true);
  for (const headers of [{ host: 'evil.test:47494' }, { ...req.headers, origin: 'https://evil.test' }, { ...req.headers, 'sec-fetch-site': 'cross-site' }]) assert.equal(trustedLocalRequest({ ...req, headers }, 47494), false);
});
test('browser setup needs its private session token; no unauthenticated action starts', async t => {
  const dataDir = await fixture(t);
  const { server, token, port } = await serveSetup({ dataDir, port: 0, open: false });
  t.after(() => new Promise(resolve => server.close(resolve)));
  const url = `http://127.0.0.1:${port}`;
  assert.equal((await fetch(url)).status, 200);
  assert.equal((await fetch(url + '/api/status')).status, 403);
  assert.equal((await fetch(url + '/api/login', { method: 'POST', body: '{}' })).status, 403);
  assert.equal((await fetch(url + '/api/status', { headers: { 'X-DropRun-Setup': token, Origin: 'https://evil.test' } })).status, 403);
  const response = await fetch(url + '/api/status', { headers: { 'X-DropRun-Setup': token } });
  assert.equal(response.status, 200); assert.equal((await response.json()).busy, false);
});

test('Codex readiness checks authentication and the project API without returning account details', async () => {
  const fake = (account, projects) => ({ start: async () => {}, call: async method => method === 'account/read' ? account : projects, close() {} });
  const ready = await diagnoseCodex({ create: () => fake({ account: { email: 'never-return@example.test' } }, { data: [{ path: 'private-path' }] }) });
  assert.equal(ready.ready, true); assert.equal(ready.projectCount, 1);
  assert.ok(!JSON.stringify(ready).includes('private-path')); assert.ok(!JSON.stringify(ready).includes('email'));
  assert.equal((await diagnoseCodex({ create: () => fake({ account: null, requiresOpenaiAuth: true }, { data: [] }) })).status, 'login-required');
  assert.equal((await diagnoseCodex({ create: () => fake({ account: {} }, {}) })).status, 'unsupported');
  assert.equal((await diagnoseCodex({ create: () => { throw Object.assign(new Error('missing'), { code: 'ENOENT' }); } })).status, 'missing');
  assert.equal((await diagnoseCodex({ create: () => ({ start: () => new Promise(() => {}), close() {} }), timeout: 5 })).status, 'timeout');
});
test('Cloudflare setup resumes resource creation and keeps credentials off disk in plaintext', async t => {
  const root = await fixture(t), dataDir = join(root, 'data');
  await mkdir(join(root, 'node_modules/wrangler/bin'), { recursive: true }); await writeFile(join(root, 'node_modules/wrangler/bin/wrangler.js'), '');
  const input = { accountId: 'a'.repeat(32), name: 'droprun-test', costAccepted: true };
  assert.throws(() => validateSetup({ ...input, costAccepted: false }));
  let migrationsFail = true, initialDeploys = 0, database = null, bucket = false;
  const run = async (exe, args) => {
    const configFile = args[args.indexOf('--config') + 1], config = JSON.parse(await readFile(configFile, 'utf8'));
    if (args[1] === 'd1' && args[2] === 'list') return JSON.stringify(database ? [database] : []);
    if (args[1] === 'd1' && args[2] === 'create') { database = { name: args[3], uuid: 'd'.repeat(32) }; return 'Created'; }
    if (args[1] === 'r2' && args[3] === 'info') { if (!bucket) throw new Error('The specified bucket does not exist'); return '{}'; }
    if (args[1] === 'r2' && args[3] === 'create') { bucket = true; return 'Created'; }
    if (args.includes('migrations')) { if (migrationsFail) throw new Error('Synthetic connection loss'); return ''; }
    assert.match(config.name, new RegExp('^' + input.name + '-[a-f0-9]{8}(-preview)?$'));
    if (!config.name.endsWith('-preview') && !config.vars.PREVIEW_ORIGIN) initialDeploys++;
    assert.ok(config.d1_databases[0].database_id); assert.ok(config.r2_buckets[0].bucket_name);
    // Real CI Wrangler never writes auto-provisioned bindings back to the config.
    return `Deployed https://${config.name}.owner.workers.dev`;
  };
  const protect = async (value, decrypt) => decrypt ? value.slice('sealed:'.length) : 'sealed:' + value;
  const fetchImpl = async url => url.endsWith('/health') ? Response.json({ ready: true, protocolVersion: 2, instanceId: JSON.parse(await readFile(join(dataDir, 'deployment/state.json'))).instanceId }) : Response.json({ projects: [] });
  await assert.rejects(provision({ root, dataDir, input, run, protect, fetchImpl }), /Synthetic connection loss/);
  assert.equal(existsSync(join(dataDir, 'deployment/deploy-secrets.json')), false);
  migrationsFail = false;
  const result = await provision({ root, dataDir, input, run, protect, fetchImpl });
  assert.equal(initialDeploys, 1);
  const config = JSON.parse(await readFile(join(dataDir, 'config.json')));
  assert.equal(config.connectorToken, undefined); assert.ok(config.connectorTokenProtected.startsWith('sealed:'));
  assert.equal(config.relay, result.relay);
  assert.equal(existsSync(join(dataDir, 'deployment/deploy-secrets.json')), false);
  await assert.rejects(provision({ root, dataDir, input: { ...input, name: 'another-instance' }, run, protect, fetchImpl }), /another instance/);
});
test('setup never replaces a preexisting private Relay configuration', async t => {
  const root = await fixture(t), dataDir = join(root, 'data'); await mkdir(dataDir);
  const original = '{"relay":"https://private.example","connectorToken":"private-existing-value"}'; await writeFile(join(dataDir, 'config.json'), original);
  await assert.rejects(provision({ root, dataDir, input: { accountId: 'b'.repeat(32), name: 'new-instance', costAccepted: true } }), /will not overwrite/);
  assert.equal(await readFile(join(dataDir, 'config.json'), 'utf8'), original);
});
test('Windows DPAPI protects and decrypts synthetic secrets for the current user', { skip: process.platform !== 'win32' }, async () => {
  const value = 'synthetic-test-only-not-a-real-token'; const encrypted = await protectSecret(value);
  assert.ok(!encrypted.includes(value)); assert.equal(await protectSecret(encrypted, true), value);
});
test('snapshot upload validates missing paths, immutable bytes and requested route', async t => {
  const dir = await fixture(t); await writeFile(join(dir, 'index.html'), '<h1>Test</h1>');
  const files = await snapshotManifest(dir); assert.equal(files.length, 1);
  const calls = [], snapshotId = 'a'.repeat(32);
  const entry = { snapshot: dir, taskId: 't'.repeat(24), revision: 'b'.repeat(64), path: '/work?view=product#demo' };
  const fetchImpl = async (url, init) => { calls.push({ url, init }); return url.endsWith('/snapshots') ? Response.json({ snapshotId, missing: ['index.html'] }) : url.includes('/publish') ? Response.json({ url: 'https://preview.example/s/token/work', expiresAt: Date.now() + 60000, kind: 'snapshot', version: entry.revision }) : Response.json({ ready: true }); };
  await publishSnapshot({ relay: 'https://relay.example', connectorToken: 'synthetic' }, entry, fetchImpl);
  assert.equal(JSON.parse(calls.at(-1).init.body).path, entry.path);
  await assert.rejects(publishSnapshot({ relay: 'https://relay.example', connectorToken: 'synthetic' }, entry, async () => Response.json({ snapshotId, missing: ['../config.json'] })), /outside this snapshot/);
});
test('media tool checksum mismatch never installs or executes downloaded data', async t => {
  const dir = await fixture(t);
  await assert.rejects(installMediaTools(dir, () => {}, async () => new Response('tampered')), /checksum mismatch/);
  assert.equal(existsSync(join(dir, 'yt-dlp.exe')), false);
});
test('installed static previews publish without a tunnel and renew the exact immutable version', async t => {
  const dir = await fixture(t), source = join(dir, 'site'); await mkdir(source); await writeFile(join(source, 'index.html'), '<h1>Original</h1>');
  const publishes = [];
  const previews = new Previews({ snapshotDir: join(dir, 'snapshots'), allowQuickTunnels: false, publishSnapshot: async entry => { publishes.push({ revision: entry.revision, path: entry.path }); return { url: 'https://preview.example/s/' + publishes.length + '/', expiresAt: Date.now() + 1800000, version: entry.revision }; } });
  t.after(() => previews.stopAll());
  const first = await previews.start('task-static', source, { static: '.', path: '/?view=product' });
  assert.equal(first.mode, 'snapshot'); assert.equal(publishes.length, 1);
  await writeFile(join(source, 'index.html'), '<h1>Changed later</h1>');
  const renewed = await previews.renewPublished('task-static');
  assert.equal(renewed.revision, first.revision); assert.equal(publishes[1].path, '/?view=product');
  assert.notEqual(renewed.url, first.url);
  await assert.rejects(previews.start('task-live', source, { command: 'python -m http.server 34567', port: 34567 }), /managed Cloudflare Tunnel/);
});
