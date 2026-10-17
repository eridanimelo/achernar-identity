import { DOCUMENT } from '@angular/common';
import { Injectable, effect, inject, signal } from '@angular/core';
import { safeGet, safeSet } from '../utils/safe-storage';

export type ThemeMode = 'system' | 'light' | 'dark';
const KEY = 'achernar.theme';

/** Tema Sistema/Claro/Escuro (SCR-30): atributo data-theme no <html>; persistido em localStorage. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);
  readonly mode = signal<ThemeMode>(read());

  constructor() {
    effect(() => this.apply(this.mode()));
  }

  set(mode: ThemeMode): void {
    this.mode.set(mode);
    if (mode === 'system') {
      safeSet('local', KEY, 'system');
    } else {
      safeSet('local', KEY, mode);
    }
  }

  private apply(mode: ThemeMode): void {
    const el = this.doc.documentElement;
    if (mode === 'system') {
      el.removeAttribute('data-theme');
    } else {
      el.setAttribute('data-theme', mode);
    }
  }
}

function read(): ThemeMode {
  const v = safeGet('local', KEY);
  return v === 'light' || v === 'dark' ? v : 'system';
}
