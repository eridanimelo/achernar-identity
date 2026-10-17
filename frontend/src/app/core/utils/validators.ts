import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** E-mail (ui-map §4): trim, ≤ 254, um '@', sem espaços. A validação definitiva é do servidor. */
export const emailValidator: ValidatorFn = (c: AbstractControl): ValidationErrors | null => {
  const v = typeof c.value === 'string' ? c.value.trim() : '';
  if (!v) {
    return null; // 'required' cuida do vazio
  }
  if (v.length > 254 || /\s/.test(v) || v.split('@').length !== 2 || v.startsWith('@') || v.endsWith('@')) {
    return { email: true };
  }
  const domain = v.split('@')[1];
  return domain.includes('.') && !domain.startsWith('.') && !domain.endsWith('.') ? null : { email: true };
};

export function passwordLengthValidator(min = 12, max = 128): ValidatorFn {
  return (c) => {
    const v = typeof c.value === 'string' ? c.value : '';
    if (!v) {
      return null;
    }
    const len = [...v].length; // pontos de código
    if (len < min) {
      return { tooShort: { min } };
    }
    return len > max ? { tooLong: { max } } : null;
  };
}

/** Confere dois campos de um FormGroup (senha/confirmação). */
export function matchValidator(field: string, other: string): ValidatorFn {
  return (group) => {
    const a = group.get(field)?.value;
    const b = group.get(other)?.value;
    return a === b ? null : { mismatch: true };
  };
}

export const CLIENT_ID_PATTERN = /^[a-z0-9-]{3,64}$/;

/** Origem: https://host[:porta] sem caminho (http apenas para localhost/127.0.0.1). */
export function isValidOrigin(raw: string): boolean {
  if (!raw || raw !== raw.trim() || raw.includes('*')) {
    return false;
  }
  try {
    const u = new URL(raw);
    const local = u.hostname === 'localhost' || u.hostname === '127.0.0.1';
    const schemeOk = u.protocol === 'https:' || (u.protocol === 'http:' && local);
    return schemeOk && u.origin === raw && !u.username && !u.hash && !u.search && (u.pathname === '/' || u.pathname === '') && !raw.endsWith('/');
  } catch {
    return false;
  }
}

/** URL absoluta de retorno/ação: https (ou http local), sem fragmento nem curinga. */
export function isValidAbsoluteUrl(raw: string): boolean {
  if (!raw || raw !== raw.trim() || raw.includes('*') || raw.includes('#') || raw.length > 500) {
    return false;
  }
  try {
    const u = new URL(raw);
    const local = u.hostname === 'localhost' || u.hostname === '127.0.0.1';
    return (u.protocol === 'https:' || (u.protocol === 'http:' && local)) && !u.username;
  } catch {
    return false;
  }
}

export function isValidAudience(raw: string): boolean {
  return !!raw && raw.length <= 200 && /^[A-Za-z0-9:/._-]+$/.test(raw);
}

/** Estimativa simples de força (apenas dica visual; a política real é do servidor). */
export function passwordStrength(pw: string): 0 | 1 | 2 | 3 {
  const len = [...pw].length;
  if (len === 0) {
    return 0;
  }
  if (len < 12) {
    return 1;
  }
  const classes = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((r) => r.test(pw)).length;
  if (len >= 20 || (len >= 16 && classes >= 2)) {
    return 3;
  }
  return 2;
}
