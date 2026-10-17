import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { createCountdown } from '../../../core/utils/countdown';
import { emailValidator } from '../../../core/utils/validators';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { TextFieldComponent } from '../../../shared/components/text-field/text-field.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { AuthApiService } from '../services/auth-api.service';

type Kind = 'resend' | 'forgot';

/**
 * SCR-04 (reenviar verificação) e SCR-07 (esqueci a senha): um único campo de e-mail e SEMPRE a mesma
 * mensagem neutra, exista a conta ou não (D-13). Rate limit mostra contagem regressiva.
 */
@Component({
  selector: 'app-email-only-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, FocusInvalidDirective,
    AuthLayoutComponent, FormAlertComponent, TextFieldComponent,
  ],
  template: `
    <app-auth-layout [title]="(kind + '.title') | transloco" [subtitle]="(kind + '.subtitle') | transloco">
      @if (sent()) {
        <p role="status">{{ (kind + '.sent') | transloco }}</p>
        <a mat-stroked-button routerLink="/login">{{ 'auth.backToLogin' | transloco }}</a>
      } @else {
        <form class="form-column" [formGroup]="form" (ngSubmit)="submit()" appFocusInvalid novalidate>
          @if (failure(); as f) {
            <app-form-alert [message]="failureMessage()" [traceId]="f.traceId" />
          }
          @if (blocked()) {
            <p class="muted" role="status">{{ 'common.retryIn' | transloco: { seconds: wait() } }}</p>
          }
          <app-text-field name="email" type="email" inputmode="email" autocomplete="email" [maxlength]="254"
            [control]="form.controls.email" [label]="'auth.email' | transloco" [required]="true" />
          <div class="actions">
            <button mat-flat-button type="submit" [disabled]="busy() || blocked()" [attr.aria-busy]="busy()">
              @if (busy()) { <mat-spinner diameter="18" /> }
              {{ (kind + '.submit') | transloco }}
            </button>
            <a routerLink="/login">{{ 'auth.backToLogin' | transloco }}</a>
          </div>
        </form>
      }
    </app-auth-layout>
  `,
})
export class EmailOnlyPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuthApiService);
  private readonly errors = inject(ErrorMessageService);
  private readonly countdown = createCountdown();
  readonly kind = (inject(ActivatedRoute).snapshot.data['kind'] as Kind) ?? 'forgot';

  readonly busy = signal(false);
  readonly sent = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly wait = this.countdown.remaining;
  readonly blocked = computed(() => this.wait() > 0);
  readonly failureMessage = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, emailValidator]] });

  async submit(): Promise<void> {
    this.failure.set(null);
    if (this.form.invalid || this.busy() || this.blocked()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      const email = this.form.controls.email.value.trim();
      await firstValueFrom(this.kind === 'resend' ? this.api.resendVerification(email) : this.api.forgotPassword(email));
      this.sent.set(true);
      if (this.kind === 'resend') {
        this.countdown.start(60); // intervalo mínimo entre reenvios
      }
    } catch (e) {
      const err = toApiError(e);
      if (err.retryAfterSeconds) {
        this.countdown.start(err.retryAfterSeconds);
      }
      this.failure.set(err);
    } finally {
      this.busy.set(false);
    }
  }
}
