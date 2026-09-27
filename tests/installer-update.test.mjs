import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, readdir, rm } from 'node:fs/promises';
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
  return @{ instanceId = 'fixture'; activeTask = 'active-task' }
}
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
