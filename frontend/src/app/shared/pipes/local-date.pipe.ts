import { Pipe, PipeTransform, inject } from '@angular/core';
import { LocaleService } from '../../core/services/locale.service';

/** Data/hora no idioma ativo (Intl). Impuro para reagir à troca de idioma. */
@Pipe({ name: 'localDate', pure: false })
export class LocalDatePipe implements PipeTransform {
  private readonly locale = inject(LocaleService);

  transform(value: string | Date | null | undefined, style: 'datetime' | 'date' = 'datetime'): string {
    if (!value) {
      return '—';
    }
    const d = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(d.getTime())) {
      return '—';
    }
    return new Intl.DateTimeFormat(this.locale.lang(), {
      dateStyle: 'medium',
      ...(style === 'datetime' ? { timeStyle: 'short' } : {}),
    }).format(d);
  }
}
