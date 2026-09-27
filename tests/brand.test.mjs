import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { Miniflare } from './miniflare.mjs';

test('public brand uses exact selected bytes; publishing is administrator-only and cannot expose private objects', async () => {
  const token = 'test-brand-admin-long-enough';
  const connector = 'test-brand-connector-long-enough';
  const digest = b => createHash('sha256').update(b).digest('hex');
  const mf = new Miniflare({ modules:true, scriptPath:'relay/worker.mjs', compatibilityDate:'2026-08-06', d1Databases:['DB'], r2Buckets:['FILES'], bindings:{CONNECTOR_HASH:digest(connector),ADMIN_HASH:digest(token)} });
  try {
    const db = await mf.getD1Database('DB');
    await db.prepare('CREATE TABLE devices(id TEXT,token_hash TEXT)').run();
    const bytes = await readFile('assets/brand/mark.png');
    const put = (body, auth='') => mf.dispatchFetch('https://brand.test/connector/brand-logo', { method:'PUT', headers:{Authorization:'Bearer '+auth,'Content-Length':String(body.length)}, body });
    assert.equal((await put(bytes)).status,401);
    await db.prepare('INSERT INTO devices VALUES (?,?)').bind('phone',digest('phone-token')).run();
    assert.equal((await put(bytes,'phone-token')).status,403);
    assert.equal((await put(bytes,connector)).status,403);
    assert.equal((await put(Buffer.from('not-the-logo'),token)).status,400);
    assert.equal((await put(bytes,token)).status,200);
    const logo = await mf.dispatchFetch('https://brand.test/brand/droprun-v5.png');
    assert.equal(logo.status,200);assert.equal(logo.headers.get('content-type'),'image/png');
    assert.equal(digest(Buffer.from(await logo.arrayBuffer())),digest(bytes));
    assert.equal((await mf.dispatchFetch('https://brand.test/brand/private.png')).status,401);
    const page = await (await mf.dispatchFetch('https://brand.test/')).text();
    assert.match(page,/self-hosted/);assert.match(page,/prefers-reduced-motion/);assert.match(page,/releases\/latest/);
    assert.doesNotMatch(page,new RegExp(token));
  } finally { await mf.dispose(); }
});
