import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, cp, writeFile, readFile, readdir, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';

// Test export decisions with fake compiler/signer tools. Real signing is a separate gate.
const windows = process.platform === 'win32';
async function fixture(t) {
  const root = await mkdtemp(join(tmpdir(), 'droprun-android-package-'));
  t.after(async () => {
    assert.equal(resolve(root).startsWith(resolve(tmpdir()) + '\\droprun-android-package-'), true);
    await rm(root, { recursive: true, force: true });
  });
  const git = (...args) => {
    const result = spawnSync('git', ['-C', root, ...args], { encoding: 'utf8' });
    assert.equal(result.status, 0, result.stderr); return result.stdout.trim();
  };
  for (const dir of ['scripts', 'licenses/android', 'android/app/build/outputs/apk/release', '.local/sdk/build-tools/35.0.0']) await mkdir(join(root, dir), { recursive: true });
  await cp(new URL('../scripts/build-android.ps1', import.meta.url), join(root, 'scripts/build-android.ps1'));
  await writeFile(join(root, '.gitignore'), '.local/\nandroid/app/build/\n');
  for (const file of ['LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md', 'licenses/android/ZXing-LICENSE.txt']) await writeFile(join(root, file), 'Synthetic license fixture\n');
  const compiler = join(root, 'android/gradlew.bat');
  await writeFile(compiler, '@exit /b 0\r\n');
  const release = join(root, 'android/app/build/outputs/apk/release');
  await writeFile(join(release, 'app-release.apk'), 'Synthetic APK bytes, not installable');
  await writeFile(join(release, 'output-metadata.json'), JSON.stringify({ applicationId: 'app.droprun.mobile', variantName: 'release', elements: [{ versionName: '9.8.7', versionCode: 987, outputFile: 'app-release.apk' }] }));
  const signer = join(root, '.local/sdk/build-tools/35.0.0/apksigner.bat');
  await writeFile(signer, '@echo Signer #1 certificate SHA-256 digest: ' + 'a'.repeat(64) + '\r\n@exit /b 0\r\n');
  git('init', '-q'); git('add', '.'); git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'Fixture');
  const output = join(root, '.local/output');
  const run = () => spawnSync('pwsh', ['-NoProfile', '-File', join(root, 'scripts/build-android.ps1'), '-OutputDirectory', '.local/output'], {
    cwd: root, encoding: 'utf8', env: { ...process.env, ANDROID_HOME: join(root, '.local/sdk'), DROPRUN_KEYSTORE: 'fixture', DROPRUN_KEYSTORE_PASSWORD: 'fixture', DROPRUN_KEY_ALIAS: 'fixture', DROPRUN_KEY_PASSWORD: 'fixture' },
  });
  return { root, output, run, git, compiler, signer };
}
test('Android export uses built version and includes verified inventory, source and licenses; refuses overwrite', { skip: !windows }, async t => {
  const f = await fixture(t), result = f.run(); assert.equal(result.status, 0, result.stdout + result.stderr);
  const manifest = JSON.parse(await readFile(join(f.output, 'BUILD-MANIFEST.json'), 'utf8'));
  assert.equal(manifest.version, '9.8.7'); assert.equal(manifest.versionCode, 987);
  assert.equal(manifest.sourceCommit, f.git('rev-parse', 'HEAD'));
  assert.equal(manifest.certificateSha256, 'a'.repeat(64));
  assert(manifest.files.some(file => file.path === 'DropRun-9.8.7-android-source.zip'));
  assert(manifest.files.some(file => file.path === 'licenses/android/ZXing-LICENSE.txt'));
  for (const file of manifest.files) {
    const bytes = await readFile(join(f.output, file.path));
    assert.equal(file.bytes, bytes.length); assert.equal(file.sha256, createHash('sha256').update(bytes).digest('hex'));
  }
  const sums = await readFile(join(f.output, 'SHA256SUMS'), 'utf8');
  assert.match(sums, /  DropRun-9\.8\.7-android\.apk/); assert.match(sums, /  BUILD-MANIFEST\.json/);
  const again = f.run(); assert.notEqual(again.status, 0); assert.match(again.stderr, /already exists/);
  assert.equal(await readFile(join(f.output, 'SHA256SUMS'), 'utf8'), sums);
});
test('Android export rejects dirty source and failed compiler without creating a package', { skip: !windows }, async t => {
  const f = await fixture(t);
  await writeFile(f.compiler, '@exit /b 7\r\n');
  const dirty = f.run(); assert.notEqual(dirty.status, 0); assert.match(dirty.stderr, /clean Git commit/);
  f.git('add', '.'); f.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'Fail compiler');
  const failed = f.run(); assert.notEqual(failed.status, 0); assert.match(failed.stderr, /Android build failed/);
  assert(!(await readdir(join(f.root, '.local'))).includes('output'));
});
test('Android export rejects a failed signature before exposing a package', { skip: !windows }, async t => {
  const f = await fixture(t); await writeFile(f.signer, '@exit /b 1\r\n');
  const failed = f.run(); assert.notEqual(failed.status, 0); assert.match(failed.stderr, /signature verification failed/);
  assert(!(await readdir(join(f.root, '.local'))).includes('output'));
});
