import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { takeFragmentToken } from '../../../core/utils/fragment';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { AuthApiService } from '../services/auth-api.service';

type View = 'loading' | 'done' | 'invalid' | 'error';

/** SCR-03: confirma o e-mail com o token do fragmento (removido da barra de endereço). */
@Component({
  selector: 'app-verify-email-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, AuthLayoutComponent],
  template: `
    <app-auth-layout [title]="'auth.verify.title' | transloco">
      @switch (view()) {
        @case ('loading') {
          <p role="status"><mat-spinner diameter="20" class="inline" /> {{ 'auth.verify.loading' | transloco }}</p>
        }
        @case ('done') {
          <p role="status">{{ 'auth.verify.done' | transloco }}</p>
          <a mat-flat-button routerLink="/login">{{ 'auth.signIn' | transloco }}</a>
        }
        @case ('invalid') {
          <p role="alert">{{ 'auth.verify.invalid' | transloco }}</p>
          <a mat-stroked-button routerLink="/resend-verification">{{ 'auth.resend' | transloco }}</a>
        }
        @default {
          <p role="alert">{{ message() }}</p>
          <a mat-stroked-button routerLink="/login">{{ 'auth.backToLogin' | transloco }}</a>
        }
      }
    </app-auth-layout>
  `,
  styles: `
    .inline {
      display: inline-block;
      vertical-align: middle;
      margin-right: 8px;
    }
  `,
})
export class VerifyEmailPage implements OnInit {
  private readonly api = inject(AuthApiService);
  private readonly errors = inject(ErrorMessageService);
  readonly view = signal<View>('loading');
  readonly failure = signal<ApiError | null>(null);
  readonly message = computed(() => (this.failure() ? this.errors.message(this.failure() as ApiError) : ''));

  async ngOnInit(): Promise<void> {
    const token = takeFragmentToken();
    if (!token) {
      this.view.set('invalid');
      return;
    }
    try {
      await firstValueFrom(this.api.verifyEmail(token));
      this.view.set('done');
    } catch (e) {
      const err = toApiError(e);
      this.failure.set(err);
      this.view.set(err.code === 'TOKEN_INVALID_OR_EXPIRED' ? 'invalid' : 'error');
    }
  }
}
