import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { NotifyService } from '../../../core/services/notify.service';
import { ConfirmDialogComponent, ConfirmResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { SessionItem } from '../models/account.models';
import { AccountService } from '../services/account.service';

/** SCR-14: minhas sessões (listar, encerrar uma, encerrar as outras, encerrar todas). */
@Component({
  selector: 'app-sessions-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, MatTableModule, TranslocoPipe, LocalDatePipe, PageHeaderComponent, SkeletonComponent, EmptyStateComponent, FormAlertComponent, StatusChipComponent],
  templateUrl: './sessions.page.html',
  styleUrl: './sessions.page.scss',
})
export class SessionsPage implements OnInit {
  private readonly api = inject(AccountService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  readonly columns = ['app', 'created', 'lastUsed', 'ip', 'agent', 'actions'];
  readonly items = signal<SessionItem[] | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly message = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });
  readonly others = computed(() => (this.items() ?? []).filter((s) => !s.current));

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      this.items.set(await firstValueFrom(this.api.sessions()));
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  async revoke(s: SessionItem): Promise<void> {
    if (!(await this.confirm('sessions.revokeTitle', 'sessions.revokeMessage', 'sessions.revoke'))) {
      return;
    }
    await this.run(async () => {
      await firstValueFrom(this.api.revokeSession(s.id));
      if (s.current) {
        await this.leave();
        return;
      }
      this.notify.success('sessions.revoked');
      await this.load();
    });
  }

  async endOthers(): Promise<void> {
    if (!(await this.confirm('sessions.othersTitle', 'sessions.othersMessage', 'sessions.endOthers'))) {
      return;
    }
    await this.run(async () => {
      await firstValueFrom(this.api.endAll(true));
      this.notify.success('sessions.revokedOthers');
      await this.load();
    });
  }

  async endAll(): Promise<void> {
    if (!(await this.confirm('sessions.allTitle', 'sessions.allMessage', 'sessions.endAll'))) {
      return;
    }
    await this.run(async () => {
      await firstValueFrom(this.api.endAll(false));
      await this.leave();
    });
  }

  private async leave(): Promise<void> {
    this.session.clearLocal();
    await this.router.navigateByUrl('/login');
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.failure.set(null);
    try {
      await action();
    } catch (e) {
      const err = toApiError(e);
      if (err.code !== 'REAUTH_REQUIRED') {
        this.failure.set(err);
      }
    } finally {
      this.busy.set(false);
    }
  }

  private async confirm(title: string, message: string, label: string): Promise<boolean> {
    const ref = this.dialog.open<ConfirmDialogComponent, unknown, ConfirmResult>(ConfirmDialogComponent, {
      width: '440px',
      ariaLabelledBy: 'confirm-title',
      data: {
        title: this.i18n.translate(title),
        message: this.i18n.translate(message),
        confirmLabel: this.i18n.translate(label),
        destructive: true,
      },
    });
    return (await firstValueFrom(ref.afterClosed()))?.confirmed === true;
  }
}
