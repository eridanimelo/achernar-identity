import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Cabeçalho de página: um único h1 por rota (recebe o foco a cada navegação) + ações. */
@Component({
  selector: 'app-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="head">
      <div>
        <h1 id="page-title" tabindex="-1">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="muted sub">{{ subtitle() }}</p>
        }
      </div>
      <div class="actions"><ng-content /></div>
    </header>
  `,
  styles: `
    .head {
      display: flex;
      flex-wrap: wrap;
      gap: 12px 24px;
      align-items: flex-start;
      justify-content: space-between;
      margin-bottom: 16px;
    }
    h1 {
      font-size: 24px;
      margin: 0;
    }
    .sub {
      margin: 4px 0 0;
    }
  `,
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
}
