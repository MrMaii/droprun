// Page identity includes query and fragment routes, while remaining inside the preview origin.
export function targetRoute(route) {
  if (typeof route !== 'string' || !route.startsWith('/') || route.startsWith('//') || /[\\\s\u0000-\u001f\u007f]/.test(route)) throw new Error('页面地址必须是站内路径，可包含查询参数或锚点');
  const url = new URL(route, 'http://preview');
  if (url.origin !== 'http://preview') throw new Error('页面地址不能指向其他网站');
  return (url.pathname.replace(/\/$/, '') || '/') + url.search + url.hash;
}

export function matchesTargetRoute(url, route, origin) {
  try {
    const actual = new URL(url);
    return (!origin || actual.origin === new URL(origin).origin) && targetRoute(actual.pathname + actual.search + actual.hash) === targetRoute(route);
  } catch { return false; }
}
