import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

test('every owned Windows script entry sets only its child process execution policy', async () => {
  const files = ['scripts/package-windows.mjs', 'installer/droprun.iss', 'scripts/install-connector-service.ps1', 'scripts/setup.mjs', 'scripts/setup-tools.mjs'];
  let count = 0;
  for (const file of files) {
    const source = await readFile(resolve(file), 'utf8');
    const launches = source.split(/\r?\n/).filter(line => line.includes('powershell.exe') && line.includes('-File'));
    count += launches.length;
    for (const line of launches) assert.match(line, /-ExecutionPolicy(?: Bypass |', 'Bypass', ')-File/, file);
    assert.doesNotMatch(source, /Set-ExecutionPolicy/, file);
  }
  assert.equal(count, 9);
});

test('owned script flags launch an inert script from a Restricted parent without changing persistent policies', { skip: process.platform !== 'win32' }, async t => {
  const root = await mkdtemp(join(tmpdir(), 'droprun-launch-policy-'));
  t.after(async () => {
    assert(resolve(root).startsWith(resolve(tmpdir()) + '\\droprun-launch-policy-'));
    await rm(root, { recursive: true, force: true });
  });
  const script = join(root, 'inert fixture.ps1');
  await writeFile(script, "@{ marker='DropRun inert fixture'; policy=(Get-ExecutionPolicy -Scope Process).ToString() } | ConvertTo-Json -Compress\n");
  const source = await readFile(resolve('scripts/package-windows.mjs'), 'utf8');
  const flags = source.match(/powershell\.exe ([^\r\n]+?) -File/)[1];
  assert.equal(flags, '-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass');
  // Windows PowerShell must load its own modules rather than an inherited PowerShell 7 runtime.
  const env = Object.fromEntries(Object.entries(process.env).filter(([key]) => key.toLowerCase() !== 'psmodulepath'));
  const parent = `
$managed = @(Get-ExecutionPolicy -List | Where-Object { $_.Scope -in @('MachinePolicy','UserPolicy') -and $_.ExecutionPolicy -ne 'Undefined' })
if ($managed.Count) { @{ managed=$true } | ConvertTo-Json -Compress; exit 0 }
$before = Get-ExecutionPolicy -List | ConvertTo-Json -Compress
& powershell.exe -NoProfile -NonInteractive -File $env:DROPRUN_TEST_SCRIPT 2>$null | Out-Null
$controlCode = $LASTEXITCODE
$flags = $env:DROPRUN_TEST_FLAGS.Split(' ')
$child = & powershell.exe @flags -File $env:DROPRUN_TEST_SCRIPT
$childCode = $LASTEXITCODE
@{ managed=$false; controlCode=$controlCode; childCode=$childCode; child=($child | ConvertFrom-Json); parent=(Get-ExecutionPolicy -Scope Process).ToString(); before=$before; after=(Get-ExecutionPolicy -List | ConvertTo-Json -Compress) } | ConvertTo-Json -Depth 4 -Compress
`;
  const { stdout } = await promisify(execFile)('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Restricted', '-Command', parent], {
    env: { ...env, DROPRUN_TEST_SCRIPT: script, DROPRUN_TEST_FLAGS: flags }, windowsHide: true, timeout: 30000
  });
  const result = JSON.parse(stdout.trim());
  if (result.managed) { t.skip('Enforced Group Policy takes precedence over a process execution policy.'); return; }
  assert.notEqual(result.controlCode, 0, 'The unmodified -File launch must be blocked by Restricted.');
  assert.equal(result.childCode, 0);
  assert.deepEqual(result.child, { marker: 'DropRun inert fixture', policy: 'Bypass' });
  assert.equal(result.parent, 'Restricted');
  assert.equal(result.after, result.before);
});
