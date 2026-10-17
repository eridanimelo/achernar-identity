import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { NotifyService } from '../../../core/services/notify.service';
import { ConfirmDialogComponent, ConfirmData, ConfirmResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { accountStatusTone, outcomeTone, severityTone } from '../components/admin-tones';
import { AdminAccountDetail } from '../models/admin.models';
import { AdminAccountsService } from '../services/admin-accounts.service';

/** SCR-19/20: detalhe da conta e ações administrativas (desativar, reativar, perfil, anonimizar). */
@Component({
  selector: 'app-account-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, TranslocoPipe, LocalDatePipe, PageHeaderComponent, SkeletonComponent, FormAlertComponent, StatusChipComponent],
  templateUrl: './account-detail.page.html',
  styleUrl: './account-detail.page.scss',
})
export class AccountDetailPage implements OnInit {
  private readonly api = inject(AdminAccountsService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);

  /** Parâmetro de rota `:id` (component input binding). */
  readonly id = input.required<string>();
  readonly account = signal<AdminAccountDetail | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly message = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });
  readonly tone = accountStatusTone;
  readonly sevTone = severityTone;
  readonly outTone = outcomeTone;
  readonly anonymized = computed(() => !!this.account()?.anonymized_at);

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      this.account.set(await firstValueFrom(this.api.get(this.id())));
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  async disable(): Promise<void> {
    const a = this.account();
    const r = a && (await this.confirm({
      title: this.i18n.translate('admin.account.disableTitle'),
      message: this.i18n.translate('admin.account.disableMessage', { email: a.email }),
      confirmLabel: this.i18n.translate('admin.account.disable'),
      destructive: true,
      reason: true,
    }));
    if (a && r?.confirmed) {
      await this.run(() => this.api.disable(a.id, r.reason), 'admin.account.disabled');
    }
  }

  async enable(): Promise<void> {
    const a = this.account();
    const r = a && (await this.confirm({
      title: this.i18n.translate('admin.account.enableTitle'),
      message: this.i18n.translate('admin.account.enableMessage', { email: a.email }),
      confirmLabel: this.i18n.translate('admin.account.enable'),
    }));
    if (a && r?.confirmed) {
      await this.run(() => this.api.enable(a.id), 'admin.account.enabled');
    }
  }

  async toggleRole(): Promise<void> {
    const a = this.account();
    if (!a) {
      return;
    }
    const target = a.role === 'ADMIN' ? 'USER' : 'ADMIN';
    const r = await this.confirm({
      title: this.i18n.translate('admin.account.roleTitle'),
      message: this.i18n.translate(target === 'ADMIN' ? 'admin.account.grantMessage' : 'admin.account.revokeMessage', { email: a.email }),
      confirmLabel: this.i18n.translate(target === 'ADMIN' ? 'admin.account.grant' : 'admin.account.revokeAdmin'),
      destructive: target === 'USER',
    });
    if (r.confirmed) {
      await this.run(() => this.api.changeRole(a.id, target, a.version), 'admin.account.roleChanged');
    }
  }

  async anonymize(): Promise<void> {
    const a = this.account();
    if (!a) {
      return;
    }
    const r = await this.confirm({
      title: this.i18n.translate('admin.account.anonymizeTitle'),
      message: this.i18n.translate('admin.account.anonymizeMessage'),
      confirmLabel: this.i18n.translate('admin.account.anonymize'),
      destructive: true,
      typeToConfirm: a.email,
      typeLabel: this.i18n.translate('admin.account.typeEmail'),
    });
    if (r.confirmed) {
      await this.run(() => this.api.anonymize(a.id, a.email), 'admin.account.anonymized');
    }
  }

  private async confirm(data: ConfirmData): Promise<ConfirmResult> {
    const ref = this.dialog.open<ConfirmDialogComponent, ConfirmData, ConfirmResult>(ConfirmDialogComponent, {
      width: '480px',
      ariaLabelledBy: 'confirm-title',
      data,
    });
    return (await firstValueFrom(ref.afterClosed())) ?? { confirmed: false };
  }

  private async run(action: () => Observable<unknown>, successKey: string): Promise<void> {
    this.busy.set(true);
    this.failure.set(null);
    try {
      await firstValueFrom(action());
      this.notify.success(successKey);
      await this.load();
    } catch (e) {
      const err = toApiError(e);
      if (err.code !== 'REAUTH_REQUIRED') {
        this.failure.set(err);
      }
    } finally {
      this.busy.set(false);
    }
  }
}
