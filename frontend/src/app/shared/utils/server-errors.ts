import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiError } from '../../core/http/api-error';

/**
 * Aplica `errors[]` do servidor aos controles do formulário (erro `server` com o código do campo).
 * Devolve os erros que não corresponderam a nenhum controle (para exibir no alerta do formulário).
 */
export function applyServerErrors(form: FormGroup, err: ApiError, aliases: Record<string, string> = {}): string[] {
  const unmatched: string[] = [];
  for (const issue of err.errors) {
    const name = aliases[issue.field] ?? issue.field;
    const control: AbstractControl | null = form.get(name);
    if (control) {
      control.setErrors({ server: issue.code });
      control.markAsTouched();
    } else {
      unmatched.push(issue.code);
    }
  }
  return unmatched;
}
