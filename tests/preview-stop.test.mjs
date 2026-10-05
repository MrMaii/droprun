import test from 'node:test';
import assert from 'node:assert/strict';
import childProcess from 'node:child_process';
import { syncBuiltinESMExports } from 'node:module';
import { EventEmitter } from 'node:events';
import { createServer } from 'node:http';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { Previews } from '../connector/preview.mjs';
import { freePort } from '../connector/browser.mjs';

async function fixture(t) {
  const root = await mkdtemp(join(tmpdir(), 'droprun-preview-stop-'));
  const worker = Object.assign(new EventEmitter(), { pid: 424242, exitCode: null, signalCode: null, stdout: new EventEmitter(), stderr: new EventEmitter() });
  const server = createServer((req, res) => res.end('synthetic worker'));
  let outcome = 'hold-close', pendingKiller, killers = 0, closed = false;
  const events = [], previews = new Previews({
    snapshotDir: join(root, 'snapshots'),
    tunnel: async port => ({ child: { pid: null, kill() {} }, url: 'http://127.0.0.1:' + port }),
    onStop: event => { assert.equal(closed, true); events.push(event); },
  });
  function releaseClose() { closed = true; worker.emit('close', worker.exitCode); }
  function exitWorker(signal = null) { server.closeAllConnections(); server.close(); worker.exitCode = signal ? null : 0; worker.signalCode = signal; worker.emit('exit', worker.exitCode, signal); }
  const mock = t.mock.method(childProcess, 'spawn', (executable, args, options) => {
    if (executable === 'npm.cmd') { server.listen(Number(options.env.PORT), '127.0.0.1'); return worker; }
    assert.equal(executable, 'taskkill');
    assert.deepEqual(args, ['/pid', '424242', '/T', '/F']);
    killers++;
    const killer = new EventEmitter(); pendingKiller = killer;
    queueMicrotask(() => {
      if (outcome === 'error') { killer.emit('error', new Error('synthetic taskkill spawn failure')); return; }
      if (outcome === 'timeout') return;
      if (outcome === 'nonzero') { killer.emit('close', 5); return; }
      exitWorker(); killer.emit('exit', 0); killer.emit('close', 0);
      if (outcome === 'success') releaseClose();
    });
    return killer;
  });
  syncBuiltinESMExports();
  t.after(async () => {
    outcome = 'success'; releaseClose(); pendingKiller?.emit('close', 0);
    try { await previews.stopAll(); }
    finally {
      mock.mock.restore(); syncBuiltinESMExports();
      server.closeAllConnections(); await new Promise(resolve => server.close(resolve));
      assert(resolve(root).startsWith(resolve(tmpdir()) + '\\droprun-preview-stop-'));
      await rm(root, { recursive: true, force: true, maxRetries: 3, retryDelay: 100 });
    }
  });
  await writeFile(join(root, 'package.json'), JSON.stringify({ scripts: { dev: 'synthetic' } }));
  await previews.start('fixture', root, { script: 'dev', port: await freePort() });
  return { root, worker, previews, events, releaseClose, exitWorker, setOutcome: value => { outcome = value; }, get killers() { return killers; } };
}

test('same-project stop waits for owned close after taskkill exits and concurrent stop joins once', { skip: process.platform !== 'win32' }, async t => {
  const f = await fixture(t);
  let edited = false, joined = false;
  const stop = f.previews.stopLiveForCwd(f.root).then(() => { edited = true; });
  const concurrent = f.previews.stopAll().then(() => { joined = true; });
  await new Promise(resolve => setTimeout(resolve, 30));
  assert.equal(f.killers, 1); assert.equal(edited, false); assert.equal(joined, false);
  assert.equal(f.previews.get('fixture'), null); assert.equal(f.previews.renew('fixture'), null);
  assert.equal(f.events.length, 0); assert.equal(f.previews.active.has('fixture'), true);
  f.releaseClose(); await Promise.all([stop, concurrent]);
  assert.equal(edited, true); assert.equal(joined, true); assert.equal(f.events.length, 1);
  assert.equal(f.previews.active.size, 0);
});

test('taskkill error/nonzero and unknown timeout retain owned handles and block edits until an explicit retry', { skip: process.platform !== 'win32' }, async t => {
  const f = await fixture(t), entry = f.previews.active.get('fixture');
  for (const [outcome, expected] of [['error', /synthetic taskkill spawn failure/], ['nonzero', /终止失败/], ['timeout', /5 秒内未确认退出/]]) {
    f.setOutcome(outcome);
    let edited = false;
    await assert.rejects(f.previews.stopLiveForCwd(f.root).then(() => { edited = true; }), expected);
    assert.equal(edited, false); assert.equal(f.events.length, 0);
    assert.equal(f.previews.active.get('fixture'), entry); assert.equal(entry.processes[0], f.worker);
    assert.equal(f.previews.get('fixture'), null); assert.equal(entry.stopPromise, null);
  }
  f.setOutcome('success'); await f.previews.stopLiveForCwd(f.root);
  assert.equal(f.events.length, 1); assert.equal(f.previews.active.size, 0);
});

for (const signal of [null, 'SIGTERM']) test(`natural ${signal || 'successful'} exit waits for close without killing an exited PID or self-waiting`, { skip: process.platform !== 'win32' }, async t => {
  const f = await fixture(t);
  f.exitWorker(signal);
  const stop = f.previews.stopLiveForCwd(f.root);
  await new Promise(resolve => setTimeout(resolve, 30));
  assert.equal(f.events.length, 0); assert.equal(f.killers, 0);
  f.releaseClose(); await stop;
  assert.equal(f.events.length, 1); assert.equal(f.events[0].reason, 'process-exited');
  assert.equal(f.previews.active.size, 0);
});
