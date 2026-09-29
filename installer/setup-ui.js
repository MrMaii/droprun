const token = location.hash.slice(1);
history.replaceState(null, '', location.pathname);
let language = navigator.language.startsWith('zh') ? 'zh' : 'en';
const el = id => document.getElementById(id);
function translate() { document.documentElement.lang = language === 'zh' ? 'zh-CN' : 'en'; document.querySelectorAll('[data-en]').forEach(node => { const value = node.dataset[language]; if (node.tagName === 'H1') node.innerHTML = value; else node.textContent = value; }); el('language').textContent = language === 'zh' ? 'English' : '中文'; }
el('language').onclick = () => { language = language === 'en' ? 'zh' : 'en'; translate(); renderDoctor(); };
async function api(path, body) { const response = await fetch('/api/' + path, { method: body === undefined ? 'GET' : 'POST', headers: { 'X-DropRun-Setup': token, 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) }); const result = await response.json(); if (!response.ok) throw new Error(result.error); return result; }
async function act(path, body = {}) { try { await api(path, body); await refresh(); } catch (error) { el('error').textContent = error.message; } }
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
let restored = false;
async function refresh() { try { const state = await api('status'); el('step').textContent = state.step; el('error').textContent = state.error || ''; el('messages').textContent = state.messages.join('\n'); ['login', 'deploy', 'start'].forEach(id => { el(id).disabled = state.busy; }); el('accounts').replaceChildren(...state.accounts.map(value => { const option = document.createElement('option'); option.value = value; return option; })); if (state.accounts.length === 1 && !el('account').value) el('account').value = state.accounts[0]; if (!restored && state.saved) { el('name').value = state.saved.name; el('account').value = state.saved.accountId; el('deploy').textContent = language === 'zh' ? '继续安装 / 检查连接' : 'Resume setup / verify connection'; restored = true; } } catch (error) { el('error').textContent = error.message; } }
el('check').onclick = doctor;
el('login').onclick = () => act('login');
el('tools').onclick = () => act('tools');
el('deploy').onclick = () => act('deploy', { accountId: el('account').value.trim(), name: el('name').value.trim(), costAccepted: el('cost').checked });
el('start').onclick = () => act('start');
el('updates').onclick = async () => { try { const result = await api('updates'); el('release').replaceChildren(document.createTextNode(`${result.current} → ${result.latest?.tag || '—'} `)); const link = document.createElement('a'); link.href = result.downloadUrl; link.target = '_blank'; link.rel = 'noreferrer'; link.textContent = language === 'zh' ? '发布说明与下载 ↗' : 'Release notes & downloads ↗'; el('release').append(link); } catch (error) { el('release').textContent = error.message; } };
translate(); doctor(); refresh(); setInterval(refresh, 2500);
