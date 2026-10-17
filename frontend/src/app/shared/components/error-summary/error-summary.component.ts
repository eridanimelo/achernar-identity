import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

export interface SummaryItem {
  /** id do campo (atributo `id`/`name` do input) */
  field: string;
  message: string;
}

/** Resumo de erros no topo do formulário; links levam ao campo e o foco vai ao resumo após o envio. */
@Component({
  selector: 'app-error-summary',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  template: `
    @if (items().length > 0) {
      <div class="summary" role="alert" tabindex="-1" #box>
        <p class="title">{{ 'common.fixErrors' | transloco }}</p>
        <ul>
          @for (i of items(); track i.field) {
            <li><a [href]="'#' + i.field" (click)="go($event, i.field)">{{ i.message }}</a></li>
          }
        </ul>
      </div>
    }
  `,
  styles: `
    .summary {
      border: 1px solid var(--ach-error);
      border-radius: var(--ach-radius-field);
      padding: 12px 16px;
      margin-bottom: 16px;
      background: color-mix(in srgb, var(--ach-error) 8%, var(--ach-surface));
    }
    .title {
      margin: 0 0 4px;
      font-weight: 600;
    }
    ul {
      margin: 0;
      padding-left: 20px;
    }
    a {
      color: var(--ach-error);
    }
  `,
})
export class ErrorSummaryComponent {
  readonly items = input.required<SummaryItem[]>();
  /** Muda a cada submissão para devolver o foco ao resumo. */
  readonly focusToken = input<number>(0);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    effect(() => {
      this.focusToken();
      if (this.items().length > 0 && this.focusToken() > 0) {
        queueMicrotask(() => this.host.nativeElement.querySelector<HTMLElement>('.summary')?.focus());
      }
    });
  }

  go(ev: Event, field: string): void {
    ev.preventDefault();
    document.getElementById(field)?.focus();
  }
}
