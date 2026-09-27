import { Miniflare as WorkerRuntime, convertV4MiniflareOptions } from 'miniflare';
import { readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';

// Keep the test fixtures' v4 binding descriptions while using the current runtime.
export class Miniflare extends WorkerRuntime {
  constructor(options) {
    if (options.scriptPath && options.modules === true) {
      const { scriptPath, ...rest } = options;
      const siblings = readdirSync(dirname(scriptPath)).filter(name => name.endsWith('.mjs'));
      options = { ...rest, modules: [scriptPath, ...siblings.map(name => join(dirname(scriptPath), name)).filter(path => path.replaceAll('\\', '/') !== scriptPath.replaceAll('\\', '/'))].map(path => ({ type: 'ESModule', path })) };
    }
    super(convertV4MiniflareOptions(options));
  }
}
