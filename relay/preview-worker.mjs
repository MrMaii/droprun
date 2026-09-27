import { digest } from './selfhost.mjs';
import { validSnapshotPath } from './snapshots.mjs';

const headers = {
  'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer', 'Access-Control-Allow-Origin': '*',
  'Content-Security-Policy': "sandbox allow-scripts; default-src 'self' data: blob:; script-src 'self' 'unsafe-inline' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline'; connect-src 'self'; object-src 'none'; form-action 'none'; base-uri 'self'; frame-ancestors 'none'",
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()'
};
const unavailable = (status = 410) => new Response('This preview is unavailable or expired. Open DropRun to request it again.', { status, headers: { ...headers, 'Content-Type': 'text/plain;charset=utf-8' } });
const scoped = (value, prefix) => value.startsWith('/') && !value.startsWith('//') && !value.startsWith(prefix) ? prefix + value.slice(1) : value;
export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);
      if (url.pathname === '/health') return Response.json({ service: 'DropRun Preview', instanceId: env.INSTANCE_ID || null, ready: !!env.INSTANCE_ID }, { headers: { 'Cache-Control': 'no-store' } });
      if (!['GET','HEAD'].includes(request.method)) return unavailable(405);
      const match = url.pathname.match(/^\/s\/([a-f0-9]{64})\/(.*)$/);
      if (!match) return unavailable(404);
      const session = await env.DB.prepare("SELECT p.* FROM preview_sessions s JOIN preview_snapshots p ON p.id=s.snapshot_id JOIN tasks t ON t.id=p.task_id JOIN devices d ON d.id=t.device_id JOIN project_permissions a ON a.device_id=t.device_id AND a.project_id=t.project_id WHERE s.token_hash=? AND s.expires_at>? AND p.expires_at>? AND p.ready=1 AND t.cancel_requested=0 AND a.enabled=1 AND a.version=t.permission_version").bind(await digest(match[1]),Date.now(),Date.now()).first();
      if (!session) return unavailable();
      let path;
      try { path = decodeURIComponent(match[2]); } catch { return unavailable(400); }
      if (!path || path.endsWith('/')) path += 'index.html';
      if (!validSnapshotPath(path)) return unavailable(400);
      let file = await env.DB.prepare('SELECT * FROM preview_files WHERE snapshot_id=? AND path=? AND ready=1').bind(session.id,path).first();
      if (!file && !path.endsWith('/index.html')) {
        const index = await env.DB.prepare('SELECT * FROM preview_files WHERE snapshot_id=? AND path=? AND ready=1').bind(session.id,path+'/index.html').first();
        if (index) { path += '/index.html'; file = index; }
      }
      if (!file && JSON.parse(session.manifest).spa && !path.split('/').at(-1).includes('.')) {
        path = JSON.parse(session.manifest).entryPath;
        file = await env.DB.prepare('SELECT * FROM preview_files WHERE snapshot_id=? AND path=? AND ready=1').bind(session.id,path).first();
      }
      if (!file) return unavailable(404);
      const object = await env.FILES.get('snapshots/' + session.id + '/' + path);
      if (!object) return unavailable();
      const prefix = '/s/' + match[1] + '/';
      const contentType = file.content_type.startsWith('text/') || file.content_type === 'application/json' ? file.content_type+';charset=utf-8' : file.content_type;
      const responseHeaders = { ...headers, 'Content-Type': contentType, 'X-DropRun-Version': session.version };
      if (request.method === 'HEAD') return new Response(null,{headers:responseHeaders});
      if (file.content_type === 'text/html') {
        // Root-relative static resources stay inside this immutable capability scope.
        return new HTMLRewriter().on('*', { element(element) {
          for (const attr of ['src','href','poster','action']) {
            const value = element.getAttribute(attr);
            if (value) element.setAttribute(attr,scoped(value,prefix));
          }
          const srcset = element.getAttribute('srcset');
          if (srcset) element.setAttribute('srcset',srcset.replace(/(^|,)(\s*)\/(?!\/)/g,'$1$2'+prefix));
        } }).transform(new Response(object.body,{headers:responseHeaders}));
      }
      if (file.content_type === 'text/css') {
        const css = (await object.text()).replace(/url\(\s*(['"]?)\/(?!\/)/g,(_all,quote) => 'url('+quote+prefix).replace(/@import\s+(['"])\/(?!\/)/g,(_all,quote) => '@import '+quote+prefix);
        return new Response(css,{headers:responseHeaders});
      }
      return new Response(object.body,{headers:responseHeaders});
    } catch { return unavailable(400); }
  }
};
