import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare } from './miniflare.mjs';

test('early rejection of an unread UTF-8 body preserves the local HTTP response',async()=>{
  const mf=new Miniflare({modules:true,compatibilityDate:'2026-08-06',script:`
    export default { fetch(request) {
      return Response.json({length:request.headers.get('Content-Length'),token:request.headers.get('X-Fixture')},{status:403});
    }};
  `});
  const body=JSON.stringify({note:'本地传输测试'.repeat(80)});
  try {
    for(let n=0;n<200;n++){
      const response=await mf.dispatchFetch('https://fixture.test/reject',{method:'POST',headers:{'X-Fixture':'retained'},body});
      assert.equal(response.status,403);
      assert.deepEqual(await response.json(),{length:String(Buffer.byteLength(body)),token:'retained'});
    }
  }finally{await mf.dispose();}
});
