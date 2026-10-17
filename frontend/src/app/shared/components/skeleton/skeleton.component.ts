import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Esqueleto de carregamento (lista/detalhe). Pulso suave desligado por prefers-reduced-motion. */
@Component({
  selector: 'app-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="sk" role="status" aria-live="polite" aria-busy="true">
      <span class="sr-only">{{ label() }}</span>
      @for (r of rows(); track r) {
        <div class="line" [style.width.%]="100 - (r % 3) * 12"></div>
      }
    </div>
  `,
  styles: `
    .sk {
      display: grid;
      gap: 12px;
      padding: 16px 0;
    }
    .line {
      height: 16px;
      border-radius: 8px;
      background: var(--ach-surface-2);
      animation: pulse 1.4s ease-in-out infinite;
    }
    @keyframes pulse {
      50% {
        opacity: 0.5;
      }
    }
  `,
})
export class SkeletonComponent {
  readonly label = input('Carregando…');
  readonly lines = input(4);
  rows(): number[] {
    return Array.from({ length: this.lines() }, (_, i) => i);
  }
}
