import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, symlink, rm, realpath } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { recordedProjectChanges } from '../connector/project-task.mjs';

test('project receipts accept canonical aliases and deleted files but reject outside targets', async t => {
  const root = await realpath(await mkdtemp(join(tmpdir(), 'droprun-paths-')));
  t.after(() => rm(root, { recursive: true, force: true }));
  const project = join(root, 'project'), alias = join(root, 'alias'), outside = join(root, 'outside');
  await mkdir(project); await mkdir(outside);
  await writeFile(join(project, 'result.txt'), 'result');
  await symlink(project, alias, 'junction');
  await symlink(outside, join(project, 'external'), 'junction');
  const events = changes => [{ type: 'files', status: 'completed', changes }];
  assert.deepEqual(await recordedProjectChanges(project, events([
    { path: join(alias, 'result.txt'), kind: { type: 'add' } },
    { path: 'result.txt', kind: { type: 'update' } },
    { path: join(alias, 'deleted', 'old.txt'), kind: { type: 'delete' } }
  ])), { changedFiles: ['result.txt', 'deleted/old.txt'], deletedPaths: ['deleted/old.txt'] });
  for (const path of [join(outside, 'missing.txt'), '../outside/missing.txt', 'external/deleted.txt']) {
    await assert.rejects(recordedProjectChanges(project, events([{ path, kind: { type: 'delete' } }])), /超出原项目范围/);
  }
});
