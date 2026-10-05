import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, readdir, rm, copyFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

test('Windows update preserves old application/data and rejects invalid Relay before replacement', { skip: process.platform !== 'win32' }, async t => {
  const root = await mkdtemp(join(tmpdir(), 'droprun-update-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const app = join(root, 'installed'), local = join(root, 'local'), data = join(local, 'DropRun');
  await mkdir(app); await mkdir(data, { recursive: true });
  await writeFile(join(app, 'old.txt'), 'previous application');
  await writeFile(join(data, 'state.json'), '{"retained":true}');
  const run = () => promisify(execFile)('powershell.exe', ['-NoProfile', '-NonInteractive', '-File', resolve('installer/preflight.ps1'), '-InstallRoot', app], { env: { ...process.env, LOCALAPPDATA: local }, windowsHide: true });
  await run();
  const backups = await readdir(join(local, 'DropRun-backups'));
  assert.equal(backups.length, 1);
  const backup = join(local, 'DropRun-backups', backups[0]);
  assert.equal(await readFile(join(backup, 'app/old.txt'), 'utf8'), 'previous application');
  assert.equal(await readFile(join(backup, 'data/state.json'), 'utf8'), '{"retained":true}');
  assert.equal(JSON.parse((await readFile(join(backup, 'backup.json'), 'utf8')).replace(/^\uFEFF/, '')).complete, true);
  await writeFile(join(data, 'config.json'), JSON.stringify({ relay: 'http://127.0.0.1/', instanceId: 'fixture' }));
  await assert.rejects(run(), /Invalid saved Relay origin/);
  assert.equal((await readdir(join(local, 'DropRun-backups'))).length, 1);
  assert.equal(await readFile(join(app, 'old.txt'), 'utf8'), 'previous application');
  // Exercise remote compatibility and active-work gates without contacting a user instance.
  await writeFile(join(data, 'config.json'), JSON.stringify({ relay: 'https://relay.example.test/', instanceId: 'fixture' }));
  const wrapper = join(root, 'remote-fixture.ps1');
  await writeFile(wrapper, `param([string]$Script,[string]$App)
function Invoke-RestMethod { param($Uri,$TimeoutSec)
  if ($Uri -like 'https:*') { return ($env:DROPRUN_TEST_RELAY | ConvertFrom-Json) }
  return @{ service = 'DropRun Connector'; instanceId = 'fixture'; activeTask = 'active-task' }
}
function Get-ScheduledTask { return $null }
& $Script -InstallRoot $App
exit $LASTEXITCODE
`);
  for (const [schemaVersion, expected] of [[10, /incompatible/], [11, /active DropRun task/]]) {
    await assert.rejects(promisify(execFile)('powershell.exe', ['-NoProfile', '-NonInteractive', '-File', wrapper, '-Script', resolve('installer/preflight.ps1'), '-App', app], {
      env: { ...process.env, LOCALAPPDATA: local, DROPRUN_TEST_RELAY: JSON.stringify({ instanceId: 'fixture', protocolVersion: 2, schemaVersion, ready: true }) }, windowsHide: true
    }), expected);
  }
  assert.equal((await readdir(join(local, 'DropRun-backups'))).length, 1);
});

test('new update/uninstall verifier preserves schedule on failed stop and bypasses the old setup helper', { skip: process.platform !== 'win32' }, async t => {
  const root = await mkdtemp(join(tmpdir(), 'droprun-stop-install-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const app = join(root, 'installed'), local = join(root, 'local'), data = join(local, 'DropRun'), log = join(root, 'events.txt');
  await mkdir(join(app, 'runtime'), { recursive: true }); await mkdir(join(app, 'scripts')); await mkdir(join(app, 'connector')); await mkdir(join(app, 'installer')); await mkdir(data, { recursive: true });
  await copyFile(process.execPath, join(app, 'runtime/node.exe'));
  await writeFile(join(data, 'config.json'), JSON.stringify({ relay: 'https://relay.example.test/', instanceId: 'fixture' }));
  await writeFile(join(app, 'old.txt'), 'previous application');
  await writeFile(join(app, 'scripts/setup.mjs'), 'throw Error("The old installed setup helper must not run");');
  // This synthetic config loader installs a fake transport before the extracted
  // real verifier runs. No test connects to the real local Connector or Relay.
  await writeFile(join(app, 'connector/config.mjs'), `import { appendFileSync } from 'node:fs';
export async function loadRuntimeConfig() {
  globalThis.fetch = async url => {
    if (!url.endsWith('/management/stop')) {
      if (process.env.DROPRUN_TEST_HEALTH === 'timeout') throw new Error('synthetic timeout');
      if (process.env.DROPRUN_TEST_HEALTH === 'refused') throw Object.assign(new Error('synthetic refused'), { code: 'ECONNREFUSED' });
      return Response.json(JSON.parse(process.env.DROPRUN_TEST_HEALTH));
    }
    appendFileSync(process.env.DROPRUN_TEST_LOG, 'verified-stop\\n');
    return Response.json({ stopping: process.env.DROPRUN_TEST_BUSY !== 'true' }, { status: process.env.DROPRUN_TEST_BUSY === 'true' ? 409 : 202 });
  };
  return { config: { instanceId: 'fixture', connectorToken: 'synthetic-only' } };
}
`);
  const helper = join(root, 'extracted-shutdown.mjs');
  await copyFile(resolve('connector/shutdown.mjs'), helper);
  await copyFile(helper, join(app, 'connector/shutdown.mjs'));
  await copyFile(resolve('installer/uninstall.ps1'), join(app, 'installer/uninstall.ps1'));
  const wrapper = join(root, 'fixture.ps1');
  await writeFile(wrapper, `param([string]$Script,[string]$App,[string]$Data,[string]$Helper,[string]$Kind)
function Invoke-RestMethod { param($Uri,$TimeoutSec)
  if ($Uri -like 'https:*') { return @{ instanceId='fixture'; protocolVersion=2; schemaVersion=11; ready=$true } }
  if ($env:DROPRUN_TEST_HEALTH -in @('timeout','refused')) { throw 'Synthetic unavailable health' }
  return ($env:DROPRUN_TEST_HEALTH | ConvertFrom-Json)
}
function Get-ScheduledTask { param($TaskName,$ErrorAction)
  return @{ TaskName=$TaskName; State=$env:DROPRUN_TEST_SCHEDULE }
}
function Stop-ScheduledTask { param($TaskName) Add-Content -LiteralPath $env:DROPRUN_TEST_LOG -Value 'stop-schedule' }
function Unregister-ScheduledTask { param($TaskName,$Confirm) Add-Content -LiteralPath $env:DROPRUN_TEST_LOG -Value 'unregister-schedule' }
if ($Kind -eq 'update') { & $Script -InstallRoot $App -StopHelper $Helper } else { & $Script -DataDir $Data }
exit $LASTEXITCODE
`);
  let missingPid = 999999;
  for (;;) { try { process.kill(missingPid, 0); missingPid++; } catch (error) { if (error.code === 'ESRCH') break; throw error; } }
  const healthy = { service: 'DropRun Connector', instanceId: 'fixture', shutdownProtocolVersion: 1, pid: missingPid, activeTask: null };
  const run = (kind, health, schedule = 'Ready', busy = false) => promisify(execFile)('powershell.exe', ['-NoProfile', '-NonInteractive', '-File', wrapper, '-Script', kind === 'update' ? resolve('installer/preflight.ps1') : join(app, 'installer/uninstall.ps1'), '-App', app, '-Data', data, '-Helper', helper, '-Kind', kind], {
    env: { ...process.env, LOCALAPPDATA: local, DROPRUN_TEST_LOG: log, DROPRUN_TEST_HEALTH: typeof health === 'string' ? health : JSON.stringify(health), DROPRUN_TEST_SCHEDULE: schedule, DROPRUN_TEST_BUSY: String(busy) }, windowsHide: true
  });
  for (const kind of ['update', 'uninstall']) {
    for (const [value, schedule, busy] of [['timeout', 'Ready', false], ['refused', 'Running', false], [{ ...healthy, instanceId: 'other' }, 'Ready', false], [{ ...healthy, shutdownProtocolVersion: undefined }, 'Ready', false], [healthy, 'Ready', true]]) {
      await writeFile(log, '');
      await assert.rejects(run(kind, value, schedule, busy));
      const events = await readFile(log, 'utf8');
      assert.doesNotMatch(events, /stop-schedule|unregister-schedule/);
      assert.equal(await readFile(join(app, 'old.txt'), 'utf8'), 'previous application');
      await assert.rejects(readdir(join(local, 'DropRun-backups')), { code: 'ENOENT' });
    }
  }
  await writeFile(log, ''); await run('update', healthy);
  assert.deepEqual((await readFile(log, 'utf8')).trim().split(/\r?\n/), ['verified-stop', 'stop-schedule']);
  assert.equal((await readdir(join(local, 'DropRun-backups'))).length, 1);
  await writeFile(log, ''); await run('uninstall', healthy);
  assert.deepEqual((await readFile(log, 'utf8')).trim().split(/\r?\n/), ['verified-stop', 'stop-schedule', 'unregister-schedule']);
  await writeFile(log, ''); await run('uninstall', 'refused');
  assert.deepEqual((await readFile(log, 'utf8')).trim().split(/\r?\n/), ['stop-schedule', 'unregister-schedule']);
});

test('Connector supervisor exits on intentional successful shutdown without restarting', { skip: process.platform !== 'win32' }, async t => {
  const root = await mkdtemp(join(tmpdir(), 'droprun-stop-supervisor-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const app = join(root, 'app'), data = join(root, 'data'), log = join(root, 'events.txt');
  await mkdir(join(app, 'runtime'), { recursive: true }); await mkdir(join(app, 'connector')); await mkdir(data); await writeFile(join(data, 'config.json'), '{}');
  await copyFile(process.execPath, join(app, 'runtime/node.exe'));
  await writeFile(join(app, 'connector/main.mjs'), `import { appendFileSync } from 'node:fs'; appendFileSync(process.env.DROPRUN_TEST_LOG, 'worker-executed\\n'); process.exit(0);`);
  const wrapper = join(root, 'fixture.ps1');
  await writeFile(wrapper, `param([string]$Script,[string]$App,[string]$Data)
function Invoke-RestMethod { throw 'Synthetic no listening worker' }
function Start-Sleep { throw 'An intentional successful stop must not restart' }
& $Script -DataDir $Data -InstallRoot $App
exit $LASTEXITCODE
`);
  await promisify(execFile)('powershell.exe', ['-NoProfile', '-NonInteractive', '-File', wrapper, '-Script', resolve('scripts/connector-service.ps1'), '-App', app, '-Data', data], { env: { ...process.env, DROPRUN_TEST_LOG: log }, windowsHide: true });
  assert.deepEqual((await readFile(log, 'utf8')).trim().split(/\r?\n/), ['worker-executed']);
});
