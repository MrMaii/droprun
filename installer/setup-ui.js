const token = location.hash.slice(1);
history.replaceState(null, '', location.pathname);
let language = navigator.language.startsWith('zh') ? 'zh' : 'en';
const el = id => document.getElementById(id);
function translate() { document.documentElement.lang = language === 'zh' ? 'zh-CN' : 'en'; document.querySelectorAll('[data-en]').forEach(node => { const value = node.dataset[language]; if (node.tagName === 'H1') node.innerHTML = value; else node.textContent = value; }); el('language').textContent = language === 'zh' ? 'English' : '中文'; }
el('language').onclick = () => { language = language === 'en' ? 'zh' : 'en'; translate(); renderDoctor(); renderActions(); renderRelease(); };
async function api(path, body) { const response = await fetch('/api/' + path, { method: body === undefined ? 'GET' : 'POST', cache: 'no-store', headers: { 'X-DropRun-Setup': token, 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(path === 'doctor' ? 30000 : path === 'status' ? 5000 : 20000) }); const result = await response.json(); if (!response.ok) throw new Error(result.error); return result; }
let pendingAction = '', activeAction = '', actionError = '', statusError = '', serverBusy = false, statusReady = false, lastState = null, statusRequest = 0, statusReads = 0, restored = false;
function renderActions() {
  const locked = !!pendingAction || serverBusy || !statusReady;
  const labels = { login: ['Connecting…', '连接中…'], tools: ['Installing…', '安装中…'], deploy: ['Preparing Relay…', '正在准备 Relay…'], start: ['Starting…', '启动中…'] };
  for (const id of Object.keys(labels)) {
    const button = el(id), working = pendingAction === id || (serverBusy && activeAction === id);
    button.disabled = locked; button.setAttribute('aria-busy', String(working));
    button.textContent = working ? words(...labels[id]) : id === 'deploy' && restored ? words('Resume setup / verify connection', '继续安装 / 检查连接') : button.dataset[language];
  }
  for (const id of ['account', 'name', 'cost']) el(id).disabled = !!pendingAction || serverBusy;
  el('step').textContent = pendingAction ? words(...labels[pendingAction]) : lastState?.step || '';
  el('error').textContent = [actionError, statusError].filter(Boolean).join(' ') || lastState?.error || '';
  el('error').hidden = !el('error').textContent;
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
function renderDoctor() {
  el('check').disabled = checking;
  el('check').textContent = checking ? words('Checking…', '检查中…') : words('Check again', '重新检查');
  el('doctor').setAttribute('aria-busy', String(checking));
  el('project-list').setAttribute('aria-label', words('Local projects', '本机项目'));
  el('doctor').replaceChildren(); el('project-list').replaceChildren();
  const status = diagnostic?.codexStatus;
  el('project-inventory').hidden = !status?.ready;
  if (!diagnostic) { el('doctor-note').textContent = checking ? words('Checking Codex and local projects…', '正在检查 Codex 和本地项目…') : diagnosticError; return; }
  for (const [name, value] of [['Node', true], ['Git', diagnostic.git], ['Codex', diagnostic.codex], [words('Browser', '浏览器'), diagnostic.browser], ['Wrangler', diagnostic.wrangler], [words('Media tools', '媒体工具'), Object.values(diagnostic.media).every(Boolean)]]) {
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
el('login').onclick = () => act('login');
el('tools').onclick = () => act('tools');
el('deploy').onclick = () => act('deploy', { accountId: el('account').value.trim(), name: el('name').value.trim(), costAccepted: el('cost').checked });
el('start').onclick = () => act('start');
let checkingUpdates = false, releaseResult = null, releaseError = '';
function renderRelease() {
  el('updates').disabled = checkingUpdates; el('updates').setAttribute('aria-busy', String(checkingUpdates));
  el('updates').textContent = checkingUpdates ? words('Checking…', '检查中…') : el('updates').dataset[language];
  el('release').textContent = checkingUpdates ? words('Checking releases…', '正在检查版本…') : releaseError;
  if (!checkingUpdates && releaseResult) {
    el('release').replaceChildren(document.createTextNode(`${releaseResult.current} → ${releaseResult.latest?.tag || '—'} `));
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
