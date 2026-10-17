import { AbstractControl } from '@angular/forms';

export interface ErrorText {
  key: string;
  params?: Record<string, unknown>;
}

/** Primeiro erro do controle como chave i18n (validators do cliente e `server` mapeado do servidor). */
export function firstError(c: AbstractControl | null): ErrorText | null {
  const e = c?.errors;
  if (!e) {
    return null;
  }
  if (e['required']) {
    return { key: 'validation.required' };
  }
  if (e['server']) {
    return { key: `field.${String(e['server'])}` };
  }
  if (e['email']) {
    return { key: 'validation.email' };
  }
  if (e['tooShort']) {
    return { key: 'validation.tooShort', params: { min: (e['tooShort'] as { min: number }).min } };
  }
  if (e['tooLong']) {
    return { key: 'validation.tooLong', params: { max: (e['tooLong'] as { max: number }).max } };
  }
  if (e['maxlength']) {
    return { key: 'validation.maxlength', params: { max: (e['maxlength'] as { requiredLength: number }).requiredLength } };
  }
  if (e['minlength']) {
    return { key: 'validation.minlength', params: { min: (e['minlength'] as { requiredLength: number }).requiredLength } };
  }
  if (e['pattern']) {
    return { key: 'validation.pattern' };
  }
  if (e['min'] || e['max']) {
    return { key: 'validation.range' };
  }
  if (e['origin']) {
    return { key: 'validation.origin' };
  }
  if (e['url']) {
    return { key: 'validation.url' };
  }
  if (e['audience']) {
    return { key: 'validation.audience' };
  }
  if (e['different']) {
    return { key: 'validation.different' };
  }
  return { key: 'validation.invalid' };
}
