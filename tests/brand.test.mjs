import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { Miniflare } from './miniflare.mjs';

test('public brand uses exact selected bytes; publishing is administrator-only and cannot expose private objects', async (t) => {
  const token = 'test-brand-admin-long-enough';
  const connector = 'test-brand-connector-long-enough';
  const digest = b => createHash('sha256').update(b).digest('hex');
  const mf = new Miniflare({ modules:true, scriptPath:'relay/worker.mjs', compatibilityDate:'2026-08-06', d1Databases:['DB'], r2Buckets:['FILES'], bindings:{CONNECTOR_HASH:digest(connector),ADMIN_HASH:digest(token)} });
  let stage = 'getD1Database(DB)';
  try {
    const db = await mf.getD1Database('DB');
    stage = 'D1 CREATE TABLE devices';
    await db.prepare('CREATE TABLE devices(id TEXT,token_hash TEXT)').run();
    stage = 'read selected mark.png';
    const bytes = await readFile('assets/brand/mark.png');
    const put = async (body, auth='') => {
      const requestStage = stage;
      stage = requestStage + ': dispatchFetch';
      const response = await mf.dispatchFetch('https://brand.test/connector/brand-logo', { method:'PUT', headers:{Authorization:'Bearer '+auth,'Content-Length':String(body.length)}, body });
      stage = requestStage + ': response.arrayBuffer';
      await response.arrayBuffer();
      stage = requestStage + ': status assertion';
      return response;
    };
    stage = 'PUT /connector/brand-logo anonymous';
    assert.equal((await put(bytes)).status,401);
    stage = 'D1 INSERT phone device';
    await db.prepare('INSERT INTO devices VALUES (?,?)').bind('phone',digest('phone-token')).run();
    stage = 'PUT /connector/brand-logo phone';
    assert.equal((await put(bytes,'phone-token')).status,403);
    stage = 'PUT /connector/brand-logo connector';
    assert.equal((await put(bytes,connector)).status,403);
    stage = 'PUT /connector/brand-logo admin invalid bytes';
    assert.equal((await put(Buffer.from('not-the-logo'),token)).status,400);
    stage = 'PUT /connector/brand-logo admin selected bytes';
    assert.equal((await put(bytes,token)).status,200);
    stage = 'GET /brand/droprun-v5.png: dispatchFetch';
    const logo = await mf.dispatchFetch('https://brand.test/brand/droprun-v5.png');
    stage = 'GET /brand/droprun-v5.png: status/content-type assertions';
    assert.equal(logo.status,200);assert.equal(logo.headers.get('content-type'),'image/png');
    stage = 'GET /brand/droprun-v5.png: response.arrayBuffer';
    const publicBytes = await logo.arrayBuffer();
    stage = 'GET /brand/droprun-v5.png: digest assertion';
    assert.equal(digest(Buffer.from(publicBytes)),digest(bytes));
    stage = 'GET /brand/private.png: dispatchFetch';
    const privateObject = await mf.dispatchFetch('https://brand.test/brand/private.png');
    stage = 'GET /brand/private.png: status assertion';
    assert.equal(privateObject.status,401);
    stage = 'GET /brand/private.png: response.arrayBuffer';
    await privateObject.arrayBuffer();
    stage = 'GET /: dispatchFetch';
    const pageResponse = await mf.dispatchFetch('https://brand.test/');
    stage = 'GET /: response.text';
    const page = await pageResponse.text();
    stage = 'GET /: page assertions';
    assert.match(page,/self-hosted/);assert.match(page,/prefers-reduced-motion/);assert.match(page,/droprun\/releases/);
    assert.doesNotMatch(page,new RegExp(token));
  } catch (error) {
    t.diagnostic('brand.test stage: '+stage);
    throw error;
  } finally {
    try { await mf.dispose(); }
    catch (error) {
      t.diagnostic('brand.test stage: Miniflare.dispose');
      throw error;
    }
  }
});
