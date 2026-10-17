import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { takeFragmentToken } from '../../../core/utils/fragment';
import { matchValidator, passwordLengthValidator } from '../../../core/utils/validators';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { ErrorSummaryComponent, SummaryItem } from '../../../shared/components/error-summary/error-summary.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PasswordFieldComponent } from '../../../shared/components/password-field/password-field.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { firstError } from '../../../shared/utils/form-errors';
import { applyServerErrors } from '../../../shared/utils/server-errors';
import { AuthApiService } from '../services/auth-api.service';

/** SCR-08: redefinição de senha com o token do fragmento. */
@Component({
  selector: 'app-reset-password-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, FocusInvalidDirective,
    AuthLayoutComponent, ErrorSummaryComponent, FormAlertComponent, PasswordFieldComponent,
  ],
  templateUrl: './reset-password.page.html',
})
export class ResetPasswordPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuthApiService);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);
  private token: string | null = null;

  readonly hasToken = signal(true);
  readonly busy = signal(false);
  readonly done = signal(false);
  readonly invalidToken = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly summary = signal<SummaryItem[]>([]);
  readonly submits = signal(0);
  readonly failureMessage = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });

  readonly form = this.fb.nonNullable.group(
    { password: ['', [Validators.required, passwordLengthValidator()]], confirm: ['', [Validators.required]] },
    { validators: [matchValidator('password', 'confirm')] },
  );

  ngOnInit(): void {
    this.token = takeFragmentToken();
    this.hasToken.set(this.token !== null);
  }

  async submit(): Promise<void> {
    this.submits.update((n) => n + 1);
    this.failure.set(null);
    this.summary.set(this.collect());
    if (this.form.invalid || this.busy() || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      await firstValueFrom(this.api.resetPassword(this.token, this.form.controls.password.value));
      this.done.set(true);
    } catch (e) {
      const err = toApiError(e);
      if (err.code === 'TOKEN_INVALID_OR_EXPIRED') {
        this.invalidToken.set(true);
      } else {
        const unmatched = applyServerErrors(this.form, err, { new_password: 'password' });
        this.summary.set(this.collect());
        if (err.errors.length === 0 || unmatched.length > 0) {
          this.failure.set(err);
        }
      }
    } finally {
      this.busy.set(false);
    }
  }

  private collect(): SummaryItem[] {
    const labels: Record<string, string> = { password: 'auth.newPassword', confirm: 'auth.confirmPassword' };
    const items: SummaryItem[] = [];
    for (const [name, control] of Object.entries(this.form.controls)) {
      const e = control.errors ? firstError(control) : null;
      if (e) {
        items.push({ field: name, message: `${this.i18n.translate(labels[name])}: ${this.i18n.translate(e.key, e.params)}` });
      }
    }
    if (this.form.errors?.['mismatch'] && !this.form.controls.confirm.errors) {
      items.push({ field: 'confirm', message: this.i18n.translate('validation.mismatch') });
    }
    return items;
  }
}
