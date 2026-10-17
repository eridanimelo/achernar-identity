import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { NotifyService } from '../../../core/services/notify.service';
import { ConfirmDialogComponent, ConfirmData, ConfirmResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SecretRevealDialogComponent } from '../../../shared/components/secret-reveal-dialog/secret-reveal-dialog.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { applyServerErrors } from '../../../shared/utils/server-errors';
import { ApplicationFieldsComponent } from '../components/application-fields.component';
import { addAudience, buildApplicationForm, patchApplicationForm, toPayload } from '../components/application-form';
import { SecretsSectionComponent } from '../components/secrets-section.component';
import { ApiSummary, ApplicationDetail } from '../models/admin.models';
import { AdminApisService } from '../services/admin-apis.service';
import { AdminApplicationsService } from '../services/admin-applications.service';

/** SCR-25/26: criar e editar aplicação, com segredos para CONFIDENTIAL e exibição única. */
@Component({
  selector: 'app-application-form-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, FocusInvalidDirective,
    PageHeaderComponent, FormAlertComponent, SkeletonComponent, ApplicationFieldsComponent, SecretsSectionComponent,
  ],
  templateUrl: './application-form.page.html',
})
export class ApplicationFormPage implements OnInit {
  private readonly api = inject(AdminApplicationsService);
  private readonly apisApi = inject(AdminApisService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);

  /** Parâmetro de rota `:id` ausente em /new. */
  readonly id = input<string | undefined>();
  readonly isNew = computed(() => !this.id());
  readonly form = buildApplicationForm(this.fb);
  readonly app = signal<ApplicationDetail | null>(null);
  readonly apis = signal<ApiSummary[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly message = computed(() => {
    const f = this.failure();
    if (!f) {
      return '';
    }
    return f.status === 409 && f.code === 'CONFLICT' ? this.i18n.translate('admin.apps.conflict') : this.errors.message(f);
  });
  readonly isConfidential = computed(() => this.app()?.type === 'CONFIDENTIAL');
  readonly title = computed(() => (this.isNew() ? this.i18n.translate('admin.apps.new') : (this.app()?.name ?? '')));

  async ngOnInit(): Promise<void> {
    try {
      this.apis.set((await firstValueFrom(this.apisApi.list('', 0, 100))).items);
    } catch {
      this.apis.set([]);
    }
    if (this.isNew()) {
      this.loading.set(false);
      return;
    }
    await this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      const a = await firstValueFrom(this.api.get(this.id() as string));
      this.app.set(a);
      patchApplicationForm(this.fb, this.form, a);
      this.form.controls.client_id.disable();
      this.form.controls.type.disable();
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  addApi(audience: string): void {
    addAudience(this.fb, this.form, audience, this.form.controls.audiences.length === 0);
    this.form.markAsDirty();
  }

  removeApi(index: number): void {
    this.form.controls.audiences.removeAt(index);
    this.form.markAsDirty();
  }

  async save(): Promise<void> {
    this.failure.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy()) {
      return;
    }
    if (!this.isNew() && this.form.controls.status.value === 'DISABLED' && this.app()?.status === 'ACTIVE') {
      const r = await this.confirm({
        title: this.i18n.translate('admin.apps.disableTitle'),
        message: this.i18n.translate('admin.apps.disableMessage'),
        confirmLabel: this.i18n.translate('admin.apps.disable'),
        destructive: true,
      });
      if (!r.confirmed) {
        return;
      }
    }
    this.busy.set(true);
    try {
      if (this.isNew()) {
        const created = await firstValueFrom(this.api.create(toPayload(this.form, true)));
        if (created.client_secret) {
          await this.reveal('secrets.appSecretTitle', created.client_secret);
        }
        this.notify.success('admin.apps.created');
        await this.router.navigate(['/admin/applications', created.id]);
      } else {
        const body = { ...toPayload(this.form, false), version: this.app()?.version ?? 0 };
        const updated = await firstValueFrom(this.api.update(this.id() as string, body));
        this.app.set(updated);
        patchApplicationForm(this.fb, this.form, updated);
        this.form.controls.client_id.disable();
        this.form.controls.type.disable();
        this.form.markAsPristine();
        this.notify.success('admin.apps.saved');
      }
    } catch (e) {
      const err = toApiError(e);
      const unmatched = applyServerErrors(this.form, err);
      if (err.errors.length === 0 || unmatched.length > 0 || err.status === 409) {
        this.failure.set(err);
      }
    } finally {
      this.busy.set(false);
    }
  }

  async generateSecret(): Promise<void> {
    const a = this.app();
    if (!a) {
      return;
    }
    await this.mutate(async () => {
      const s = await firstValueFrom(this.api.createSecret(a.id));
      await this.reveal('secrets.appSecretTitle', s.client_secret);
      await this.load();
    });
  }

  async revokeSecret(secretId: string): Promise<void> {
    const a = this.app();
    if (!a) {
      return;
    }
    const r = await this.confirm({
      title: this.i18n.translate('secrets.revokeTitle'),
      message: this.i18n.translate('secrets.revokeMessage'),
      confirmLabel: this.i18n.translate('secrets.revoke'),
      destructive: true,
    });
    if (r.confirmed) {
      await this.mutate(async () => {
        await firstValueFrom(this.api.revokeSecret(a.id, secretId));
        this.notify.success('secrets.revoked');
        await this.load();
      });
    }
  }

  private async mutate(action: () => Promise<void>): Promise<void> {
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

  private async reveal(titleKey: string, secret: string): Promise<void> {
    const ref = this.dialog.open(SecretRevealDialogComponent, {
      width: '560px',
      disableClose: true,
      ariaLabelledBy: 'secret-title',
      data: { title: this.i18n.translate(titleKey), secret },
    });
    await firstValueFrom(ref.afterClosed());
  }

  private async confirm(data: ConfirmData): Promise<ConfirmResult> {
    const ref = this.dialog.open<ConfirmDialogComponent, ConfirmData, ConfirmResult>(ConfirmDialogComponent, {
      width: '480px',
      ariaLabelledBy: 'confirm-title',
      data,
    });
    return (await firstValueFrom(ref.afterClosed())) ?? { confirmed: false };
  }
}
