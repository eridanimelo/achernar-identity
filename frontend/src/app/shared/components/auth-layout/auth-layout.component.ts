import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { BrandComponent } from '../brand/brand.component';

/** Layout dividido das páginas de autenticação: painel de marca (≥ 840 px) + cartão do formulário. */
@Component({
  selector: 'app-auth-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BrandComponent, TranslocoPipe],
  templateUrl: './auth-layout.component.html',
  styleUrl: './auth-layout.component.scss',
})
export class AuthLayoutComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
}
