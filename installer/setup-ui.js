const token = location.hash.slice(1);
history.replaceState(null, '', location.pathname);
let language = navigator.language.startsWith('zh') ? 'zh' : 'en';
const el = id => document.getElementById(id);
function translate() { document.documentElement.lang = language === 'zh' ? 'zh-CN' : 'en'; document.querySelectorAll('[data-en]').forEach(node => { const value = node.dataset[language]; if (node.tagName === 'H1') node.innerHTML = value; else node.textContent = value; }); el('language').textContent = language === 'zh' ? 'English' : '中文'; el('account').placeholder = words('32-character account ID', '32 位账号 ID'); if (deploymentAttempted) validateDeployment(); }
el('language').onclick = () => { language = language === 'en' ? 'zh' : 'en'; translate(); renderDoctor(); renderActions(); renderRelease(); };
async function api(path, body) { const response = await fetch('/api/' + path, { method: body === undefined ? 'GET' : 'POST', cache: 'no-store', headers: { 'X-DropRun-Setup': token, 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(path === 'doctor' ? 30000 : path === 'status' ? 5000 : 20000) }); const result = await response.json(); if (!response.ok) throw new Error(result.error); return result; }
let pendingAction = '', activeAction = '', actionError = '', statusError = '', serverBusy = false, statusReady = false, lastState = null, statusRequest = 0, statusReads = 0, restored = false;
let deploymentAttempted = false;
function renderActions() {
  const locked = !!pendingAction || serverBusy || !statusReady;
  const labels = { login: ['Connecting…', '连接中…'], tools: ['Installing…', '安装中…'], deploy: ['Preparing Relay…', '正在准备 Relay…'], start: ['Starting…', '启动中…'] };
  for (const id of Object.keys(labels)) {
    const button = el(id), working = pendingAction === id || (serverBusy && activeAction === id);
    button.disabled = locked; button.setAttribute('aria-busy', String(working));
    button.textContent = working ? words(...labels[id]) : id === 'deploy' && restored ? words('Resume setup / verify connection', '继续安装 / 检查连接') : button.dataset[language];
  }
  for (const id of ['account', 'name', 'cost']) el(id).disabled = !!pendingAction || serverBusy;
  el('step').textContent = pendingAction ? words(...labels[pendingAction]) : stepLabel(lastState?.step);
  el('error').textContent = [actionError, statusError].filter(Boolean).join(' ') || lastState?.error || '';
  el('error').hidden = !el('error').textContent;
  renderConnection();
}
async function act(path, body = {}) {
  if (pendingAction || serverBusy || !statusReady) return;
  pendingAction = path; actionError = ''; ++statusRequest;
  el(path).after(el('error'));
  if (lastState) lastState = { ...lastState, error: null };
  renderActions();
  try {
    await api(path, body); activeAction = path; serverBusy = true;
  } catch (error) {
    actionError = error.message;
  } finally {
    // A failed acknowledgement may still have started work. Read status before
    // allowing another action; never retry a mutation automatically.
    statusReady = false; await refresh(); pendingAction = '';
    if (!serverBusy) activeAction = '';
    renderActions();
    if (actionError) el('error').scrollIntoView({ block: 'nearest' });
  }
}
let diagnostic = null, diagnosticError = '', checking = false;
const words = (en, zh) => language === 'zh' ? zh : en;
function validateDeployment(moveFocus = false) {
  const errors = {
    account: /^[a-f0-9]{32}$/i.test(el('account').value.trim()) ? '' : words('Enter a 32-character Cloudflare account ID using 0–9 and a–f.', '请输入由 0–9、a–f 组成的 32 位 Cloudflare 账号 ID。'),
    name: /^[a-z][a-z0-9-]{2,43}$/.test(el('name').value.trim()) ? '' : words('Use 3–44 lowercase letters, numbers or hyphens, starting with a letter.', '请使用 3–44 位小写字母、数字或连字符，并以字母开头。'),
    cost: el('cost').checked ? '' : words('Confirm the selected account and possible Cloudflare usage charges before creating resources.', '创建资源前，请确认所选账号及可能产生的 Cloudflare 用量费用。')
  };
  for (const [id, message] of Object.entries(errors)) {
    el(id).setAttribute('aria-invalid', String(!!message));
    el(id + '-error').textContent = message; el(id + '-error').hidden = !message;
  }
  const first = Object.keys(errors).find(id => errors[id]);
  if (first && moveFocus) el(first).focus();
  return !first;
}
function stepLabel(step = '') {
  const labels = { 'Cloudflare login': ['Connecting Cloudflare', '正在连接 Cloudflare'], 'Cloudflare connected': ['Cloudflare connected', 'Cloudflare 已连接'], 'Installing upstream media tools': ['Installing media tools', '正在安装媒体工具'], 'Media tools ready': ['Media tools ready', '媒体工具已准备好'], 'Relay ready — start your Connector': ['Relay verified — start your Connector', 'Relay 已验证，可启动 Connector'], 'Starting Connector': ['Starting Connector', '正在启动 Connector'], 'Connector started': ['Connector started', 'Connector 已启动'], configuration: ['Preparing your instance', '正在准备实例'], 'd1-resource': ['Preparing the database', '正在准备数据库'], 'r2-resource': ['Preparing file storage', '正在准备文件存储'], resources: ['Deploying your Relay', '正在部署 Relay'], database: ['Preparing database tables', '正在准备数据表'], preview: ['Preparing static previews', '正在准备静态预览'], connect: ['Connecting your computer', '正在连接电脑'], verify: ['Checking the Relay and preview', '正在核对 Relay 与预览'] };
  return labels[step] ? words(...labels[step]) : step;
}
function renderConnection() {
  const verified = lastState?.completed, relay = verified?.relay || lastState?.saved?.relay;
  el('relay-summary').hidden = !relay;
  el('relay-status').textContent = verified?.relay ? words('Last verified setup', '上次验证的配置') : words('Saved setup', '已保存的配置');
  el('relay-address').textContent = relay || '';
  el('relay-instance-row').hidden = !verified?.instanceId;
  el('relay-instance').textContent = verified?.instanceId || '';
  el('relay-note').textContent = verified?.relay ? words('Verified for this computer. Pairing still asks you to confirm the computer and server on Android.', '已为这台电脑验证。配对时仍需在 Android 上确认电脑与服务器。') : words('Saved settings are not a current health check. Resume setup to verify this connection.', '已保存不代表当前连接已验证。继续安装，核对这套连接。');
  const started = lastState?.step === 'Connector started' && !lastState?.error;
  const connection = diagnostic?.connection, online = connection?.online === true;
  const pairReady = statusReady && !pendingAction && !serverBusy && online;
  el('pair-link').setAttribute('aria-disabled', String(!pairReady));
  if (pairReady) { el('pair-link').href = 'http://127.0.0.1:47493/pair'; el('pair-link').removeAttribute('tabindex'); }
  else { el('pair-link').removeAttribute('href'); el('pair-link').tabIndex = -1; }
  el('pair-note').textContent = connection?.online === false ? words('Your computer responded, but the Relay is offline. Check its network connection, then choose Check again.', '电脑已响应，但 Relay 离线。检查网络连接，再选择「重新检查」。') : connection && !online ? words('Your computer responded. The Relay connection is not confirmed; choose Check again.', '电脑已响应，Relay 连接尚未确认。请选择「重新检查」。') : pairReady ? words('Open a fresh code, then verify your computer and Relay in the Android app.', '打开新配对码，再在 Android App 中核对电脑与 Relay。') : words('Start the Connector, then choose Check again to confirm the computer and Relay connection.', '启动 Connector 后，选择「重新检查」，确认电脑与 Relay 连接。');
  let next;
  if (!statusReady) next = words('Reading setup status. If unavailable, reopen DropRun Setup from its shortcut.', '正在读取配置状态。如无法连接，请从快捷方式重新打开安装向导。');
  else if (pendingAction || serverBusy) next = words('A step is running. Keep this window open; another action will wait.', '当前步骤正在进行。保留本窗口，下一项操作会等待。');
  else if (lastState?.saved?.relay && !verified?.relay && !connection) next = words('Next: resume setup to verify your Relay before starting the Connector.', '下一步：继续安装，核对 Relay 连接，再启动 Connector。');
  else if ((connection && !online) || (started && !connection)) next = words('Next: check the connection, then choose Check again before pairing.', '下一步：检查连接，再选择「重新检查」，确认后配对。');
  else if (!diagnostic || !diagnostic.codex || !diagnostic.git || !diagnostic.browser) next = words('Next: finish the computer checks before creating cloud resources.', '下一步：先完成电脑检查，再创建云资源。');
  else if (pairReady) next = words('Next: open the pairing code and connect Android.', '下一步：打开配对码，连接 Android 手机。');
  else if (relay || diagnostic.configured) next = words('Next: start the Connector, then check its connection.', '下一步：启动 Connector，再检查连接。');
  else next = words('Next: connect Cloudflare, choose your account, and create your Relay.', '下一步：连接 Cloudflare，选择账号，创建你的 Relay。');
  el('next-step').textContent = next;
}
function renderDoctor() {
  for (const id of ['check', 'connection-check']) {
    el(id).disabled = checking;
    el(id).textContent = checking ? words('Checking…', '检查中…') : words('Check again', '重新检查');
  }
  el('doctor').setAttribute('aria-busy', String(checking));
  el('project-list').setAttribute('aria-label', words('Local projects', '本机项目'));
  el('doctor').replaceChildren(); el('project-list').replaceChildren();
  renderConnection();
  const status = diagnostic?.codexStatus;
  el('project-inventory').hidden = !status?.ready;
  if (!diagnostic) { el('doctor-note').textContent = checking ? words('Checking Codex and local projects…', '正在检查 Codex 和本地项目…') : diagnosticError; return; }
  for (const [name, value] of [['Node', true], ['Git', diagnostic.git], ['Codex', diagnostic.codex], [words('Browser', '浏览器'), diagnostic.browser], ['Wrangler', diagnostic.wrangler], [words('Media tools · optional', '媒体工具 · 可选'), Object.values(diagnostic.media).every(Boolean)]]) {
    const badge = document.createElement('span'); badge.className = 'check' + (value ? '' : ' missing'); badge.textContent = `${value ? '✓' : '○'} ${name}`; el('doctor').append(badge);
  }
  const hints = { 'missing': ['Install Codex desktop, then sign in.', '请安装 Codex 桌面版并登录。'], 'login-required': ['Sign in to Codex, then check again.', '请登录 Codex 后重新检查。'], 'unsupported': ['Update Codex desktop: this version does not support the required project API.', '请更新 Codex 桌面版：当前版本不支持所需项目接口。'], 'app-server-failed': ['Codex app server could not start. Open Codex and try again.', 'Codex 服务未启动。请先打开 Codex 后重试。'], 'timeout': ['Codex did not respond within 10 seconds. Open Codex and check again.', 'Codex 在 10 秒内没有响应。请打开 Codex 后重新检查。'] };
  el('doctor-note').textContent = status?.ready ? words(`Codex connected · ${status.projectCount} existing projects`, `Codex 已连接 · ${status.projectCount} 个已有项目`) : (hints[status?.status]?.[language === 'zh' ? 1 : 0] || '');
  if (!status?.ready) return;
  const projects = status.projects || [];
  el('project-empty').hidden = projects.length > 0;
  el('project-list').hidden = projects.length === 0;
  for (const project of projects) {
    const row = document.createElement('li'), heading = document.createElement('div'), name = document.createElement('strong'), badge = document.createElement('span'), id = document.createElement('code'), roots = document.createElement('ul');
    row.className = 'project'; heading.className = 'project-heading'; name.textContent = project.name;
    badge.className = 'check' + (project.available ? '' : ' missing'); badge.textContent = project.available ? words('Available', '可用') : words('Unavailable', '暂不可用');
    id.className = 'project-id'; id.textContent = `ID · ${project.id}`;
    heading.append(name, badge); row.append(heading, id);
    for (const root of project.roots) { const item = document.createElement('li'); item.textContent = root.path + (root.available ? '' : words(' · Folder unavailable', ' · 目录不可用')); roots.append(item); }
    row.append(roots);
    if (!project.available) { const help = document.createElement('p'); help.className = 'note'; help.textContent = words('Open the project folder in Codex, then check again.', '请在 Codex 中打开项目目录，再重新检查。'); row.append(help); }
    el('project-list').append(row);
  }
}
async function doctor() {
  if (checking) return;
  checking = true; diagnostic = null; diagnosticError = ''; renderDoctor();
  try { diagnostic = await api('doctor'); } catch (error) { diagnosticError = error.message; }
  finally { checking = false; renderDoctor(); }
}
async function refresh() {
  const request = ++statusRequest; statusReads++;
  try {
    const state = await api('status'); if (request !== statusRequest) return;
    lastState = state; statusReady = true; statusError = ''; serverBusy = !!state.busy;
    if (!serverBusy && !pendingAction) activeAction = '';
    el('messages').textContent = state.messages.join('\n');
    el('accounts').replaceChildren(...state.accounts.map(value => { const option = document.createElement('option'); option.value = value; return option; }));
    if (state.accounts.length === 1 && !el('account').value) el('account').value = state.accounts[0];
    if (!restored && state.saved) { el('name').value = state.saved.name; el('account').value = state.saved.accountId; restored = true; }
  } catch (error) {
    if (request !== statusRequest) return;
    statusReady = false; statusError = words('Cannot check setup status: ', '无法检查配置状态：') + error.message;
  } finally { statusReads--; if (request === statusRequest) renderActions(); }
}
el('check').onclick = doctor;
el('connection-check').onclick = doctor;
el('login').onclick = () => act('login');
el('tools').onclick = () => act('tools');
el('deployment-form').onsubmit = event => {
  event.preventDefault();
  if (pendingAction || serverBusy || !statusReady) return;
  deploymentAttempted = true;
  if (validateDeployment(true)) act('deploy', { accountId: el('account').value.trim(), name: el('name').value.trim(), costAccepted: el('cost').checked });
};
for (const id of ['account', 'name', 'cost']) el(id).addEventListener('input', () => { if (deploymentAttempted) validateDeployment(); });
el('start').onclick = () => act('start');
let checkingUpdates = false, releaseResult = null, releaseError = '';
function renderRelease() {
  el('updates').disabled = checkingUpdates; el('updates').setAttribute('aria-busy', String(checkingUpdates));
  el('updates').textContent = checkingUpdates ? words('Checking…', '检查中…') : el('updates').dataset[language];
  el('release').textContent = checkingUpdates ? words('Checking releases…', '正在检查版本…') : releaseError;
  if (!checkingUpdates && releaseResult) {
    el('release').replaceChildren(document.createTextNode(`${releaseResult.current} → ${releaseResult.latest?.tag || '—'}${releaseResult.latest?.prerelease ? words(' · Public preview', ' · 公开预览版') : ''} `));
    const link = document.createElement('a'); link.href = releaseResult.downloadUrl; link.target = '_blank'; link.rel = 'noreferrer'; link.textContent = words('Release notes & downloads ↗', '发布说明与下载 ↗'); el('release').append(link);
  }
}
el('updates').onclick = async () => {
  if (checkingUpdates) return;
  checkingUpdates = true; releaseResult = null; releaseError = ''; renderRelease();
  try { releaseResult = await api('updates'); } catch (error) { releaseError = error.message; }
  finally { checkingUpdates = false; renderRelease(); }
};
translate(); renderActions(); doctor(); refresh(); setInterval(() => { if (!statusReads) refresh(); }, 2500);
