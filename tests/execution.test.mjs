import { test } from 'node:test';
import assert from 'node:assert/strict';
import { executionOverrides, verifyTaskTools } from '../connector/execution.mjs';

test('execution overrides disable integrations without copying credentials or changing user config', () => {
  const config = { mcp_servers: { node_repl: { enabled: true, env: { TOKEN: 'synthetic' } } }, plugins: { 'plugin@market': { enabled: true } } };
  const before = JSON.stringify(config), overrides = executionOverrides(config);
  assert.deepEqual(overrides, { 'features.apps': false, 'features.plugins': false, 'features.remote_plugin': false, 'mcp_servers.node_repl.enabled': false, 'plugins.plugin@market.enabled': false });
  assert.equal(JSON.stringify(config), before);
  assert.ok(!JSON.stringify(overrides).includes('synthetic'));
  assert.throws(() => executionOverrides({ mcp_servers: { 'with.dot': {} } }), /无法安全覆盖/);
});

test('task tools gate checks the thread and refuses any exposed MCP server', async () => {
  const calls = [];
  await verifyTaskTools({ call: async (method, params) => { calls.push({ method, params }); return { data: [], nextCursor: null }; } }, 'our-thread');
  assert.equal(calls[0].method, 'mcpServerStatus/list');
  assert.equal(calls[0].params.threadId, 'our-thread');
  await verifyTaskTools({ call: async () => ({ data: [{ runtimeStatus: 'disabled', tools: {}, resources: [], resourceTemplates: [] }], nextCursor: null }) }, 'our-thread');
  await assert.rejects(verifyTaskTools({ call: async () => ({ data: [{ name: 'unexpected' }], nextCursor: null }) }, 'our-thread'), /MCP/);
});
