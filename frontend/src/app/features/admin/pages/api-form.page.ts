import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { NotifyService } from '../../../core/services/notify.service';
import { isValidAudience } from '../../../core/utils/validators';
import { ConfirmDialogComponent, ConfirmData, ConfirmResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SecretRevealDialogComponent } from '../../../shared/components/secret-reveal-dialog/secret-reveal-dialog.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { TextFieldComponent } from '../../../shared/components/text-field/text-field.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { applyServerErrors } from '../../../shared/utils/server-errors';
import { SecretsSectionComponent } from '../components/secrets-section.component';
import { ApiSummary } from '../models/admin.models';
import { AdminApisService } from '../services/admin-apis.service';

/** SCR-28: criar/editar API (audiência) e gerenciar a credencial de introspecção. */
@Component({
  selector: 'app-api-form-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatProgressSpinnerModule, MatSelectModule, MatFormFieldModule,
    TranslocoPipe, FocusInvalidDirective, PageHeaderComponent, FormAlertComponent, SkeletonComponent, TextFieldComponent,
    SecretsSectionComponent,
  ],
  templateUrl: './api-form.page.html',
})
export class ApiFormPage implements OnInit {
  private readonly api = inject(AdminApisService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);

  readonly id = input<string | undefined>();
  readonly isNew = computed(() => !this.id());
  readonly current = signal<ApiSummary | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly message = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });
  readonly title = computed(() => (this.isNew() ? this.i18n.translate('admin.apis.new') : (this.current()?.name ?? '')));

  readonly form = this.fb.nonNullable.group({
    audience: ['', [Validators.required, (c: { value: string }) => (!c.value || isValidAudience(c.value) ? null : { audience: true })]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    status: ['ACTIVE'],
  });

  async ngOnInit(): Promise<void> {
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
      this.current.set(a);
      this.form.patchValue({ audience: a.audience, name: a.name, description: a.description ?? '', status: a.status });
      this.form.controls.audience.disable();
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    this.failure.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy()) {
      return;
    }
    const v = this.form.getRawValue();
    if (!this.isNew() && v.status === 'DISABLED' && this.current()?.status === 'ACTIVE') {
      const r = await this.confirm({
        title: this.i18n.translate('admin.apis.disableTitle'),
        message: this.i18n.translate('admin.apis.disableMessage'),
        confirmLabel: this.i18n.translate('admin.apis.disable'),
        destructive: true,
      });
      if (!r.confirmed) {
        return;
      }
    }
    this.busy.set(true);
    try {
      if (this.isNew()) {
        const created = await firstValueFrom(
          this.api.create({ audience: v.audience.trim(), name: v.name.trim(), description: v.description.trim() || null }),
        );
        await this.reveal(created.credential.secret);
        this.notify.success('admin.apis.created');
        await this.router.navigate(['/admin/apis', created.id]);
      } else {
        const updated = await firstValueFrom(
          this.api.update(this.id() as string, {
            name: v.name.trim(),
            description: v.description.trim() || null,
            status: v.status,
            version: this.current()?.version ?? 0,
          }),
        );
        this.current.set(updated);
        this.notify.success('admin.apis.saved');
      }
    } catch (e) {
      const err = toApiError(e);
      const unmatched = applyServerErrors(this.form, err);
      if (err.errors.length === 0 || unmatched.length > 0) {
        this.failure.set(err);
      }
    } finally {
      this.busy.set(false);
    }
  }

  async generateCredential(): Promise<void> {
    const a = this.current();
    if (!a) {
      return;
    }
    await this.mutate(async () => {
      const c = await firstValueFrom(this.api.createCredential(a.id));
      await this.reveal(c.secret);
      await this.load();
    });
  }

  async revokeCredential(credentialId: string): Promise<void> {
    const a = this.current();
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
        await firstValueFrom(this.api.revokeCredential(a.id, credentialId));
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

  private async reveal(secret: string): Promise<void> {
    const ref = this.dialog.open(SecretRevealDialogComponent, {
      width: '560px',
      disableClose: true,
      ariaLabelledBy: 'secret-title',
      data: { title: this.i18n.translate('secrets.apiSecretTitle'), secret, note: this.i18n.translate('secrets.apiSecretNote') },
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
