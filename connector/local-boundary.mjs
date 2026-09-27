import { timingSafeEqual } from 'node:crypto';

export function trustedLocalRequest(request, port) {
  if (!['127.0.0.1', '::1', '::ffff:127.0.0.1'].includes(request.socket?.remoteAddress)) return false;
  const expected = `127.0.0.1:${port}`;
  if (request.headers.host !== expected && request.headers.host !== `localhost:${port}`) return false;
  const origin = request.headers.origin;
  if (origin && origin !== `http://${expected}` && origin !== `http://localhost:${port}`) return false;
  if (request.headers['sec-fetch-site'] === 'cross-site') return false;
  return true;
}

export function matchesLocalToken(actual, expected) {
  return typeof actual === 'string' && typeof expected === 'string' && actual.length === expected.length && timingSafeEqual(Buffer.from(actual), Buffer.from(expected));
}
