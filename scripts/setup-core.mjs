import { readFile, writeFile, mkdir, rm } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { resolve, join } from 'node:path';
import { randomBytes, randomUUID, createHash } from 'node:crypto';
import { spawn } from 'node:child_process';
import { atomicJson, protectSecret, relayOrigin } from '../connector/config.mjs';

export const WRANGLER_VERSION = '4.141.0';
export function validateSetup(input) {
  if (!/^[a-f0-9]{32}$/i.test(input.accountId || '')) throw new Error('Choose a Cloudflare account ID (32 hexadecimal characters).');
  if (!/^[a-z][a-z0-9-]{2,43}$/.test(input.name || '')) throw new Error('Instance name must be 3–44 lowercase letters, numbers or hyphens.');
  if (input.costAccepted !== true) throw new Error('Confirm that these resources belong to your Cloudflare account and may incur usage charges.');
  return { accountId: input.accountId.toLowerCase(), name: input.name, costAccepted: true };
}

export function wranglerPath(root) { return join(root, 'node_modules', 'wrangler', 'bin', 'wrangler.js'); }
export function command(executable, args, { cwd, input, onOutput = () => {}, env = process.env, timeout = 300000 } = {}) {
  return new Promise((resolvePromise, reject) => {
    const child = spawn(executable, args, { cwd, env, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
    let output = '';
    const capture = bytes => { output = (output + bytes).slice(-30000); onOutput(String(bytes)); };
    child.stdout.on('data', capture); child.stderr.on('data', capture);
    const timer = setTimeout(() => { child.kill(); reject(new Error('The setup command timed out. You can resume this step.')); }, timeout);
    child.once('error', error => { clearTimeout(timer); reject(error); });
    child.once('exit', code => { clearTimeout(timer); code === 0 ? resolvePromise(output) : reject(new Error('Setup command failed. ' + output.slice(-1500))); });
    child.stdin.end(input);
  });
}

export function redact(text) {
  return String(text).replace(/(bearer\s+)[^\s"']+/gi, '$1[redacted]').replace(/((?:token|secret|code|key)[=:]\s*)[^\s&"']+/gi, '$1[redacted]');
}

export async function setupState(dataDir) {
  try { return JSON.parse(await readFile(join(dataDir, 'deployment', 'state.json'), 'utf8')); }
  catch (error) { if (error.code === 'ENOENT') return null; throw error; }
}

export async function provision({ root, dataDir, input, run = command, protect = protectSecret, fetchImpl = fetch, progress = () => {} }) {
  const settings = validateSetup(input), directory = join(dataDir, 'deployment');
  const existingConfig = existsSync(join(dataDir, 'config.json')) ? JSON.parse(await readFile(join(dataDir, 'config.json'), 'utf8')) : null;
  await mkdir(directory, { recursive: true });
  let state = await setupState(dataDir);
  if (state && (state.name !== settings.name || state.accountId !== settings.accountId)) throw new Error('This installation already belongs to another instance. Use a different data directory to create another Relay.');
  if (!state && existingConfig) throw new Error('Existing configuration detected. Setup will not overwrite or redeploy a private instance.');
  if (!state) {
    state = { ...settings, instanceId: randomUUID(), connectorTokenProtected: await protect(randomBytes(32).toString('hex')), completed: [] };
    await atomicJson(join(directory, 'state.json'), state);
  }
  const connectorToken = await protect(state.connectorTokenProtected, true);
  const hash = value => createHash('sha256').update(value).digest('hex');
  const secretFile = join(directory, 'deploy-secrets.json');
  const relayConfigFile = join(directory, 'wrangler.jsonc'), previewConfigFile = join(directory, 'preview.jsonc');
  const wrangler = wranglerPath(root);
  if (!existsSync(wrangler)) throw new Error('Wrangler is missing from this package. Developers: run npm ci first.');
  const execute = args => run(process.execPath, [wrangler, ...args], { cwd: directory, env: { ...process.env, CLOUDFLARE_ACCOUNT_ID: settings.accountId, CI: 'true', WRANGLER_SEND_METRICS: 'false' }, onOutput: text => progress({ message: redact(text) }) });
  const step = async (key, action) => {
    if (state.completed.includes(key)) return;
    progress({ step: key, status: 'running' });
    await action(); state.completed.push(key); await atomicJson(join(directory, 'state.json'), state);
    progress({ step: key, status: 'complete' });
  };
  try {
    // Only one-way token hashes leave this machine. Cloudflare login credentials are managed by Wrangler.
    await writeFile(secretFile, JSON.stringify({ CONNECTOR_HASH: hash(connectorToken), ADMIN_HASH: hash(randomBytes(32)) }), { mode: 0o600 });
    await step('configuration', async () => {
      if (!existsSync(relayConfigFile)) await atomicJson(relayConfigFile, {
        name: settings.name + '-' + state.instanceId.slice(0, 8), account_id: settings.accountId, main: resolve(root, 'relay/worker.mjs'), compatibility_date: '2026-09-25', workers_dev: true,
        d1_databases: [{ binding: 'DB', migrations_dir: resolve(root, 'relay/migrations-fresh') }], r2_buckets: [{ binding: 'FILES' }], ai: { binding: 'AI' },
        vars: { INSTANCE_ID: state.instanceId }, triggers: { crons: ['0 * * * *'] }
      });
    });
    // Wrangler deliberately does not write auto-provisioned IDs in non-interactive mode.
    // Resolve deterministic resources ourselves so interrupted setup is safe to resume.
    await step('d1-resource', async () => {
      const name = settings.name + '-db-' + state.instanceId.slice(0, 8);
      let databases = JSON.parse(await execute(['d1', 'list', '--json', '--config', relayConfigFile]));
      if (!databases.some(db => db.name === name)) {
        await execute(['d1', 'create', name, '--config', relayConfigFile]);
        databases = JSON.parse(await execute(['d1', 'list', '--json', '--config', relayConfigFile]));
      }
      const database = databases.find(db => db.name === name);
      if (!database?.uuid) throw new Error('Cloudflare did not return the created database ID. Resume setup.');
      const config = JSON.parse(await readFile(relayConfigFile, 'utf8'));
      config.d1_databases = [{ binding: 'DB', database_name: name, database_id: database.uuid, migrations_dir: resolve(root, 'relay/migrations-fresh') }];
      await atomicJson(relayConfigFile, config);
    });
    await step('r2-resource', async () => {
      const name = settings.name + '-files-' + state.instanceId.slice(0, 8);
      try { await execute(['r2', 'bucket', 'info', name, '--json', '--config', relayConfigFile]); }
      catch (error) {
        if (!/does not exist|not found|10006|10007/i.test(error.message)) throw error;
        await execute(['r2', 'bucket', 'create', name, '--config', relayConfigFile]);
      }
      const config = JSON.parse(await readFile(relayConfigFile, 'utf8'));
      config.r2_buckets = [{ binding: 'FILES', bucket_name: name }];
      await atomicJson(relayConfigFile, config);
    });
    await step('resources', async () => {
      const output = await execute(['deploy', '--config', relayConfigFile, '--secrets-file', secretFile]);
      const matches = output.match(/https:\/\/[a-z0-9-]+\.[a-z0-9-]+\.workers\.dev\b/g);
      if (!matches?.length) throw new Error('Deployment completed, but its workers.dev address was not returned. Resume to verify the existing resource.');
      state.relay = relayOrigin(matches.at(-1));
    });
    await step('database', () => execute(['d1', 'migrations', 'apply', 'DB', '--remote', '--config', relayConfigFile]));
    await step('preview', async () => {
      const relay = JSON.parse(await readFile(relayConfigFile, 'utf8'));
      if (!relay.d1_databases?.[0]?.database_id || !relay.r2_buckets?.[0]?.bucket_name) throw new Error('The instance resource IDs are missing. Resume provisioning before deploying the preview.');
      await atomicJson(previewConfigFile, { name: relay.name + '-preview', account_id: settings.accountId, main: resolve(root, 'relay/preview-worker.mjs'), compatibility_date: '2026-09-25', workers_dev: true, d1_databases: relay.d1_databases, r2_buckets: relay.r2_buckets, vars: { INSTANCE_ID: state.instanceId } });
      const output = await execute(['deploy', '--config', previewConfigFile]);
      const matches = output.match(/https:\/\/[a-z0-9-]+\.[a-z0-9-]+\.workers\.dev\b/g);
      if (!matches?.length) throw new Error('Preview Worker address was not returned. Resume setup.');
      state.previewOrigin = relayOrigin(matches.at(-1));
    });
    await step('connect', async () => {
      const relay = JSON.parse(await readFile(relayConfigFile, 'utf8'));
      relay.vars.PREVIEW_ORIGIN = state.previewOrigin;
      await atomicJson(relayConfigFile, relay);
      await execute(['deploy', '--config', relayConfigFile, '--secrets-file', secretFile]);
    });
    progress({ step: 'verify', status: 'running' });
    const response = await fetchImpl(state.relay + '/health', { signal: AbortSignal.timeout(20000) });
    const health = await response.json();
    if (!response.ok || health.instanceId !== state.instanceId || health.protocolVersion !== 2 || health.ready !== true) throw new Error('Relay is not ready or belongs to a different instance. Resume setup after checking Cloudflare.');
    const previewResponse = await fetchImpl(state.previewOrigin + '/health', { signal: AbortSignal.timeout(20000) });
    const previewHealth = await previewResponse.json();
    if (!previewResponse.ok || previewHealth.instanceId !== state.instanceId || previewHealth.ready !== true) throw new Error('Preview is not ready or belongs to a different instance. Resume setup after checking Cloudflare.');
    const authenticated = await fetchImpl(state.relay + '/projects', { headers: { Authorization: 'Bearer ' + connectorToken }, signal: AbortSignal.timeout(20000) });
    if (!authenticated.ok) throw new Error('Relay rejected this Connector credential. Setup has not replaced your configuration.');
    const tools = join(dataDir, 'tools');
    const toolPaths = Object.fromEntries([['ytDlp', 'yt-dlp.exe'], ['ffmpeg', 'ffmpeg.exe'], ['ffprobe', 'ffprobe.exe']].filter(([, file]) => existsSync(join(tools, file))).map(([key, file]) => [key, join(tools, file)]));
    await atomicJson(join(dataDir, 'config.json'), { ...(existingConfig || {}), relay: state.relay, instanceId: state.instanceId, connectorTokenProtected: state.connectorTokenProtected, openDesktop: true, ...toolPaths });
    progress({ step: 'verify', status: 'complete' });
    return { relay: state.relay, instanceId: state.instanceId, previewOrigin: state.previewOrigin };
  } finally { await rm(secretFile, { force: true }); }
}
