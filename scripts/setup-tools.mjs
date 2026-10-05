import { mkdir, readFile, writeFile, readdir, copyFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { createHash } from 'node:crypto';
import { defaultDataDir, atomicJson } from '../connector/config.mjs';
import { command } from './setup-core.mjs';

// User-owned tools are downloaded directly from upstream, separately from the Apache-2.0 app.
export const mediaTools = [
  { name: 'yt-dlp.exe', version: '2026.08.19', url: 'https://github.com/yt-dlp/yt-dlp/releases/download/2026.08.19/yt-dlp.exe', sha256: '66674953fe251b89f4d08c5f0e35e0728679bd67ab3d7d05c0562af101dd3e7a', license: 'GPL-3.0-or-later', source: 'https://github.com/yt-dlp/yt-dlp/tree/2026.08.19' },
  { name: 'ffmpeg.zip', version: 'N-126864-gc966a1de0a', url: 'https://github.com/yt-dlp/FFmpeg-Builds/releases/download/autobuild-2026-09-25-18-43/ffmpeg-N-126864-gc966a1de0a-win64-gpl.zip', sha256: '0c3dc508209dc850168ded0917dc6e1439a6661619530e7cbb1396d48e3bf7cf', license: 'GPL-3.0-or-later', source: 'https://github.com/yt-dlp/FFmpeg-Builds/releases/tag/autobuild-2026-09-25-18-43' }
];

export async function installMediaTools(directory, progress = () => {}, fetchImpl = fetch) {
  await mkdir(directory, { recursive: true });
  for (const tool of mediaTools) {
    progress({ message: 'Downloading ' + tool.name + ' ' + tool.version + ' from its upstream project…' });
    const response = await fetchImpl(tool.url, { signal: AbortSignal.timeout(300000) });
    if (!response.ok) throw new Error('Could not download ' + tool.name + ' (HTTP ' + response.status + ').');
    const bytes = Buffer.from(await response.arrayBuffer());
    if (createHash('sha256').update(bytes).digest('hex') !== tool.sha256) throw new Error(tool.name + ' checksum mismatch. Nothing was executed.');
    const file = join(directory, tool.name);
    await writeFile(file, bytes);
    if (tool.name.endsWith('.zip')) {
      const destination = join(directory, 'ffmpeg-' + tool.version);
      // Fixed, checksum-verified upstream archive. Paths are arguments, never shell-built commands.
      const script = join(directory, 'extract.ps1');
      await writeFile(script, 'param([string]$Archive,[string]$Destination)\n$ErrorActionPreference="Stop"\nExpand-Archive -LiteralPath $Archive -DestinationPath $Destination -Force\n');
      await command('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', script, '-Archive', file, '-Destination', destination]);
      const dirs = await readdir(destination, { withFileTypes: true });
      const extracted = dirs.find(entry => entry.isDirectory());
      if (!extracted) throw new Error('FFmpeg archive layout changed.');
      for (const name of ['ffmpeg.exe', 'ffprobe.exe']) await copyFile(join(destination, extracted.name, 'bin', name), join(directory, name));
    }
  }
  await atomicJson(join(directory, 'UPSTREAM-TOOLS.json'), mediaTools);
  await writeFile(join(directory, 'README.txt'), 'These independent programs were downloaded directly from their upstream projects. They retain their own licenses; see UPSTREAM-TOOLS.json and the extracted FFmpeg license. DropRun does not relicense them.\n');
  return { ytDlp: join(directory, 'yt-dlp.exe'), ffmpeg: join(directory, 'ffmpeg.exe'), ffprobe: join(directory, 'ffprobe.exe') };
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  installMediaTools(resolve(process.argv[2] || join(defaultDataDir(), 'tools')), ({ message }) => console.log(message)).then(result => console.log(JSON.stringify(result))).catch(error => { console.error(error.message); process.exitCode = 1; });
}
