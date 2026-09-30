import { cp, mkdir, readFile, writeFile, readdir, stat } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join, resolve, dirname, relative } from 'node:path';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';
import { command, WRANGLER_VERSION } from './setup-core.mjs';

export async function packageWindows({ root = resolve(import.meta.dirname, '..'), output = join(root, '.local', 'releases'), compiler = null } = {}) {
  if (process.platform !== 'win32' || process.arch !== 'x64') throw new Error('Build the Windows x64 package using Windows x64 Node.');
  root = resolve(root); output = resolve(output);
  const git = (...args) => execFileSync('git', ['-C', root, ...args], { encoding: 'utf8', windowsHide: true }).trim();
  const sourceCommit = git('rev-parse', 'HEAD');
  if (git('status', '--porcelain')) throw new Error('Package from a clean Git commit so source and binaries correspond.');
  const pkg = JSON.parse(await readFile(join(root, 'package.json'), 'utf8'));
  const name = `DropRun-${pkg.version}-windows-x64`, directory = join(resolve(output), name);
  const zip = directory + '.zip', sourceArchive = directory + '-source.zip';
  const installer = compiler ? join(output, `${name}-setup.exe`) : null;
  if ([directory, zip, sourceArchive, zip + '.sha256', sourceArchive + '.sha256', installer, installer && installer + '.sha256'].filter(Boolean).some(existsSync)) throw new Error('Package output already exists. Choose a new --output directory; existing artifacts are never overwritten.');
  const wrangler = JSON.parse(await readFile(join(root, 'node_modules/wrangler/package.json'), 'utf8'));
  if (wrangler.version !== WRANGLER_VERSION) throw new Error('Install the pinned Wrangler version before building.');
  await mkdir(join(directory, 'runtime'), { recursive: true });
  const sourcePaths = ['connector', 'relay', 'installer', ...['setup.mjs', 'setup-core.mjs', 'setup-tools.mjs', 'connector-service.ps1', 'install-connector-service.ps1'].map(name => 'scripts/' + name)];
  for (const file of git('ls-files', '-z', '--', ...sourcePaths).split('\0').filter(Boolean)) {
    const target = join(directory, file); await mkdir(dirname(target), { recursive: true }); await cp(join(root, file), target);
  }
  await cp(process.execPath, join(directory, 'runtime', 'node.exe'));
  await writeFile(join(directory, 'package.json'), JSON.stringify({ name: 'droprun', version: pkg.version, private: true, type: 'module' }, null, 2));
  await writeFile(join(directory, 'DropRun.cmd'), '@echo off\r\npowershell.exe -NoProfile -WindowStyle Hidden -File "%~dp0installer\\launch.ps1"\r\n');
  const copied = new Set();
  async function copyPackage(name, parent = root, optional = false) {
    let search = parent, source;
    while (search.startsWith(root)) {
      const candidate = join(search, 'node_modules', name);
      if (existsSync(join(candidate, 'package.json'))) { source = candidate; break; }
      const next = dirname(search); if (next === search) break; search = next;
    }
    if (!source) { if (optional) return; throw new Error('Missing package dependency: ' + name); }
    if (copied.has(source)) return;
    copied.add(source);
    const metadata = JSON.parse(await readFile(join(source, 'package.json'), 'utf8'));
    const target = join(directory, relative(root, source));
    await cp(source, target, { recursive: true });
    for (const dep of Object.keys(metadata.dependencies || {})) await copyPackage(dep, source, !!metadata.optionalDependencies?.[dep]);
    for (const dep of Object.keys(metadata.optionalDependencies || {})) await copyPackage(dep, source, true);
  }
  for (const name of ['qrcode', 'cloudflared', 'wrangler']) await copyPackage(name);
  const license = await fetch(`https://raw.githubusercontent.com/nodejs/node/${process.version}/LICENSE`, { signal: AbortSignal.timeout(30000) });
  if (!license.ok) throw new Error('Could not fetch the license for the bundled Node runtime.');
  await writeFile(join(directory, 'runtime/LICENSE.txt'), await license.text());
  if (existsSync(join(root, 'LICENSE'))) await cp(join(root, 'LICENSE'), join(directory, 'LICENSE'));
  else throw new Error('The project LICENSE is required before distribution.');
  if (existsSync(join(root, 'NOTICE'))) await cp(join(root, 'NOTICE'), join(directory, 'NOTICE'));
  await writeFile(join(directory, 'THIRD-PARTY-NOTICES.txt'), 'Node.js: runtime/LICENSE.txt. npm dependency licenses are retained alongside each package in node_modules. Cloudflared retains its upstream license. Media tools are downloaded separately from their upstream projects by setup; they are not part of this Apache-2.0 application package.\n');
  const zipScript = join(directory, 'installer', 'zip-package.ps1');
  await writeFile(zipScript, 'param([string]$Source,[string]$Destination)\nAdd-Type -AssemblyName System.IO.Compression.FileSystem\n[IO.Compression.ZipFile]::CreateFromDirectory($Source,$Destination,[IO.Compression.CompressionLevel]::Optimal,$true)\n');
  if (git('status', '--porcelain') || git('rev-parse', 'HEAD') !== sourceCommit) throw new Error('Source changed during packaging; do not distribute this incomplete directory.');
  git('-c', 'core.autocrlf=false', '-c', 'core.eol=lf', 'archive', '--format=zip', '--output=' + sourceArchive, sourceCommit);
  const sourceSha256 = createHash('sha256').update(await readFile(sourceArchive)).digest('hex');
  await writeFile(sourceArchive + '.sha256', sourceSha256 + '  ' + name + '-source.zip\n');
  const files = [];
  async function inventory(dir) { for (const name of (await readdir(dir)).sort()) { const file = join(dir, name), info = await stat(file); if (info.isDirectory()) await inventory(file); else files.push({ path: relative(directory, file).replaceAll('\\', '/'), bytes: info.size, sha256: createHash('sha256').update(await readFile(file)).digest('hex') }); } }
  await inventory(directory);
  await writeFile(join(directory, 'BUILD-MANIFEST.json'), JSON.stringify({ version: pkg.version, sourceCommit, sourceArchive: { file: name + '-source.zip', sha256: sourceSha256 }, node: process.version, wrangler: WRANGLER_VERSION, builtAt: new Date().toISOString(), files }, null, 2));
  await command('powershell.exe', ['-NoProfile', '-NonInteractive', '-File', zipScript, '-Source', directory, '-Destination', zip], { timeout: 300000 });
  await writeFile(zip + '.sha256', createHash('sha256').update(await readFile(zip)).digest('hex') + '  ' + name + '.zip\n');
  if (compiler) {
    await command(resolve(compiler), [`/DPackageDir=${directory}`, `/DAppVersion=${pkg.version}`, join(directory, 'installer/droprun.iss')], { timeout: 300000 });
    await writeFile(installer + '.sha256', createHash('sha256').update(await readFile(installer)).digest('hex') + '  ' + name + '-setup.exe\n');
  }
  return { directory, zip, sourceArchive, fileCount: files.length, bytes: (await stat(zip)).size, installer };
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const get = name => { const i = process.argv.indexOf(name); return i < 0 ? undefined : process.argv[i + 1]; };
  packageWindows({ output: get('--output'), compiler: get('--iscc') }).then(result => console.log(JSON.stringify(result, null, 2))).catch(error => { console.error(error.message); process.exitCode = 1; });
}
