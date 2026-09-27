import { Codex } from './codex.mjs';

// These are per-process overrides. Never alter the user's desktop configuration.
export function executionOverrides(config) {
  const overrides = { 'features.apps': false, 'features.plugins': false, 'features.remote_plugin': false };
  for (const section of ['mcp_servers', 'plugins']) {
    for (const name of Object.keys(config[section] || {})) {
      // Codex CLI override paths split on dots; quoted segments are not TOML keys here.
      if (!/^[A-Za-z0-9_@-]+$/.test(name)) throw new Error('工具配置名称无法安全覆盖，未启动隔离任务。');
      overrides[section + '.' + name + '.enabled'] = false;
    }
  }
  return overrides;
}

export async function startTaskRunner(catalog, cwd) {
  const { config } = await catalog.call('config/read', { cwd, includeLayers: false });
  const runner = new Codex(catalog.executable);
  try {
    await runner.start({ cwd, configOverrides: executionOverrides(config) });
    const { config: effective } = await runner.call('config/read', { cwd, includeLayers: false });
    if (effective.features?.apps !== false || effective.features?.plugins !== false || effective.features?.remote_plugin !== false ||
        ['mcp_servers', 'plugins'].some(section => Object.values(effective[section] || {}).some(value => value.enabled !== false))) {
      throw new Error('隔离执行器工具限制未生效，未启动任务。');
    }
    return runner;
  } catch (error) { runner.close(); throw error; }
}

export async function verifyTaskTools(runner, threadId) {
  let cursor;
  do {
    const page = await runner.call('mcpServerStatus/list', { threadId, cursor, limit: 100 });
    if (page.data.some(server => server.runtimeStatus !== 'disabled' || Object.keys(server.tools || {}).length || server.resources?.length || server.resourceTemplates?.length)) throw new Error('隔离会话仍暴露 MCP 工具，未投递任务。');
    cursor = page.nextCursor;
  } while (cursor);
}
