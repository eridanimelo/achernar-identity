import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { SetupStatusService } from '../../../core/auth/guards';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { LocaleService } from '../../../core/services/locale.service';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { createCountdown } from '../../../core/utils/countdown';
import { emailValidator, matchValidator, passwordLengthValidator } from '../../../core/utils/validators';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { ErrorSummaryComponent, SummaryItem } from '../../../shared/components/error-summary/error-summary.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PasswordFieldComponent } from '../../../shared/components/password-field/password-field.component';
import { TextFieldComponent } from '../../../shared/components/text-field/text-field.component';
import { firstError } from '../../../shared/utils/form-errors';
import { applyServerErrors } from '../../../shared/utils/server-errors';
import { SetupService } from '../services/setup.service';

/** SCR-01: criação do primeiro administrador (UC-001). */
@Component({
  selector: 'app-setup-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, FocusInvalidDirective,
    AuthLayoutComponent, ErrorSummaryComponent, FormAlertComponent, PasswordFieldComponent, TextFieldComponent,
  ],
  templateUrl: './setup.page.html',
})
export class SetupPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(SetupService);
  private readonly status = inject(SetupStatusService);
  private readonly router = inject(Router);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);
  private readonly locale = inject(LocaleService);
  private readonly countdown = createCountdown();

  readonly busy = signal(false);
  readonly done = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly submits = signal(0);
  readonly summary = signal<SummaryItem[]>([]);
  readonly wait = this.countdown.remaining;
  readonly blocked = computed(() => this.wait() > 0);

  readonly form = this.fb.nonNullable.group(
    {
      bootstrap_token: ['', [Validators.required, Validators.minLength(32)]],
      email: ['', [Validators.required, emailValidator]],
      password: ['', [Validators.required, passwordLengthValidator()]],
      confirm: ['', [Validators.required]],
    },
    { validators: [matchValidator('password', 'confirm')] },
  );

  readonly failureMessage = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });

  async submit(): Promise<void> {
    this.submits.update((n) => n + 1);
    this.failure.set(null);
    this.summary.set(this.collect());
    if (this.form.invalid || this.blocked() || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      await firstValueFrom(
        this.api.create({ bootstrap_token: v.bootstrap_token, email: v.email.trim(), password: v.password, locale: this.locale.lang() }),
      );
      this.status.markDone();
      this.done.set(true);
    } catch (e) {
      const err = toApiError(e);
      if (err.retryAfterSeconds) {
        this.countdown.start(err.retryAfterSeconds);
      }
      applyServerErrors(this.form, err);
      this.summary.set(this.collect());
      this.failure.set(err);
    } finally {
      this.busy.set(false);
    }
  }

  signIn(): void {
    void this.router.navigateByUrl('/login');
  }

  private collect(): SummaryItem[] {
    const items: SummaryItem[] = [];
    const labels: Record<string, string> = {
      bootstrap_token: 'setup.token',
      email: 'auth.email',
      password: 'auth.password',
      confirm: 'auth.confirmPassword',
    };
    for (const [name, control] of Object.entries(this.form.controls)) {
      const e = control.errors ? firstError(control) : null;
      if (e) {
        items.push({ field: name, message: `${this.i18n.translate(labels[name])}: ${this.i18n.translate(e.key, e.params)}` });
      }
    }
    if (this.form.errors?.['mismatch']) {
      items.push({ field: 'confirm', message: this.i18n.translate('validation.mismatch') });
    }
    return items;
  }
}
