import { HttpClient, HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { PasswordFieldComponent } from '../../../shared/components/password-field/password-field.component';
import { ApiError, toApiError } from '../../http/api-error';
import { ErrorMessageService } from '../../services/error-message.service';
import { SKIP_AUTH_RETRY } from '../api.interceptor';
import { Provider } from '../auth.models';
import { SessionService } from '../session.service';
import { SocialFlowService } from '../social-flow.service';

/** SCR-11: reautenticação por senha ou por provedor social vinculado. */
@Component({
  selector: 'app-reauth-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatProgressSpinnerModule, TranslocoPipe, PasswordFieldComponent],
  templateUrl: './reauth-dialog.component.html',
  styleUrl: './reauth-dialog.component.scss',
})
export class ReauthDialogComponent {
  private readonly http = inject(HttpClient);
  private readonly ref = inject(MatDialogRef<ReauthDialogComponent, boolean>);
  private readonly session = inject(SessionService);
  private readonly social = inject(SocialFlowService);
  private readonly router = inject(Router);
  private readonly errors = inject(ErrorMessageService);
  private readonly fb = inject(FormBuilder);

  readonly hasPassword = computed(() => this.session.account()?.has_password ?? false);
  readonly providers = computed<Provider[]>(() => (this.session.account()?.social_links ?? []).map((l) => l.provider));
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.nonNullable.group({ password: ['', [Validators.required]] });

  async submit(): Promise<void> {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post('/api/v1/account/reauth', { password: this.form.controls.password.value }, {
          context: new HttpContext().set(SKIP_AUTH_RETRY, true),
        }),
      );
      this.ref.close(true);
    } catch (e) {
      const err: ApiError = toApiError(e);
      this.error.set(this.errors.message(err));
      this.form.controls.password.reset('');
    } finally {
      this.busy.set(false);
    }
  }

  async withProvider(p: Provider): Promise<void> {
    this.busy.set(true);
    try {
      await this.social.startIntent('reauth', p, this.router.url);
    } catch (e) {
      this.error.set(this.errors.message(toApiError(e)));
      this.busy.set(false);
    }
  }

  cancel(): void {
    this.ref.close(false);
  }
}
