import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Estado vazio: ícone + título + texto + ação primária (ui-map §3). */
@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  template: `
    <div class="empty">
      <mat-icon aria-hidden="true">{{ icon() }}</mat-icon>
      @if (level() === 1) {
        <h1 id="page-title" tabindex="-1">{{ title() }}</h1>
      } @else {
        <h2>{{ title() }}</h2>
      }
      @if (text()) {
        <p class="muted">{{ text() }}</p>
      }
      <div class="actions"><ng-content /></div>
    </div>
  `,
  styles: `
    .empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 40px 16px;
      gap: 4px;
    }
    mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: var(--ach-text-2);
    }
  `,
})
export class EmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly text = input<string | null>(null);
  /** 1 quando o estado vazio é o conteúdo principal da rota (então vira o h1). */
  readonly level = input<1 | 2>(2);
}
