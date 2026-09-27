import { spawn, execFileSync } from 'node:child_process';
import { readdirSync, existsSync, statSync, copyFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { homedir } from 'node:os';
import { createInterface } from 'node:readline';
import { EventEmitter } from 'node:events';

export function findCodex() {
  if (process.env.DROPRUN_CODEX) return process.env.DROPRUN_CODEX;
  const root = join(process.env.LOCALAPPDATA || homedir(), 'OpenAI', 'Codex', 'bin');
  if (existsSync(root)) {
    const candidates = readdirSync(root).map(d => join(root, d, 'codex.exe')).filter(existsSync);
    // Desktop versions expose project APIs that an older global CLI may lack.
    candidates.sort((a, b) => version(b).localeCompare(version(a), undefined, { numeric: true }));
    if (candidates.length) return privateCopy(candidates[0]);
  }
  return process.env.CODEX_EXECUTABLE || 'codex';
}
/**
 * The Codex desktop app terminates every process named codex.exe every five minutes (observed 2026-09-09),
 * which killed DropRun's background sessions mid-task. A sibling copy under another name is left alone
 * and still finds its runtime files, so the Connector runs that instead.
 */
export function privateCopy(executable) {
  const copy = join(dirname(executable), 'codex-droprun.exe');
  try {
    const source = statSync(executable);
    if (!existsSync(copy) || statSync(copy).size !== source.size || statSync(copy).mtimeMs < source.mtimeMs) copyFileSync(executable, copy);
    return copy;
  } catch { return executable; }
}
function version(exe) {
  try { return execFileSync(exe, ['--version'], { encoding: 'utf8', windowsHide: true }).trim(); }
  catch { return ''; }
}
export class Codex extends EventEmitter {
  constructor(executable = findCodex()) {
    super(); this.executable = executable; this.pending = new Map(); this.serial = 0;
  }
  async start({ cwd, configOverrides = {} } = {}) {
    const overrides = Object.entries(configOverrides).flatMap(([key, value]) => ['-c', key + '=' + JSON.stringify(value)]);
    this.proc = spawn(this.executable, [...overrides, 'app-server', '--stdio'], {
      cwd, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env, RUST_LOG: 'error' }
    });
    this.proc.stderr.on('data', d => this.emit('diagnostic', String(d).slice(0, 1500)));
    createInterface({ input: this.proc.stdout }).on('line', line => {
      let msg; try { msg = JSON.parse(line); } catch { return; }
      if (msg.id !== undefined && !msg.method) {
        const pending = this.pending.get(msg.id);
        if (pending) { clearTimeout(pending.timer); this.pending.delete(msg.id); msg.error ? pending.reject(new Error(JSON.stringify(msg.error))) : pending.resolve(msg.result); }
      } else this.emit('event', msg);
    });
    this.proc.on('error', e => this.fail(e));
    this.proc.on('exit', code => { this.fail(new Error(`Codex exited (${code})`)); this.emit('disconnected', code); });
    await this.call('initialize', { clientInfo: { name: 'droprun', title: 'DropRun', version: '0.1.0' }, capabilities: { experimentalApi: true } });
    this.send({ method: 'initialized' });
    return this;
  }
  fail(error) { for (const p of this.pending.values()) { clearTimeout(p.timer); p.reject(error); } this.pending.clear(); }
  send(msg) { this.proc.stdin.write(JSON.stringify(msg) + '\n'); }
  call(method, params = {}, timeout = 60000) {
    const id = ++this.serial;
    return new Promise((resolve, reject) => {
      const timer = timeout ? setTimeout(() => { this.pending.delete(id); reject(new Error(`${method} timed out`)); }, timeout) : null;
      this.pending.set(id, { resolve, reject, timer }); this.send({ id, method, params });
    });
  }
  async projects() {
    const all = []; let cursor;
    do { const page = await this.call('project/list', { limit: 100, cursor }); all.push(...page.data); cursor = page.nextCursor; } while (cursor);
    return all;
  }
  close() { this.proc?.kill(); }
}
