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
    const dispatch=this.dispatchFetch;
    this.dispatchFetch=(input,init)=>{
      // Known string bodies need explicit framing: an early 4xx with an unread
      // chunked body can reset workerd's local connection on Windows. Do not
      // retry mutations or consume authentication failures inside the Worker.
      if(typeof init?.body==='string'){
        const headers=new Headers(init.headers);
        if(!headers.has('Content-Length'))headers.set('Content-Length',String(Buffer.byteLength(init.body)));
        init={...init,headers};
      }
      return dispatch(input,init);
    };
  }
}
