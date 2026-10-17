import { Injectable, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { ApiError } from '../http/api-error';

/** Mensagens de erro localizadas por código (`error.<CODE>`), sem revelar existência de contas (D-13). */
@Injectable({ providedIn: 'root' })
export class ErrorMessageService {
  private readonly i18n = inject(TranslocoService);

  message(err: ApiError): string {
    const key = `error.${err.code}`;
    const params = { seconds: err.retryAfterSeconds ?? 0 };
    const text = this.i18n.translate(key, params);
    return text === key ? this.i18n.translate('error.UNKNOWN', params) : text;
  }

  field(code: string): string {
    const key = `field.${code}`;
    const text = this.i18n.translate(key);
    return text === key ? this.i18n.translate('field.INVALID') : text;
  }
}
