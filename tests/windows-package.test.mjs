import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, rm, readdir, copyFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve, relative } from 'node:path';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { packageWindows } from '../scripts/package-windows.mjs';
import { WRANGLER_VERSION } from '../scripts/setup-core.mjs';

const windows = process.platform === 'win32' && process.arch === 'x64';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
async function fixture(t) {
  const root = await mkdtemp(join(tmpdir(), 'droprun-windows-package-'));
  t.after(async () => {
    assert(resolve(root).startsWith(resolve(tmpdir()) + '\\droprun-windows-package-'));
    await rm(root, { recursive: true, force: true });
  });
  const git = (...args) => execFileSync('git', ['-C', root, ...args], { encoding: 'utf8', windowsHide: true }).trim();
  for (const dir of ['connector', 'relay', 'installer', 'scripts']) {
    await mkdir(join(root, dir), { recursive: true }); await writeFile(join(root, dir, 'fixture.txt'), 'Committed source\n');
  }
  for (const name of ['setup.mjs', 'setup-core.mjs', 'setup-tools.mjs', 'connector-service.ps1', 'install-connector-service.ps1']) await writeFile(join(root, 'scripts', name), '// Fixture\n');
  await copyFile(resolve('installer/droprun.iss'), join(root, 'installer/droprun.iss'));
  for (const name of ['qrcode', 'cloudflared', 'wrangler']) {
    await mkdir(join(root, 'node_modules', name), { recursive: true });
    await writeFile(join(root, 'node_modules', name, 'package.json'), JSON.stringify({ name, version: name === 'wrangler' ? WRANGLER_VERSION : '1.0.0' }));
    await writeFile(join(root, 'node_modules', name, 'LICENSE'), 'Fixture license\n');
  }
  await writeFile(join(root, 'package.json'), JSON.stringify({ name: 'droprun', version: '9.8.7' }));
  await writeFile(join(root, '.gitignore'), '.local/\nnode_modules/\n*.env\n');
  await writeFile(join(root, 'LICENSE'), 'Fixture license\n'); await writeFile(join(root, 'NOTICE'), 'Fixture notice\n');
  git('init', '-q'); git('add', '.'); git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'Fixture');
  await writeFile(join(root, 'connector', 'private.env'), 'LOCAL_ONLY_SENTINEL');
  t.mock.method(globalThis, 'fetch', async () => new Response('Synthetic Node license, not for distribution'));
  return { root, output: join(root, '.local/releases'), git };
}
test('Windows package inventories every payload file and ships only committed project sources with matching archive', { skip: !windows }, async t => {
  const f = await fixture(t), result = await packageWindows(f);
  const manifest = JSON.parse(await readFile(join(result.directory, 'BUILD-MANIFEST.json'), 'utf8'));
  assert.equal(manifest.sourceCommit, f.git('rev-parse', 'HEAD'));
  assert.match(await readFile(join(result.directory, 'DropRun.cmd'), 'utf8'), /powershell\.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "%~dp0installer\\launch\.ps1"/);
  const installerLaunches = (await readFile(join(result.directory, 'installer/droprun.iss'), 'utf8')).split(/\r?\n/).filter(line => line.includes('powershell.exe'));
  assert.equal(installerLaunches.length, 4);
  for (const line of installerLaunches) assert.match(line, /-ExecutionPolicy Bypass -File /);
  const diskFiles = (await readdir(result.directory, { recursive: true, withFileTypes: true })).filter(e => e.isFile()).map(e => relative(result.directory, join(e.parentPath, e.name)).replaceAll('\\', '/')).filter(p => p !== 'BUILD-MANIFEST.json').sort();
  assert.deepEqual(manifest.files.map(e => e.path).sort(), diskFiles);
  assert(diskFiles.includes('installer/zip-package.ps1')); assert(!diskFiles.includes('connector/private.env'));
  for (const file of manifest.files) assert.equal(hash(await readFile(join(result.directory, file.path))), file.sha256);
  assert.equal(hash(await readFile(result.sourceArchive)), manifest.sourceArchive.sha256);
  for (const file of [result.zip, result.sourceArchive]) assert((await readFile(file + '.sha256', 'utf8')).startsWith(hash(await readFile(file)) + '  '));
  const zipPaths = file => JSON.parse(execFileSync('powershell.exe', ['-NoProfile', '-Command', 'Add-Type -AssemblyName System.IO.Compression.FileSystem; $zip=[IO.Compression.ZipFile]::OpenRead($env:PACKAGE_TEST_ZIP); ConvertTo-Json -InputObject @($zip.Entries | ForEach-Object FullName) -Compress; $zip.Dispose()'], { encoding: 'utf8', windowsHide: true, env: { ...process.env, PACKAGE_TEST_ZIP: file } }));
  const sourcePaths = zipPaths(result.sourceArchive); assert(sourcePaths.includes('connector/fixture.txt')); assert(!sourcePaths.some(p => p.endsWith('.env') || p.startsWith('node_modules/')));
  const payload = zipPaths(result.zip).map(p => p.replaceAll('\\', '/')).filter(p => !p.endsWith('/')).map(p => p.split('/').slice(1).join('/')).sort();
  assert.deepEqual(payload, [...diskFiles, 'BUILD-MANIFEST.json'].sort());
});
test('Windows package rejects dirty sources and existing standalone artifacts before creating output', { skip: !windows }, async t => {
  const f = await fixture(t);
  await writeFile(join(f.root, 'connector', 'fixture.txt'), 'Uncommitted change');
  await assert.rejects(packageWindows(f), /clean Git commit/);
  f.git('restore', 'connector/fixture.txt');
  await mkdir(f.output, { recursive: true });
  const installer = join(f.output, 'DropRun-9.8.7-windows-x64-setup.exe'); await writeFile(installer, 'Existing installer');
  await assert.rejects(packageWindows({ ...f, compiler: 'unused-compiler.exe' }), /already exists/);
  assert.equal(await readFile(installer, 'utf8'), 'Existing installer');
  assert.deepEqual(await readdir(f.output), ['DropRun-9.8.7-windows-x64-setup.exe']);
});
