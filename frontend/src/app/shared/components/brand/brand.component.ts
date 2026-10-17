import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Marca oficial adaptada da referência fornecida pelo usuário. */
@Component({
  selector: 'app-brand',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="brand" [class.compact]="compact()">
      <img src="/brand/logo.png" alt="Achernar Identity" width="1983" height="793" />
    </span>
  `,
  styles: `
    :host { display: inline-flex; max-width: 100%; }
    .brand {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 230px;
      height: 72px;
      overflow: hidden;
      max-width: 100%;
      padding: 2px 4px;
      border-radius: 12px;
      background: var(--ach-logo-surface);
    }
    img { display: block; width: 100%; height: auto; }
    .compact { width: 210px; height: 68px; }
    @media (max-width: 599px) {
      .brand { width: 168px; height: 56px; padding: 6px 8px; }
    }
  `,
})
export class BrandComponent {
  readonly compact = input(false);
}
