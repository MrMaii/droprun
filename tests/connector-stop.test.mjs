import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { ConnectorWork } from '../connector/shutdown.mjs';
import { stopConnector } from '../scripts/setup.mjs';

const deferred = () => { let resolve; const promise = new Promise(accept => { resolve = accept; }); return { promise, resolve }; };
const config = { instanceId: 'synthetic-stop-instance', connectorToken: 'synthetic-local-owner-credential' };
const health = { service: 'DropRun Connector', instanceId: config.instanceId, shutdownProtocolVersion: 1, pid: 12345, activeTask: null };

for (const source of ['claim', 'recover']) test(`shutdown refuses pending ${source} and prevents later work after acceptance`, async () => {
  const work = new ConnectorWork(), receipt = deferred(), execution = deferred(), events = [];
  const polling = work.run(async () => {
    events.push(source); const task = await receipt.promise;
    events.push('execute:' + task.id); await execution.promise;
  });
  assert.equal(work.requestStop(), false); assert.equal(work.stopping, false);
  receipt.resolve({ id: 'synthetic-task' }); await Promise.resolve();
  assert.deepEqual(events, [source, 'execute:synthetic-task']);
  assert.equal(work.requestStop(), false);
  execution.resolve(); await polling;
  assert.equal(work.requestStop(), true);
  await work.run(() => events.push('late claim'));
  assert.deepEqual(events, [source, 'execute:synthetic-task']);
});

test('shutdown waits for every concurrent background/local write and survives a failed operation', async () => {
  const work = new ConnectorWork(), first = deferred(), second = deferred();
  const sync = work.run(() => first.promise), preview = work.run(() => second.promise);
  assert.equal(work.begin(), true); assert.equal(work.requestStop(), false);
  work.end(); first.resolve(); await sync;
  assert.equal(work.requestStop(), false);
  second.resolve(); await preview;
  await assert.rejects(work.run(() => { throw new Error('synthetic write failure'); }), /synthetic/);
  assert.equal(work.busy, false); assert.equal(work.requestStop(), true);
  assert.equal(work.begin(), false);
});

test('stop acknowledgement is not completion: wait for the identified harmless worker process to exit', async t => {
  const child = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { windowsHide: true, stdio: 'ignore' });
  t.after(() => { if (child.exitCode === null) child.kill(); });
  await once(child, 'spawn');
  const exited = once(child, 'exit'), requested = deferred(), calls = [];
  let completed = false;
  const result = stopConnector(config, { timeout: 5000, fetchImpl: async (url, options) => {
    calls.push({ url, options });
    if (url.endsWith('/management/stop')) { requested.resolve(); return Response.json({ stopping: true }, { status: 202 }); }
    return Response.json({ ...health, pid: child.pid });
  } }).then(() => { completed = true; });
  await requested.promise; await new Promise(resolve => setTimeout(resolve, 120));
  assert.equal(completed, false); child.kill(); await exited; await result;
  assert.equal(completed, true); assert.equal(calls.length, 2);
  assert.equal(calls[1].options.headers.Authorization, 'Bearer ' + config.connectorToken);
});

test('unknown health, foreign identity and active work never send an owner stop request', async () => {
  const cases = [
    () => { throw Object.assign(new Error('synthetic timeout'), { name: 'TimeoutError' }); },
    () => new Response('invalid JSON'),
    () => new Response('{}', { status: 503 }),
    () => Response.json({ ...health, service: 'Other service' }),
    () => Response.json({ ...health, instanceId: 'other-instance' }),
    () => Response.json({ ...health, shutdownProtocolVersion: undefined }),
    () => Response.json({ ...health, pid: null }),
    () => Response.json({ ...health, activeTask: 'synthetic-active-task' })
  ];
  for (const response of cases) {
    const calls = [];
    await assert.rejects(stopConnector(config, { fetchImpl: async url => { calls.push(url); return response(); } }));
    assert.deepEqual(calls, ['http://127.0.0.1:47493']);
  }
});

test('only connection refusal proves there is no listening worker', async () => {
  for (const error of [Object.assign(new Error('refused'), { code: 'ECONNREFUSED' }), new TypeError('fetch failed', { cause: { code: 'ECONNREFUSED' } })]) {
    let calls = 0;
    await stopConnector(config, { fetchImpl: async () => { calls++; throw error; } });
    assert.equal(calls, 1);
  }
  await assert.rejects(stopConnector(config, { fetchImpl: async () => { throw Object.assign(new Error('reset'), { code: 'ECONNRESET' }); } }), /Could not verify/);
});

test('busy stop, invalid acknowledgement, unknown process and exit timeout fail closed', async () => {
  for (const response of [Response.json({ error: 'busy' }, { status: 409 }), Response.json({ stopping: true }), Response.json({ stopping: false }, { status: 202 })]) {
    let probes = 0;
    await assert.rejects(stopConnector(config, { fetchImpl: async url => url.endsWith('/management/stop') ? response : Response.json(health), alive: () => { probes++; return false; } }), /not be stopped safely/);
    assert.equal(probes, 0);
  }
  const fetchImpl = async url => url.endsWith('/management/stop') ? Response.json({ stopping: true }, { status: 202 }) : Response.json(health);
  await assert.rejects(stopConnector(config, { fetchImpl, alive: () => { throw new Error('synthetic process query failure'); } }), /process query failure/);
  await assert.rejects(stopConnector(config, { fetchImpl, alive: () => true, timeout: 5 }), /not finished stopping/);
});
