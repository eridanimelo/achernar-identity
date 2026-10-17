import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { safeGet, safeSet } from '../utils/safe-storage';

export type Lang = 'pt-BR' | 'en';
export const SUPPORTED_LANGS: readonly Lang[] = ['pt-BR', 'en'];
const KEY = 'achernar.locale';

export function detectLang(stored: string | null, navigatorLang: string | undefined): Lang {
  if (stored === 'pt-BR' || stored === 'en') {
    return stored;
  }
  return (navigatorLang ?? 'pt-BR').toLowerCase().startsWith('pt') ? 'pt-BR' : 'en';
}

/** Idioma da interface (pt-BR/en): `lang` dinâmico no <html> e Accept-Language nas chamadas. */
@Injectable({ providedIn: 'root' })
export class LocaleService {
  private readonly doc = inject(DOCUMENT);
  private readonly transloco = inject(TranslocoService);
  readonly lang = signal<Lang>(detectLang(safeGet('local', KEY), typeof navigator === 'undefined' ? undefined : navigator.language));

  init(): void {
    this.apply(this.lang());
  }

  set(lang: Lang): void {
    this.lang.set(lang);
    safeSet('local', KEY, lang);
    this.apply(lang);
  }

  private apply(lang: Lang): void {
    this.transloco.setActiveLang(lang);
    this.doc.documentElement.setAttribute('lang', lang);
  }
}
