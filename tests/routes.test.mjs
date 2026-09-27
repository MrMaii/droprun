import test from 'node:test';
import assert from 'node:assert/strict';
import { targetRoute, matchesTargetRoute } from '../connector/routes.mjs';

test('preview route gate includes query and fragment without allowing origin changes',()=>{
  const origin='http://127.0.0.1:32100';
  assert.equal(matchesTargetRoute(origin+'/?view=product','/?view=product',origin),true);
  assert.equal(matchesTargetRoute(origin+'/#/search','/#/search',origin),true);
  assert.equal(matchesTargetRoute(origin+'/work/','/work',origin),true);
  for(const url of [origin+'/',origin+'/?view=admin',origin+'/?view=product#other','http://127.0.0.1:32101/?view=product','https://other.test/?view=product']) {
    assert.equal(matchesTargetRoute(url,'/?view=product',origin),false,url);
  }
  for(const route of ['//other.test','/\\other.test','https://other.test/','/\t/other.test','/foo\n']) assert.throws(()=>targetRoute(route));
});
