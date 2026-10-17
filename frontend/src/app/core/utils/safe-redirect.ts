function hasControlChars(value: string): boolean {
  for (let i = 0; i < value.length; i++) {
    if (value.charCodeAt(i) <= 0x1f) {
      return true;
    }
  }
  return false;
}

/** Valida o parâmetro `next`: somente caminho relativo interno (evita open redirect). */
export function safeNext(next: string | null | undefined, fallback = '/account'): string {
  if (!next || typeof next !== 'string') {
    return fallback;
  }
  if (!next.startsWith('/') || next.startsWith('//') || next.includes('\\') || hasControlChars(next)) {
    return fallback;
  }
  try {
    const u = new URL(next, 'http://internal.invalid');
    if (u.origin !== 'http://internal.invalid') {
      return fallback;
    }
  } catch {
    return fallback;
  }
  const blocked = ['/login', '/register', '/session-expired'];
  const path = next.split(/[?#]/)[0];
  return blocked.includes(path) ? fallback : next;
}
