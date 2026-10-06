import QRCode from 'qrcode';
import { pairingLink } from './config.mjs';

export const connectionPageStyles = `
*{box-sizing:border-box}body{margin:0;background:#f7f7f2;color:#191d1a;font:16px/1.55 system-ui,-apple-system,sans-serif}
main{max-width:560px;margin:auto;padding:36px 24px 48px}.wordmark{font-size:18px;font-weight:650;margin-bottom:28px}
header{display:flex;align-items:center;justify-content:space-between;gap:16px}header .wordmark{margin:0}
h1{font-size:34px;line-height:1.12;letter-spacing:-.04em;margin:28px 0 14px}p{color:#616a63;margin:10px 0}
.panel{background:#fff;border:1px solid #e3e6df;border-radius:26px;padding:24px;margin-top:22px}
.qr{max-width:100%;width:316px;margin:0 auto 20px;padding:18px;border-radius:22px;background:#fff}
.qr svg{display:block;width:100%;height:auto}code{display:block;max-width:100%;overflow-wrap:anywhere;padding:14px 16px;border-radius:14px;background:#f0f1ec;font:18px/1.5 ui-monospace,monospace;user-select:all}
.pairing-link{font-size:12px;letter-spacing:0}.note{font-size:14px}.actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:14px}
button,.button{display:inline-flex;align-items:center;justify-content:center;min-height:48px;padding:12px 18px;border:1px solid #dce1d6;border-radius:16px;background:#fff;color:#191d1a;font:600 14px/1.3 system-ui;cursor:pointer;text-decoration:none;transition:transform 120ms,background 160ms}
.primary{background:#b8ef73;color:#172510;border-color:#b8ef73}button:hover,.button:hover{background:#eaf5dd}button:active,.button:active{transform:scale(.98)}
button:disabled{cursor:default;opacity:.5;transform:none}button:focus-visible,a:focus-visible,code:focus-visible{outline:3px solid #477c24;outline-offset:4px}
a{color:#365a24}summary{min-height:48px;padding:12px 0;cursor:pointer;font-size:14px;font-weight:600}summary:focus-visible{outline:3px solid #477c24;outline-offset:4px}.expired{color:#ab342e}#copy-status{min-height:22px}[hidden]{display:none!important}
@media(prefers-color-scheme:dark){body{background:#171b18;color:#f4f5ef}p{color:#bec6bd}.panel{background:#202521;border-color:#384139}code{background:#2b312c}button,.button{background:#202521;border-color:#384139;color:#f4f5ef}.primary{background:#b8ef73;color:#172510;border-color:#b8ef73}button:hover,.button:hover{background:#394339}a{color:#b8ef73}.expired{color:#ffb4a9}button:focus-visible,a:focus-visible,code:focus-visible,summary:focus-visible{outline-color:#b8ef73}}
@media(max-width:380px){main{padding:24px 18px 36px}.panel{padding:18px}h1{font-size:30px}}
@media(prefers-reduced-motion:reduce){button,.button{transition:none}button:active,.button:active{transform:none}}
`;

const escape = value => String(value).replace(/[&<>"']/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c]));

export function renderLocalActionPage(action, title, localToken) {
  const pairing = action === '/pair';
  const label = pairing ? 'Create a pairing code / 生成配对码' : 'Open login window / 打开登录窗口';
  const description = pairing
    ? 'Create a one-use invitation on your own Relay. This replaces any older invitation; existing phones stay paired.'
    : 'Open DropRun’s separate browser profile to sign in to the selected media platform. Its login state stays on this computer.';
  const descriptionZh = pairing
    ? '在你的中转服务生成一次性邀请。旧邀请将作废，已连接的手机不受影响。'
    : '打开 DropRun 独立的浏览器身份，登录所选素材平台。登录状态保存在这台电脑。';
  const scriptValue = value => JSON.stringify(value).replace(/</g, '\\u003c');
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light dark"><title>${escape(title)} · DropRun</title><style>${connectionPageStyles}</style></head>
<body><main><div class="wordmark">DropRun</div><h1>${escape(title)}</h1><p>${description}</p><p>${descriptionZh}</p><section class="panel"><button class="primary" id="action" type="button">${label}</button><p id="error" role="alert"></p></section></main>
<script>document.getElementById('action').onclick=async()=>{const b=document.getElementById('action');if(b.disabled)return;b.disabled=true;b.textContent='Working… / 正在处理…';try{const r=await fetch(${scriptValue(action)},{method:'POST',headers:{'X-DropRun-Local':${scriptValue(localToken)}}});const body=await r.text();if(!r.ok)throw new Error(body);document.open();document.write(body);document.close();}catch(e){document.getElementById('error').textContent=e.message;b.disabled=false;b.textContent=${scriptValue(label)};}};</script></body></html>`;
}

export function renderMediaLoginPage() {
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light dark"><title>Connect a media account · DropRun</title><style>${connectionPageStyles}</style></head><body><main><div class="wordmark">DropRun</div><h1>Continue in the browser.<br>在浏览器中继续。</h1><p>Complete sign-in to the media platform in the window already opened by DropRun.</p><p>在 DropRun 已打开的窗口中完成素材平台登录。</p><section class="panel"><p>This is DropRun’s separate browser identity. Login data stays in the local DropRun data folder on this computer.</p><p>这是 DropRun 独立的浏览器身份。登录数据保存在这台电脑的 DropRun 数据目录。</p><p class="note">Keep the window open while sign-in is checked. It closes when the local check finishes or times out. If the window does not close automatically, close the window opened by DropRun, wait a moment, then retry the update. If materials still need sign-in, return to setup and connect again.</p><p class="note">请在检查登录状态时保留窗口；检查结束或超时后会关闭。如果窗口未自动关闭，请手动关闭 DropRun 打开的窗口，稍候再重试更新。如材料仍需登录，请回到安装设置重新连接。</p></section></main></body></html>`;
}

export async function renderPairPage(relay, pairing) {
  const legacy = !pairing.instanceId;
  const headingEn = legacy ? 'Legacy pairing invitation.' : 'Connect your phone.';
  const headingZh = legacy ? '旧版配对邀请。' : '连接你的手机。';
  const guideEn = legacy ? 'This private legacy invitation has no instance ID. Upgrade the Relay and generate a new invitation before connecting the public Android app. This code is for the older private app only.' : 'Open DropRun on Android and choose Scan to connect. Confirm this computer and Relay on your phone.';
  const guideZh = legacy ? '此私人旧版邀请没有实例 ID。连接公开安卓版前，请先升级中转服务并生成新邀请；当前配对码仅供旧版私人 App 使用。' : '在安卓 DropRun 选择扫码连接，再在手机核对这台电脑和中转服务。';
  const link = pairing.instanceId ? pairingLink(relay, pairing.instanceId, pairing.code) : relay + '/pair#code=' + pairing.code;
  const svg = await QRCode.toString(link, { type:'svg', margin:1, width:320 });
  const expiresAt = Number.isFinite(Number(pairing.expiresAt)) ? Number(pairing.expiresAt) : 0;
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light dark"><title>${legacy ? 'Legacy invitation' : 'Connect your phone'} · DropRun</title><style>${connectionPageStyles}</style></head>
<body><main><header><div class="wordmark">DropRun</div><button id="language" type="button">中文</button></header>
<h1 data-en="${headingEn}" data-zh="${headingZh}">${headingEn}</h1><p data-en="${guideEn}" data-zh="${guideZh}">${guideEn}</p>
<section class="panel"><div class="qr" id="qr" role="img">${svg}</div><p class="note" id="countdown" aria-live="off"></p><p id="expiry" class="note" aria-live="polite"></p>
<p class="note" data-en="Your Relay" data-zh="你的中转服务">Your Relay</p><code class="pairing-link">${escape(relay)}</code>
<p class="note" data-en="Instance" data-zh="实例">Instance</p><code class="pairing-link">${escape(pairing.instanceId || 'Private legacy instance')}</code>
<div id="pairing-values"><p class="note" data-en="Pairing code" data-zh="配对码">Pairing code</p><code id="pairing-code" tabindex="0">${escape(pairing.code)}</code>
<div class="actions"><button id="copy-code" type="button" data-en="Copy code" data-zh="复制配对码">Copy code</button><button id="copy-link" type="button" data-en="Copy pairing link" data-zh="复制配对链接">Copy pairing link</button></div>
<details><summary data-en="Full pairing link" data-zh="完整配对链接">Full pairing link</summary><code class="pairing-link" id="pairing-link" tabindex="0">${escape(link)}</code></details></div>
<p id="copy-status" class="note" role="status"></p></section>
<div class="actions">${legacy ? '<a class="button primary" href="https://github.com/MrMaii/droprun/blob/main/docs/technical/SELF_HOSTING.md" data-en="Relay upgrade guide" data-zh="中转升级指南">Relay upgrade guide</a>' : ''}<a class="button ${legacy ? '' : 'primary'}" href="/pair" data-en="Get a new code" data-zh="获取新配对码">Get a new code</a><a class="button" href="https://github.com/MrMaii/droprun/releases" data-en="Download Android" data-zh="下载安卓版">Download Android</a></div>
<p class="note" data-en="A code works once. Getting a new code asks for confirmation and invalidates the old invitation; existing phones stay paired." data-zh="配对码只能使用一次。新码需确认生成，会作废旧邀请；已连接的手机不受影响。">A code works once. Getting a new code asks for confirmation and invalidates the old invitation; existing phones stay paired.</p></main>
<script>(${pairPageClient.toString()})(${expiresAt},${legacy});</script></body></html>`;
}

// Kept in this renderer so it can be exercised without starting the Connector.
function pairPageClient(expiresAt, legacy) {
  let language = 'en', copied = '', copying = false, expired = false, timer;
  try { if(localStorage.getItem('droprun-pair-language') === 'zh') language = 'zh'; } catch {}
  const el = id => document.getElementById(id);
  const words = (en,zh) => language === 'zh' ? zh : en;
  function status() {
    el('copy-status').textContent = copied === 'code' ? words('Pairing code copied.','配对码已复制。') : copied === 'link' ? words('Pairing link copied.','配对链接已复制。') : copied === 'error' ? words('Copy unavailable. Select the code or link to copy it manually.','暂时无法复制，请选中配对码或链接手动复制。') : '';
  }
  function updateExpiry() {
    const seconds = Math.max(0,Math.ceil((expiresAt-Date.now())/1000));
    expired = seconds === 0;
    el('qr').hidden = expired; el('pairing-values').hidden = expired;
    for(const id of ['copy-code','copy-link']) el(id).disabled = expired || copying;
    el('countdown').hidden = expired;
    el('countdown').textContent = words('One use · ', '仅使用一次 · ') + Math.floor(seconds/60) + ':' + String(seconds%60).padStart(2,'0');
    el('expiry').textContent = expired ? words('This code has expired. Get a new code to continue.','配对码已过期，请获取新码后继续。') : '';
    el('expiry').className = 'note' + (expired ? ' expired' : '');
    if(expired) { copied = ''; status(); clearInterval(timer); }
  }
  function translate() {
    document.documentElement.lang = language === 'zh' ? 'zh-CN' : 'en';
    document.title = legacy ? words('Legacy invitation · DropRun','旧版邀请 · DropRun') : words('Connect your phone · DropRun','连接手机 · DropRun');
    el('language').textContent = language === 'zh' ? 'English' : '中文';
    for(const item of document.querySelectorAll('[data-en]')) item.textContent = item.dataset[language];
    el('qr').setAttribute('aria-label',legacy ? words('Invitation for the older private Android app.','旧版私人安卓版配对邀请。') : words('Scan this code in DropRun on Android.','在安卓 DropRun 扫描此配对码。'));
    status(); updateExpiry();
  }
  el('language').onclick = () => { language = language === 'en' ? 'zh' : 'en'; try {localStorage.setItem('droprun-pair-language',language);} catch {} translate(); };
  async function copy(kind) {
    updateExpiry(); if(expired || copying) return;
    copying = true; updateExpiry(); copied = ''; status();
    try { await navigator.clipboard.writeText(el('pairing-'+kind).textContent); copied = kind; }
    catch { copied = 'error'; const target=el('pairing-'+kind); if(kind==='link')target.parentElement.open=true; target.focus(); }
    finally { copying = false; updateExpiry(); status(); }
  }
  el('copy-code').onclick = () => copy('code'); el('copy-link').onclick = () => copy('link');
  translate(); if(!expired) timer = setInterval(updateExpiry,1000);
}
