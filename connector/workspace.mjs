import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { copyFile, chmod, lstat, mkdir, readFile, readdir, realpath, writeFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { dirname, isAbsolute, join, relative, resolve, sep } from 'node:path';

const run = promisify(execFile);
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const generated = new Set(['.git', '.local', 'node_modules', '.gradle', '.next', '.venv', 'venv', '__pycache__', 'build', 'dist']);
const secret = name => name === '.env' || (name.startsWith('.env.') && !/\.(example|sample|template)$/.test(name)) || /\.(keystore|jks|pem|key)$/i.test(name);
function inside(root, path) { const rel = relative(root, path); return !isAbsolute(rel) && rel !== '..' && !rel.startsWith('..' + sep); }
async function git(cwd, args) {
  const { stdout } = await run('git', ['-c', 'core.fsmonitor=false', '-c', 'core.hooksPath=' + join(cwd, '.git', 'droprun-hooks-disabled'), '-C', cwd, ...args], { windowsHide: true, timeout: 60000, maxBuffer: 32 * 1024 * 1024 });
  return stdout;
}
async function files(source) {
  let isGit = false;
  try {
    await git(source, ['rev-parse', '--is-inside-work-tree']);
    isGit = true;
  } catch (error) {
    // A broken Git repository must not silently turn into an unfiltered copy.
    try { await lstat(join(source, '.git')); throw error; } catch (check) { if (check.code !== 'ENOENT') throw check; }
  }
  if (isGit) return { kind: 'git-snapshot', paths: [...new Set((await git(source, ['ls-files', '-z', '--cached', '--others', '--exclude-standard'])).split('\0').filter(Boolean))] };
  {
    const paths = [];
    async function walk(dir) {
      for (const entry of await readdir(dir, { withFileTypes: true })) {
        if (generated.has(entry.name) || secret(entry.name)) continue;
        const path = join(dir, entry.name);
        if (entry.isSymbolicLink()) throw new Error('项目包含符号链接，需先确认隔离策略：' + relative(source, path));
        if (entry.isDirectory()) await walk(path); else if (entry.isFile()) paths.push(relative(source, path));
        if (paths.length > 20000) throw new Error('项目超过 20000 个源文件，未启动自动修改。');
      }
    }
    await walk(source); return { kind: 'directory-snapshot', paths };
  }
}

export async function createWorkspace({ taskId, projectId, sourceCwd, baseDirectory }) {
  if (!/^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(taskId)) throw new Error('Invalid workspace task ID');
  const source = await realpath(sourceCwd);
  if (inside(source, resolve(baseDirectory))) throw new Error('隔离目录不能位于原项目内。');
  await mkdir(baseDirectory, { recursive: true });
  const base = await realpath(baseDirectory);
  if (inside(source, base)) throw new Error('隔离目录不能位于原项目内。');
  const directory = join(base, taskId), manifestPath = join(directory, 'manifest.json');
  try {
    const existing = JSON.parse(await readFile(manifestPath, 'utf8'));
    if (existing.projectId !== projectId || existing.sourceCwd !== source || existing.cwd !== join(directory, 'workspace')) throw new Error('隔离目录归属不一致。');
    const actual = await realpath(existing.cwd);
    if (actual !== existing.cwd || inside(source, actual)) throw new Error('隔离目录已被替换或指向原项目。');
    return existing;
  } catch (error) { if (error.code !== 'ENOENT') throw error; }
  // Refuse to overwrite a partially prepared or already edited workspace.
  await mkdir(directory);
  const cwd = join(directory, 'workspace'); await mkdir(cwd);
  const inventory = await files(source);
  if (inventory.paths.length > 20000) throw new Error('项目超过 20000 个源文件，未启动自动修改。');
  const baseline = [], omitted = [];
  let bytes = 0;
  for (const path of inventory.paths) {
    const input = resolve(source, path), output = resolve(cwd, path);
    if (!inside(source, input) || !inside(cwd, output) || path.split(/[\\/]/).includes('.git')) throw new Error('项目文件路径越界。');
    if (path.split(/[\\/]/).some(secret)) { omitted.push(path); continue; }
    let stat;
    try { stat = await lstat(input); } catch (error) { if (error.code === 'ENOENT') continue; throw error; }
    if (!stat.isFile() || stat.isSymbolicLink() || !inside(source, await realpath(input))) throw new Error('项目含子模块、链接或特殊文件，未自动复制：' + path);
    bytes += stat.size;
    if (bytes > 1024 * 1024 * 1024 || stat.size > 100 * 1024 * 1024) throw new Error('项目快照超过大小限制，未启动自动修改。');
    await mkdir(dirname(output), { recursive: true }); await copyFile(input, output); await chmod(output, stat.mode & 0o777);
    baseline.push({ path, sha256: hash(await readFile(output)) });
  }
  // A private baseline gives Codex normal git diff/status without touching the original repo or its index.
  await git(cwd, ['init', '--template=']);
  await git(cwd, ['add', '--all', '--force']);
  await git(cwd, ['-c', 'user.name=DropRun Snapshot', '-c', 'user.email=snapshot@localhost', '-c', 'commit.gpgSign=false', 'commit', '--allow-empty', '--no-verify', '-m', 'DropRun private workspace baseline']);
  const manifest = { taskId, projectId, sourceCwd: source, cwd, kind: inventory.kind, baselineCommit: (await git(cwd, ['rev-parse', 'HEAD'])).trim(), baseline, omitted, bytes, createdAt: new Date().toISOString() };
  await writeFile(manifestPath, JSON.stringify(manifest, null, 2));
  return manifest;
}

export function workspacePolicy(cwd) {
  return { type: 'workspaceWrite', writableRoots: [resolve(cwd)], networkAccess: true, excludeTmpdirEnvVar: true, excludeSlashTmp: true };
}

export async function inspectWorkspace(workspace) {
  const changed = (await git(workspace.cwd, ['diff', '--no-ext-diff', '--name-only', '-z', workspace.baselineCommit])).split('\0').filter(Boolean);
  const added = (await git(workspace.cwd, ['ls-files', '--others', '--exclude-standard', '-z'])).split('\0').filter(Boolean);
  const sourceChanged = [];
  for (const file of workspace.baseline) {
    try {
      const path = resolve(workspace.sourceCwd, file.path);
      if (!inside(workspace.sourceCwd, await realpath(path)) || hash(await readFile(path)) !== file.sha256) sourceChanged.push(file.path);
    } catch (error) { if (error.code === 'ENOENT') sourceChanged.push(file.path); else throw error; }
  }
  return { cwd: workspace.cwd, sourceCwd: workspace.sourceCwd, changedFiles: [...new Set([...changed, ...added])], sourceChanged, appliedToOriginal: false };
}
