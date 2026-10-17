import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { NotifyService } from '../../../core/services/notify.service';
import { ConfirmDialogComponent, ConfirmData, ConfirmResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { ChipTone, StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { SigningKey } from '../models/admin.models';
import { AdminKeysService } from '../services/admin-keys.service';

const MAX_ACTIVE_DAYS = 100; // alerta se a chave ativa passa do dobro do intervalo padrão de rotação (P-10)

/** SCR-29: chaves de assinatura — estados, rotação manual e remoção emergencial. */
@Component({
  selector: 'app-keys-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatTableModule, TranslocoPipe, LocalDatePipe, PageHeaderComponent, SkeletonComponent, EmptyStateComponent, FormAlertComponent, StatusChipComponent],
  templateUrl: './keys.page.html',
})
export class KeysPage implements OnInit {
  private readonly api = inject(AdminKeysService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);

  readonly columns = ['kid', 'status', 'publish', 'activate', 'retire', 'actions'];
  readonly keys = signal<SigningKey[] | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly message = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });
  readonly activeTooOld = computed(() => {
    const active = (this.keys() ?? []).find((k) => k.status === 'ACTIVE');
    if (!active?.activate_at) {
      return false;
    }
    return (Date.now() - new Date(active.activate_at).getTime()) / 86_400_000 > MAX_ACTIVE_DAYS;
  });

  ngOnInit(): void {
    void this.load();
  }

  tone(status: string): ChipTone {
    return status === 'ACTIVE' ? 'success' : status === 'PUBLISHED' ? 'info' : status === 'RETIRED' ? 'warning' : 'neutral';
  }

  canRemove(k: SigningKey): boolean {
    return k.status !== 'REMOVED';
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      this.keys.set(await firstValueFrom(this.api.list()));
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  async rotate(): Promise<void> {
    const r = await this.confirm({
      title: this.i18n.translate('admin.keys.rotateTitle'),
      message: this.i18n.translate('admin.keys.rotateMessage'),
      confirmLabel: this.i18n.translate('admin.keys.rotate'),
    });
    if (r.confirmed) {
      await this.run(async () => {
        await firstValueFrom(this.api.rotate());
        this.notify.success('admin.keys.rotated');
        await this.load();
      });
    }
  }

  async remove(k: SigningKey): Promise<void> {
    const emergency = k.status === 'ACTIVE' || k.status === 'PUBLISHED';
    const r = await this.confirm({
      title: this.i18n.translate('admin.keys.removeTitle'),
      message: this.i18n.translate(emergency ? 'admin.keys.removeEmergency' : 'admin.keys.removeMessage'),
      confirmLabel: this.i18n.translate('admin.keys.remove'),
      destructive: true,
      typeToConfirm: k.kid,
      typeLabel: this.i18n.translate('admin.keys.typeKid'),
      reason: true,
      option: emergency ? { label: this.i18n.translate('admin.keys.revokeSessions') } : undefined,
    });
    if (r.confirmed) {
      await this.run(async () => {
        await firstValueFrom(this.api.remove(k.kid, { emergency, revoke_all_sessions: emergency && !!r.option, reason: r.reason }));
        this.notify.success('admin.keys.removed');
        await this.load();
      });
    }
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

  private async confirm(data: ConfirmData): Promise<ConfirmResult> {
    const ref = this.dialog.open<ConfirmDialogComponent, ConfirmData, ConfirmResult>(ConfirmDialogComponent, {
      width: '520px',
      ariaLabelledBy: 'confirm-title',
      data,
    });
    return (await firstValueFrom(ref.afterClosed())) ?? { confirmed: false };
  }
}
