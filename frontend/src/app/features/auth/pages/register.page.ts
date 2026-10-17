import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { createCountdown } from '../../../core/utils/countdown';
import { emailValidator, matchValidator, passwordLengthValidator } from '../../../core/utils/validators';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { ErrorSummaryComponent, SummaryItem } from '../../../shared/components/error-summary/error-summary.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PasswordFieldComponent } from '../../../shared/components/password-field/password-field.component';
import { TextFieldComponent } from '../../../shared/components/text-field/text-field.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { firstError } from '../../../shared/utils/form-errors';
import { applyServerErrors } from '../../../shared/utils/server-errors';
import { PublicConfig } from '../models/auth.models';
import { AuthApiService } from '../services/auth-api.service';

/** SCR-02: cadastro. Sucesso sempre leva a SCR-05, mesmo para e-mail já cadastrado (D-13). */
@Component({
  selector: 'app-register-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, FocusInvalidDirective,
    AuthLayoutComponent, ErrorSummaryComponent, FormAlertComponent, PasswordFieldComponent, TextFieldComponent,
  ],
  templateUrl: './register.page.html',
})
export class RegisterPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly errors = inject(ErrorMessageService);
  private readonly i18n = inject(TranslocoService);
  private readonly countdown = createCountdown();

  readonly config = signal<PublicConfig | null>(null);
  readonly loaded = signal(false);
  readonly busy = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly summary = signal<SummaryItem[]>([]);
  readonly submits = signal(0);
  readonly wait = this.countdown.remaining;
  readonly blocked = computed(() => this.wait() > 0);
  readonly unavailable = computed(() => this.loaded() && !(this.config()?.allow_registration ?? false));
  readonly failureMessage = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });

  readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, emailValidator]],
      password: ['', [Validators.required, passwordLengthValidator()]],
      confirm: ['', [Validators.required]],
    },
    { validators: [matchValidator('password', 'confirm')] },
  );

  async ngOnInit(): Promise<void> {
    try {
      this.config.set(await firstValueFrom(this.api.config()));
    } catch {
      this.config.set(null);
    } finally {
      this.loaded.set(true);
    }
  }

  async submit(): Promise<void> {
    this.submits.update((n) => n + 1);
    this.failure.set(null);
    this.summary.set(this.collect());
    if (this.form.invalid || this.busy() || this.blocked()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      await firstValueFrom(this.api.register(v.email.trim(), v.password));
      await this.router.navigateByUrl('/register/check-email');
    } catch (e) {
      const err = toApiError(e);
      if (err.retryAfterSeconds) {
        this.countdown.start(err.retryAfterSeconds);
      }
      const unmatched = applyServerErrors(this.form, err, { new_password: 'password' });
      this.summary.set(this.collect());
      if (err.errors.length === 0 || unmatched.length > 0) {
        this.failure.set(err);
      }
    } finally {
      this.busy.set(false);
    }
  }

  private collect(): SummaryItem[] {
    const labels: Record<string, string> = { email: 'auth.email', password: 'auth.password', confirm: 'auth.confirmPassword' };
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
