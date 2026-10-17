import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Marca provisória: estrela de quatro pontas + "achernar·identity" (visual-proposal §1). */
@Component({
  selector: 'app-brand',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="brand">
      <svg viewBox="0 0 24 24" width="28" height="28" aria-hidden="true" focusable="false">
        <path d="M12 1.5l2.2 7.3 7.3 2.2-7.3 2.2L12 20.5l-2.2-7.3-7.3-2.2 7.3-2.2z" fill="currentColor" />
      </svg>
      <span class="text">achernar<span class="dot">·</span>identity</span>
    </span>
  `,
  styles: `
    .brand {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-family: var(--ach-font-title);
      font-weight: 600;
      font-size: 18px;
      color: inherit;
    }
    svg {
      color: var(--ach-accent);
    }
    .dot {
      opacity: 0.7;
    }
  `,
})
export class BrandComponent {
  readonly compact = input(false);
}
