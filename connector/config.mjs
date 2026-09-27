import { readFile, writeFile, mkdir, rename } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join, resolve, dirname } from 'node:path';
import { homedir } from 'node:os';
import { spawn } from 'node:child_process';

export function defaultDataDir(env = process.env) {
  return resolve(env.DROPRUN_DATA_DIR || join(env.LOCALAPPDATA || join(homedir(), '.local', 'share'), 'DropRun'));
}

export function configLocation(root, env = process.env) {
  if (env.DROPRUN_CONFIG) return { file: resolve(env.DROPRUN_CONFIG), dataDir: dirname(resolve(env.DROPRUN_CONFIG)), legacy: false };
  if (!env.DROPRUN_DATA_DIR && existsSync(join(root, '.local', 'config.json'))) return { file: join(root, '.local', 'config.json'), dataDir: join(root, '.local'), legacy: true };
  const dataDir = defaultDataDir(env);
  return { file: join(dataDir, 'config.json'), dataDir, legacy: false };
}

export function relayOrigin(value) {
  const url = new URL(value);
  if (url.protocol !== 'https:' || url.username || url.password || url.search || url.hash || url.pathname !== '/' || url.port) throw new Error('Use an HTTPS Relay origin without a path, credentials or port.');
  return url.origin;
}

export function protectSecret(value, decrypt = false) {
  if (process.platform !== 'win32') throw new Error('Windows DPAPI is required for the installed Connector credentials.');
  // Input is transferred over stdin, never in the command line or shell interpolation.
  const script = `Add-Type -AssemblyName System.Security; $v=[Console]::In.ReadToEnd(); $b=${decrypt ? '[Convert]::FromBase64String($v)' : '[Text.Encoding]::UTF8.GetBytes($v)'}; $r=[Security.Cryptography.ProtectedData]::${decrypt ? 'Unprotect' : 'Protect'}($b,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser); [Console]::Out.Write(${decrypt ? '[Text.Encoding]::UTF8.GetString($r)' : '[Convert]::ToBase64String($r)'});`;
  return new Promise((resolve, reject) => {
    const child = spawn('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command', script], { windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
    let result = '';
    child.stdout.on('data', bytes => { result += bytes; });
    child.stderr.resume();
    child.once('error', reject);
    child.once('exit', code => code === 0 && result ? resolve(result) : reject(new Error('Windows could not unlock the DropRun credential for this user.')));
    child.stdin.end(value);
  });
}

export async function atomicJson(file, data) {
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file + '.tmp', JSON.stringify(data, null, 2), { mode: 0o600 });
  await rename(file + '.tmp', file);
}

export async function loadRuntimeConfig(root, env = process.env) {
  const location = configLocation(root, env);
  let config;
  try { config = JSON.parse(await readFile(location.file, 'utf8')); }
  catch (error) { if (error.code === 'ENOENT') throw new Error('DropRun is not connected yet. Open the DropRun setup shortcut.'); throw error; }
  config.relay = relayOrigin(config.relay);
  if (config.connectorTokenProtected) config.connectorToken = await protectSecret(config.connectorTokenProtected, true);
  if (config.tunnelTokenProtected) config.tunnelToken = await protectSecret(config.tunnelTokenProtected, true);
  if (typeof config.connectorToken !== 'string' || config.connectorToken.length < 32) throw new Error('The Connector credential is missing. Run setup to repair this installation.');
  return { ...location, config };
}

export function pairingLink(relay, instanceId, code) {
  code = typeof code === 'string' ? code.trim().toUpperCase().replace(/[\s-]/g, '') : code;
  if (!/^[0-9a-f-]{36}$/i.test(instanceId || '') || !/^[0-9A-F]{20}$/.test(code || '')) throw new Error('This Relay needs the self-host pairing update before connecting a new phone.');
  return 'droprun://pair?' + new URLSearchParams({ relay: relayOrigin(relay), instance: instanceId, code }).toString();
}
