/** Acesso a localStorage/sessionStorage que nunca lança (modo privado, bloqueio, quota). */
export function safeGet(storage: 'local' | 'session', key: string): string | null {
  try {
    return (storage === 'local' ? localStorage : sessionStorage).getItem(key);
  } catch {
    return null;
  }
}

export function safeSet(storage: 'local' | 'session', key: string, value: string): void {
  try {
    (storage === 'local' ? localStorage : sessionStorage).setItem(key, value);
  } catch {
    // preferências são opcionais; falha silenciosa
  }
}

export function safeRemove(storage: 'local' | 'session', key: string): void {
  try {
    (storage === 'local' ? localStorage : sessionStorage).removeItem(key);
  } catch {
    // ignorado
  }
}
