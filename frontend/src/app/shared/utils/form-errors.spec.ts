import { FormControl, Validators } from '@angular/forms';
import { firstError } from './form-errors';

describe('firstError', () => {
  it('retorna null sem erros', () => {
    expect(firstError(new FormControl('ok'))).toBeNull();
    expect(firstError(null)).toBeNull();
  });

  it('prioriza required e mapeia validators nativos', () => {
    expect(firstError(new FormControl('', Validators.required))?.key).toBe('validation.required');
    const c = new FormControl('abc', Validators.minLength(5));
    expect(firstError(c)).toEqual({ key: 'validation.minlength', params: { min: 5 } });
  });

  it('mapeia erro vindo do servidor para field.<CODE>', () => {
    const c = new FormControl('x');
    c.setErrors({ server: 'COMMON' });
    expect(firstError(c)?.key).toBe('field.COMMON');
  });
});
