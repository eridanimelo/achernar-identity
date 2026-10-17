import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';
import { SessionService } from '../../../core/auth/session.service';
import { toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';

/** SCR-09: visão somente leitura da própria conta. */
@Component({
  selector: 'app-account-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatCardModule, MatIconModule, TranslocoPipe, LocalDatePipe, FormAlertComponent, PageHeaderComponent, SkeletonComponent, StatusChipComponent],
  templateUrl: './account.page.html',
  styleUrl: './account.page.scss',
})
export class AccountPage implements OnInit {
  readonly session = inject(SessionService);
  private readonly errors = inject(ErrorMessageService);
  readonly loading = signal(false);
  readonly failure = signal<string | null>(null);
  readonly trace = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.session.account()) {
      void this.reload();
    }
  }

  async reload(): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      await this.session.loadAccount();
    } catch (e) {
      const err = toApiError(e);
      this.failure.set(this.errors.message(err));
      this.trace.set(err.traceId ?? null);
    } finally {
      this.loading.set(false);
    }
  }

  statusTone(status: string): 'success' | 'warning' | 'error' {
    return status === 'ACTIVE' ? 'success' : status === 'DISABLED' ? 'error' : 'warning';
  }
}
