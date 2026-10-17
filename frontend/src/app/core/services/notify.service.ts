import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslocoService } from '@jsverse/transloco';
import { ApiError } from '../http/api-error';
import { ErrorMessageService } from './error-message.service';

/** Notificações curtas (4 s, role=status — ui-map §3). */
@Injectable({ providedIn: 'root' })
export class NotifyService {
  private readonly snack = inject(MatSnackBar);
  private readonly i18n = inject(TranslocoService);
  private readonly errors = inject(ErrorMessageService);

  success(key: string, params?: Record<string, unknown>): void {
    this.open(this.i18n.translate(key, params));
  }

  error(err: ApiError): void {
    this.open(this.errors.message(err), 6000);
  }

  /** Falha identificada por chave i18n (ex.: retorno de erro do provedor social). */
  failure(key: string): void {
    this.open(this.i18n.translate(key), 6000);
  }

  info(message: string): void {
    this.open(message);
  }

  private open(message: string, duration = 4000): void {
    this.snack.open(message, this.i18n.translate('common.close'), {
      duration,
      politeness: 'polite',
      panelClass: 'ach-snack',
    });
  }
}
