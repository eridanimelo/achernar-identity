/**
 * Lê `token` do fragmento (`#token=...`) e o remove da barra de endereço (`history.replaceState`),
 * para que não permaneça no histórico nem vaze por Referer (ui-map SCR-03/08/12).
 */
export function takeFragmentToken(win: Window = window): string | null {
  const hash = win.location.hash.startsWith('#') ? win.location.hash.slice(1) : win.location.hash;
  const token = new URLSearchParams(hash).get('token');
  if (hash) {
    try {
      win.history.replaceState(win.history.state, '', win.location.pathname + win.location.search);
    } catch {
      // ambiente sem History API
    }
  }
  return token && /^[A-Za-z0-9_-]{16,256}$/.test(token) ? token : null;
}
