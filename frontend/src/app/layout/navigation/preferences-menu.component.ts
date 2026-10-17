import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslocoPipe } from '@jsverse/transloco';
import { Lang, LocaleService } from '../../core/services/locale.service';
import { ThemeMode, ThemeService } from '../../core/services/theme.service';

/** SCR-30: tema (Sistema/Claro/Escuro) e idioma (pt-BR/en). */
@Component({
  selector: 'app-preferences-menu',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, MatMenuModule, TranslocoPipe],
  template: `
    <button mat-icon-button [matMenuTriggerFor]="theme" [attr.aria-label]="'prefs.theme' | transloco">
      <mat-icon aria-hidden="true">{{ themeIcon() }}</mat-icon>
    </button>
    <mat-menu #theme="matMenu">
      @for (m of modes; track m) {
        <button mat-menu-item role="menuitemradio" [attr.aria-checked]="themes.mode() === m" (click)="themes.set(m)">
          <mat-icon aria-hidden="true">{{ themes.mode() === m ? 'radio_button_checked' : 'radio_button_unchecked' }}</mat-icon>
          <span>{{ 'prefs.theme.' + m | transloco }}</span>
        </button>
      }
    </mat-menu>
    <button mat-button [matMenuTriggerFor]="lang" [attr.aria-label]="'prefs.language' | transloco">
      <mat-icon aria-hidden="true">translate</mat-icon>
      {{ locale.lang() === 'pt-BR' ? 'PT' : 'EN' }}
    </button>
    <mat-menu #lang="matMenu">
      @for (l of langs; track l.code) {
        <button mat-menu-item role="menuitemradio" [attr.aria-checked]="locale.lang() === l.code" (click)="locale.set(l.code)">
          <mat-icon aria-hidden="true">{{ locale.lang() === l.code ? 'radio_button_checked' : 'radio_button_unchecked' }}</mat-icon>
          <span>{{ l.label }}</span>
        </button>
      }
    </mat-menu>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  `,
})
export class PreferencesMenuComponent {
  readonly themes = inject(ThemeService);
  readonly locale = inject(LocaleService);
  readonly modes: ThemeMode[] = ['system', 'light', 'dark'];
  readonly langs: { code: Lang; label: string }[] = [
    { code: 'pt-BR', label: 'Português (Brasil)' },
    { code: 'en', label: 'English' },
  ];

  themeIcon(): string {
    const m = this.themes.mode();
    return m === 'dark' ? 'dark_mode' : m === 'light' ? 'light_mode' : 'brightness_auto';
  }
}
