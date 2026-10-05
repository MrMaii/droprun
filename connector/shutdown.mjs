import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

// Task acquisition and background writes must finish before an owner stops the worker.
export class ConnectorWork {
  pending = 0;
  stopping = false;
  get busy() { return this.pending > 0; }
  begin() {
    if (this.stopping) return false;
    this.pending++;
    return true;
  }
  end() { this.pending--; }
  async run(action) {
    if (!this.begin()) return;
    try { return await action(); }
    finally { this.end(); }
  }
  requestStop() {
    if (this.busy) return false;
    this.stopping = true;
    return true;
  }
}

function processAlive(pid) {
  try { process.kill(pid, 0); return true; }
  catch (error) { if (error.code === 'ESRCH') return false; throw error; }
}

export async function stopConnector(config, { fetchImpl = fetch, alive = processAlive, wait = ms => new Promise(resolve => setTimeout(resolve, ms)), timeout = 30000 } = {}) {
  let health;
  try {
    const response = await fetchImpl('http://127.0.0.1:47493', { signal: AbortSignal.timeout(2000) });
    if (!response.ok) throw new Error();
    health = await response.json();
  } catch (error) {
    if ((error.code || error.cause?.code) === 'ECONNREFUSED') return;
    throw new Error('Could not verify the running Connector. Keep the current installation and try again.');
  }
  if (health.service !== 'DropRun Connector' || health.instanceId !== config.instanceId) throw new Error('A different service or DropRun instance is running. It will not be stopped.');
  if (health.shutdownProtocolVersion !== 1) throw new Error('This older Connector does not support verified shutdown. Stop it manually before retrying the update or uninstall.');
  if (!Number.isSafeInteger(health.pid) || health.pid < 1) throw new Error('The Connector process identity could not be verified. Keep the current installation.');
  if (health.activeTask) throw new Error('Finish or cancel the active task before updating.');
  const response = await fetchImpl('http://127.0.0.1:47493/management/stop', { method: 'POST', headers: { Authorization: 'Bearer ' + config.connectorToken }, signal: AbortSignal.timeout(5000) });
  if (response.status !== 202 || (await response.json()).stopping !== true) throw new Error('The Connector could not be stopped safely.');
  const deadline = Date.now() + timeout;
  while (alive(health.pid)) {
    if (Date.now() >= deadline) throw new Error('The Connector has not finished stopping. Keep the current installation and try again.');
    await wait(Math.min(100, Math.max(1, deadline - Date.now())));
  }
}

// The new installer extracts this verifier instead of executing an old setup helper.
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  try {
    if (!process.argv[2] || !process.argv[3]) throw new Error('Provide the installation root and data directory.');
    const installRoot = resolve(process.argv[2]), dataDir = resolve(process.argv[3]);
    const { loadRuntimeConfig } = await import(pathToFileURL(resolve(installRoot, 'connector/config.mjs')).href);
    const { config } = await loadRuntimeConfig(installRoot, { ...process.env, DROPRUN_DATA_DIR: dataDir });
    await stopConnector(config);
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
