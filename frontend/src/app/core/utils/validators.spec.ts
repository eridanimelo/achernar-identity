import { FormControl, FormGroup } from '@angular/forms';
import {
  emailValidator, isValidAbsoluteUrl, isValidAudience, isValidOrigin, matchValidator, passwordLengthValidator, passwordStrength,
} from './validators';

describe('validators', () => {
  it('emailValidator', () => {
    const v = (s: string) => emailValidator(new FormControl(s));
    expect(v('')).toBeNull();
    expect(v('a@b.co')).toBeNull();
    expect(v(' a@b.co ')).toBeNull();
    for (const bad of ['a', 'a@', '@b.co', 'a@b', 'a b@c.com', 'a@@b.com', 'a@.com', 'a@b.', `${'x'.repeat(250)}@b.co`]) {
      expect(v(bad), bad).toEqual({ email: true });
    }
  });

  it('passwordLengthValidator conta pontos de código e respeita limites', () => {
    const v = passwordLengthValidator(12, 128);
    expect(v(new FormControl('curta'))).toEqual({ tooShort: { min: 12 } });
    expect(v(new FormControl('a'.repeat(12)))).toBeNull();
    expect(v(new FormControl('😀'.repeat(12)))).toBeNull();
    expect(v(new FormControl('a'.repeat(129)))).toEqual({ tooLong: { max: 128 } });
    expect(v(new FormControl(''))).toBeNull();
  });

  it('matchValidator', () => {
    const g = new FormGroup({ a: new FormControl('x'), b: new FormControl('y') }, { validators: matchValidator('a', 'b') });
    expect(g.errors).toEqual({ mismatch: true });
    g.patchValue({ b: 'x' });
    expect(g.errors).toBeNull();
  });

  it('isValidOrigin', () => {
    expect(isValidOrigin('https://app.exemplo.com')).toBe(true);
    expect(isValidOrigin('http://localhost:4200')).toBe(true);
    for (const bad of ['http://app.exemplo.com', 'https://app.exemplo.com/', 'https://app.exemplo.com/x', 'https://*.exemplo.com', '', 'app.exemplo.com']) {
      expect(isValidOrigin(bad), bad).toBe(false);
    }
  });

  it('isValidAbsoluteUrl', () => {
    expect(isValidAbsoluteUrl('https://app.exemplo.com/auth/callback')).toBe(true);
    expect(isValidAbsoluteUrl('http://localhost:4200/x')).toBe(true);
    for (const bad of ['http://app.exemplo.com/x', 'https://app.exemplo.com/x#frag', 'https://u:p@app.exemplo.com/x', 'https://*.x.com/', '/relativo']) {
      expect(isValidAbsoluteUrl(bad), bad).toBe(false);
    }
  });

  it('isValidAudience', () => {
    expect(isValidAudience('https://api.exemplo.com')).toBe(true);
    expect(isValidAudience('api:orders_v1')).toBe(true);
    expect(isValidAudience('com espaço')).toBe(false);
    expect(isValidAudience('')).toBe(false);
    expect(isValidAudience('a'.repeat(201))).toBe(false);
  });

  it('passwordStrength é apenas uma dica monotônica', () => {
    expect(passwordStrength('')).toBe(0);
    expect(passwordStrength('abc')).toBe(1);
    expect(passwordStrength('abcdefghijkl')).toBe(2);
    expect(passwordStrength('uma frase bem longa e boa')).toBe(3);
  });
});
