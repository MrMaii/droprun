import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile, symlink } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { createWorkspace, inspectWorkspace, workspacePolicy } from '../connector/workspace.mjs';
const git = (cwd, args) => execFileSync('git', ['-C', cwd, ...args], { encoding: 'utf8', windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
async function fixture() {
  const root = await mkdtemp(join(tmpdir(), 'droprun-workspace-test-'));
  const source = join(root, 'source'); await mkdir(source);
  return { root, source, options: { taskId: randomUUID(), projectId: randomUUID(), sourceCwd: source, baseDirectory: join(root, 'isolated') } };
}
test('snapshot includes dirty tracked and untracked source without changing the original index', async () => {
  const { source, options } = await fixture();
  git(source, ['init', '--template=']);
  await writeFile(join(source, 'tracked.txt'), 'original');
  git(source, ['add', '.']);
  git(source, ['-c', 'user.name=Test', '-c', 'user.email=test@localhost', '-c', 'commit.gpgSign=false', 'commit', '--no-verify', '-m', 'test baseline']);
  await writeFile(join(source, 'tracked.txt'), 'staged'); git(source, ['add', '.']);
  await writeFile(join(source, 'tracked.txt'), 'current unstaged');
  await writeFile(join(source, 'new.txt'), 'untracked');
  await writeFile(join(source, '.gitignore'), 'cache/\n');
  await mkdir(join(source, 'cache')); await writeFile(join(source, 'cache', 'temp.txt'), 'ignored');
  await writeFile(join(source, '.env'), 'SYNTHETIC_SECRET');
  const before = git(source, ['status', '--porcelain', '-z']);
  const snapshot = await createWorkspace(options);
  assert.equal(await readFile(join(snapshot.cwd, 'tracked.txt'), 'utf8'), 'current unstaged');
  assert.equal(await readFile(join(snapshot.cwd, 'new.txt'), 'utf8'), 'untracked');
  await assert.rejects(readFile(join(snapshot.cwd, '.env')));
  await assert.rejects(readFile(join(snapshot.cwd, 'cache', 'temp.txt')));
  assert.equal(git(source, ['status', '--porcelain', '-z']), before);
  assert.equal(git(source, ['show', ':tracked.txt']), 'staged');
  await writeFile(join(snapshot.cwd, 'tracked.txt'), 'agent change');
  assert.equal(await readFile(join(source, 'tracked.txt'), 'utf8'), 'current unstaged');
  let result = await inspectWorkspace(snapshot);
  assert.deepEqual(result.changedFiles, ['tracked.txt']); assert.deepEqual(result.sourceChanged, []);
  await writeFile(join(source, 'tracked.txt'), 'concurrent user change');
  result = await inspectWorkspace(snapshot);
  assert.deepEqual(result.sourceChanged, ['tracked.txt']); assert.equal(result.appliedToOriginal, false);
});
test('unborn Git and non-Git projects both get usable private baselines', async () => {
  for (const useGit of [true, false]) {
    const { source, options } = await fixture();
    if (useGit) git(source, ['init', '--template=']);
    await writeFile(join(source, 'README.md'), 'Current project');
    const snapshot = await createWorkspace(options);
    assert.equal(await readFile(join(snapshot.cwd, 'README.md'), 'utf8'), 'Current project');
    assert.ok(snapshot.baselineCommit);
    const again = await createWorkspace(options); assert.equal(again.cwd, snapshot.cwd);
    assert.deepEqual(workspacePolicy(snapshot.cwd).writableRoots, [snapshot.cwd]);
  }
});
test('snapshot rejects source nesting, task traversal and external junction contents', async () => {
  const { root, source, options } = await fixture();
  await assert.rejects(createWorkspace({ ...options, taskId: '../escape' }));
  await assert.rejects(createWorkspace({ ...options, baseDirectory: join(source, 'nested') }));
  const outside = join(root, 'outside'); await mkdir(outside);
  await writeFile(join(outside, 'private.txt'), 'SYNTHETIC_PRIVATE');
  await symlink(outside, join(source, 'linked'), process.platform === 'win32' ? 'junction' : 'dir');
  await assert.rejects(createWorkspace(options), /链接/);
  assert.equal(await readFile(join(outside, 'private.txt'), 'utf8'), 'SYNTHETIC_PRIVATE');
});

test('broken Git index in an ancestor never falls back to an unfiltered directory snapshot', async () => {
  const { source, options } = await fixture();
  git(source, ['init', '--template=']);
  const nested = join(source, 'nested'); await mkdir(nested);
  await writeFile(join(nested, 'source.txt'), 'synthetic');
  await writeFile(join(source, '.git', 'index'), 'INVALID_SYNTHETIC_INDEX');
  await assert.rejects(createWorkspace({ ...options, sourceCwd: nested }));
  assert.equal(await readFile(join(nested, 'source.txt'), 'utf8'), 'synthetic');
});
