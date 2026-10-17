import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { SecretRow } from '../models/admin.models';

/** Lista de segredos/credenciais (aplicação CONFIDENTIAL ou API de introspecção). Apresentacional. */
@Component({
  selector: 'app-secrets-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, TranslocoPipe, LocalDatePipe, StatusChipComponent],
  template: `
    <section class="secrets" [attr.aria-label]="title()">
      <div class="head">
        <h2>{{ title() }}</h2>
        <button mat-stroked-button type="button" [disabled]="busy()" (click)="generate.emit()">{{ 'secrets.generate' | transloco }}</button>
      </div>
      @if (rows().length === 0) {
        <p class="muted">{{ 'secrets.none' | transloco }}</p>
      }
      <ul>
        @for (r of rows(); track r.id) {
          <li>
            <code>{{ r.id.slice(0, 8) }}…</code>
            <span class="muted">{{ 'secrets.created' | transloco }}: {{ r.created_at | localDate }}</span>
            <span class="muted">{{ 'secrets.lastUsed' | transloco }}: {{ r.last_used_at | localDate }}</span>
            @if (r.revoked_at) {
              <app-status-chip tone="neutral" [label]="'secrets.revoked' | transloco" />
            } @else {
              <app-status-chip tone="success" [label]="'secrets.active' | transloco" />
              <button mat-button type="button" [disabled]="busy()" (click)="revoke.emit(r.id)">{{ 'secrets.revoke' | transloco }}</button>
            }
          </li>
        }
      </ul>
    </section>
  `,
  styles: `
    .head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
    ul {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 8px;
    }
    li {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 16px;
      align-items: center;
    }
  `,
})
export class SecretsSectionComponent {
  readonly title = input.required<string>();
  readonly rows = input<SecretRow[]>([]);
  readonly busy = input(false);
  readonly generate = output<void>();
  readonly revoke = output<string>();
}
