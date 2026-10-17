import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { Provider } from '../../../core/auth/auth.models';
import { SessionService } from '../../../core/auth/session.service';
import { SocialFlowService } from '../../../core/auth/social-flow.service';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { createCountdown } from '../../../core/utils/countdown';
import { safeNext } from '../../../core/utils/safe-redirect';
import { emailValidator } from '../../../core/utils/validators';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PasswordFieldComponent } from '../../../shared/components/password-field/password-field.component';
import { TextFieldComponent } from '../../../shared/components/text-field/text-field.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { SocialButtonsComponent } from '../components/social-buttons.component';
import { PublicConfig } from '../models/auth.models';
import { AuthApiService } from '../services/auth-api.service';

/** SCR-06: login por e-mail e senha + botões sociais. */
@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule, TranslocoPipe, FocusInvalidDirective,
    AuthLayoutComponent, FormAlertComponent, PasswordFieldComponent, TextFieldComponent, SocialButtonsComponent,
  ],
  templateUrl: './login.page.html',
})
export class LoginPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly api = inject(AuthApiService);
  private readonly social = inject(SocialFlowService);
  private readonly errors = inject(ErrorMessageService);
  private readonly countdown = createCountdown();

  /** Query param `next` (vinculado pelo roteador). */
  readonly next = input<string | undefined>();

  readonly busy = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly config = signal<PublicConfig | null>(null);
  readonly wait = this.countdown.remaining;
  readonly blocked = computed(() => this.wait() > 0);
  readonly providers = computed<Provider[]>(() => this.config()?.providers ?? []);
  readonly canRegister = computed(() => this.config()?.allow_registration ?? false);
  readonly unverified = computed(() => this.failure()?.code === 'EMAIL_NOT_VERIFIED');
  readonly failureMessage = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, emailValidator]],
    password: ['', [Validators.required]],
  });

  async ngOnInit(): Promise<void> {
    try {
      this.config.set(await firstValueFrom(this.api.config()));
    } catch {
      this.config.set(null); // sem botões sociais/cadastro; o login por senha segue disponível
    }
  }

  async submit(): Promise<void> {
    this.failure.set(null);
    if (this.form.invalid || this.busy() || this.blocked()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      await this.session.login(v.email.trim(), v.password);
      await this.router.navigateByUrl(safeNext(this.next()));
    } catch (e) {
      const err = toApiError(e);
      if (err.retryAfterSeconds) {
        this.countdown.start(err.retryAfterSeconds);
      }
      this.form.controls.password.reset('');
      this.failure.set(err);
    } finally {
      this.busy.set(false);
    }
  }

  async withProvider(p: Provider): Promise<void> {
    try {
      await this.social.startLogin(p, this.next());
    } catch (e) {
      this.failure.set(toApiError(e));
    }
  }
}
