import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { Provider } from '../../../core/auth/auth.models';

/** SCR-15: botões Google/Facebook conforme a configuração pública; desabilitados durante a navegação. */
@Component({
  selector: 'app-social-buttons',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, TranslocoPipe],
  template: `
    @if (providers().length > 0) {
      <div class="divider" role="separator"><span>{{ 'auth.or' | transloco }}</span></div>
      <div class="buttons">
        @for (p of providers(); track p) {
          <button mat-stroked-button type="button" [disabled]="navigating() || disabled()" (click)="choose(p)">
            {{ 'auth.continueWith' | transloco: { provider: ('social.' + p) | transloco } }}
          </button>
        }
      </div>
    }
  `,
  styles: `
    .divider {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 16px 0;
      color: var(--ach-text-2);
    }
    .divider::before,
    .divider::after {
      content: '';
      flex: 1;
      border-top: 1px solid color-mix(in srgb, var(--ach-outline) 50%, transparent);
    }
    .buttons {
      display: grid;
      gap: 12px;
    }
  `,
})
export class SocialButtonsComponent {
  readonly providers = input<Provider[]>([]);
  readonly disabled = input(false);
  readonly selected = output<Provider>();
  readonly navigating = signal(false);

  choose(p: Provider): void {
    this.navigating.set(true);
    this.selected.emit(p);
  }
}
