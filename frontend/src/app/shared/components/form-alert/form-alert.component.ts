import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';

/** Alerta de operação (role=alert) com `trace_id` copiável e ação de nova tentativa (ui-map §3). */
@Component({
  selector: 'app-form-alert',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, TranslocoPipe],
  template: `
    <div class="alert" role="alert" [class.info]="tone() === 'info'">
      <mat-icon aria-hidden="true">{{ tone() === 'info' ? 'info' : 'error' }}</mat-icon>
      <div class="body">
        <p class="msg">{{ message() }}</p>
        @if (traceId()) {
          <p class="trace">
            <span class="muted">{{ 'common.traceId' | transloco }}:</span>
            <code>{{ traceId() }}</code>
            <button mat-button type="button" (click)="copy()">{{ (copied() ? 'common.copied' : 'common.copy') | transloco }}</button>
          </p>
        }
      </div>
    </div>
  `,
  styleUrl: './form-alert.component.scss',
})
export class FormAlertComponent {
  readonly message = input.required<string>();
  readonly traceId = input<string | null | undefined>(null);
  readonly tone = input<'error' | 'info'>('error');
  readonly copied = signal(false);

  async copy(): Promise<void> {
    const id = this.traceId();
    if (!id) {
      return;
    }
    try {
      await navigator.clipboard.writeText(id);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      // área de transferência indisponível
    }
  }
}
