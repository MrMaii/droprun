import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { renderPairPage, renderLocalActionPage, renderMediaLoginPage } from '../connector/pair-page.mjs';

const relay = 'https://ui-fixture.invalid';
const instanceId = '11111111-1111-4111-8111-111111111111';
const code = '1234567890ABCDEF1234';
const script = html => html.match(/<script>([\s\S]*?)<\/script>/)[1];
const gate = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b; }); return { promise, resolve, reject }; };

async function pairFixture({ remaining = 61000, clipboard = async () => {} } = {}) {
  let now = 1000000, tick, focused;
  const ids = ['language','qr','pairing-values','copy-code','copy-link','countdown','expiry','copy-status','pairing-code','pairing-link'];
  const nodes = Object.fromEntries(ids.map(id => [id, { textContent:'', hidden:false, disabled:false, parentElement:{open:false}, setAttribute(name,value) { this[name]=value; }, focus() { focused=id; } }]));
  nodes['pairing-code'].textContent = code;
  nodes['pairing-link'].textContent = relay + '/pair';
  const labels = [{ dataset:{en:'Connect your phone.',zh:'连接你的手机。'}, textContent:'' }];
  const document = { title:'', documentElement:{lang:'en'}, getElementById:id=>nodes[id], querySelectorAll:()=>labels };
  const writes = [];
  const html = await renderPairPage(relay,{instanceId,code,expiresAt:now+remaining});
  vm.runInNewContext(script(html), {
    document, localStorage:{getItem:()=>null,setItem(){}},
    navigator:{clipboard:{async writeText(value) { writes.push(value); return clipboard(value); }}},
    Date:{now:()=>now}, setInterval:callback=>{tick=callback;return 1;}, clearInterval(){}
  });
  return { nodes, document, labels, writes, focus:()=>focused, advance(ms) { now+=ms; tick?.(); } };
}

test('pairing renderer preserves the instance link, escapes legacy values and needs confirmation to create another code', async () => {
  const html = await renderPairPage(relay,{instanceId,code,expiresAt:2000000});
  assert.match(html,/href="\/pair"/);
  assert.match(html,/instance=11111111-1111-4111-8111-111111111111/);
  assert.match(html,/code=1234567890ABCDEF1234/);
  assert.equal((html.match(/<script>/g)||[]).length,1);
  assert.doesNotMatch(script(html),/\bfetch\(/);
  const legacy = await renderPairPage('https://legacy.invalid/<script>alert(1)</script>',{code:'<img onerror=alert(1)>',expiresAt:2000000});
  assert.match(legacy,/&lt;img onerror=alert\(1\)&gt;/);
  assert.match(legacy,/&lt;script&gt;/);
  assert.match(legacy,/Upgrade the Relay/);
  assert.doesNotMatch(legacy,/Open DropRun on Android and choose Scan/);
  assert.equal((legacy.match(/<script>/g)||[]).length,1);
  const login = renderLocalActionPage('/login?site=</script><script>alert(1)</script>', '<img>', 'fixture-token');
  assert.match(login,/&lt;img&gt;/);
  assert.equal((login.match(/<script>/g)||[]).length,1);
  assert.match(script(login),/\\u003c\/script>/);
  assert.match(login,/separate browser profile/);
  assert.doesNotMatch(login,/Create a one-use invitation/);
  assert.match(renderMediaLoginPage(),/window already opened by DropRun/);
  assert.doesNotMatch(renderMediaLoginPage(),/Instagram|\.local\/browser-profile|<script>/);
});

test('expired invitations stop showing QR/code and cannot copy; locale changes retain expiry', async () => {
  const f = await pairFixture();
  assert.match(f.nodes.countdown.textContent,/1:01/);
  await f.nodes['copy-code'].onclick();
  assert.deepEqual(f.writes,[code]);
  assert.equal(f.nodes['copy-status'].textContent,'Pairing code copied.');
  f.advance(61000);
  assert.equal(f.nodes.qr.hidden,true);
  assert.equal(f.nodes['pairing-values'].hidden,true);
  assert.equal(f.nodes['copy-code'].disabled,true);
  assert.equal(f.nodes['copy-status'].textContent,'');
  assert.match(f.nodes.expiry.textContent,/expired/);
  await f.nodes['copy-link'].onclick();
  assert.equal(f.writes.length,1);
  f.nodes.language.onclick();
  assert.equal(f.document.documentElement.lang,'zh-CN');
  assert.equal(f.document.title,'连接手机 · DropRun');
  assert.match(f.nodes.expiry.textContent,/已过期/);
  assert.equal(f.nodes.qr.hidden,true);
});

test('clipboard failure gives manual-copy focus; expiry outranks a late clipboard result', async () => {
  const failed = await pairFixture({clipboard:async()=>{throw new Error('Clipboard denied');}});
  await failed.nodes['copy-link'].onclick();
  assert.equal(failed.focus(),'pairing-link');
  assert.equal(failed.nodes['pairing-link'].parentElement.open,true);
  assert.match(failed.nodes['copy-status'].textContent,/copy it manually/);
  assert.doesNotMatch(failed.nodes['copy-status'].textContent,/copied/);
  const pending = gate(), f = await pairFixture({clipboard:()=>pending.promise});
  const result = f.nodes['copy-code'].onclick();
  assert.equal(f.nodes['copy-link'].disabled,true);
  await f.nodes['copy-link'].onclick();
  assert.equal(f.writes.length,1);
  f.advance(62000); pending.resolve(); await result;
  assert.equal(f.nodes['copy-status'].textContent,'');
  assert.equal(f.nodes['copy-code'].disabled,true);
  assert.equal(f.nodes.qr.hidden,true);
});

test('local action only posts on confirmation, locks duplicate clicks and restores retry after error', async () => {
  const pending = gate(), calls=[];
  const button={disabled:false,textContent:'Create a pairing code / 生成配对码'}, error={textContent:''};
  const written=[];
  const document={getElementById:id=>id==='action'?button:error,open(){},write:body=>written.push(body),close(){}};
  vm.runInNewContext(script(renderLocalActionPage('/pair','Connect a phone / 连接手机','fixture-token')),{document,fetch:async(url,options)=>{calls.push({url,...options});return pending.promise;}});
  assert.equal(calls.length,0);
  const request=button.onclick();
  assert.equal(button.disabled,true);
  assert.match(button.textContent,/Working/);
  await button.onclick();
  assert.equal(calls.length,1);
  assert.equal(calls[0].method,'POST');
  assert.equal(calls[0].headers['X-DropRun-Local'],'fixture-token');
  pending.resolve({ok:false,text:async()=> 'Synthetic Relay unavailable'}); await request;
  assert.equal(error.textContent,'Synthetic Relay unavailable');
  assert.equal(button.disabled,false);
  assert.match(button.textContent,/Create a pairing code/);
  assert.deepEqual(written,[]);
});
