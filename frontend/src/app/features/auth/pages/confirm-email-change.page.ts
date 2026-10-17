import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { takeFragmentToken } from '../../../core/utils/fragment';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { AuthApiService } from '../services/auth-api.service';

type View = 'loading' | 'done' | 'invalid' | 'error';

/** SCR-12: confirma a troca de e-mail (UC-036). Todas as sessões foram encerradas pelo servidor. */
@Component({
  selector: 'app-confirm-email-change-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, AuthLayoutComponent],
  template: `
    <app-auth-layout [title]="'auth.emailChange.title' | transloco">
      @switch (view()) {
        @case ('loading') {
          <p role="status"><mat-spinner diameter="20" /> {{ 'auth.verify.loading' | transloco }}</p>
        }
        @case ('done') {
          <p role="status">{{ 'auth.emailChange.done' | transloco }}</p>
          <a mat-flat-button routerLink="/login">{{ 'auth.signIn' | transloco }}</a>
        }
        @case ('invalid') {
          <p role="alert">{{ 'auth.emailChange.invalid' | transloco }}</p>
          <a mat-stroked-button routerLink="/login">{{ 'auth.backToLogin' | transloco }}</a>
        }
        @default {
          <p role="alert">{{ message() }}</p>
          <a mat-stroked-button routerLink="/login">{{ 'auth.backToLogin' | transloco }}</a>
        }
      }
    </app-auth-layout>
  `,
})
export class ConfirmEmailChangePage implements OnInit {
  private readonly api = inject(AuthApiService);
  private readonly session = inject(SessionService);
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
      await firstValueFrom(this.api.confirmEmailChange(token));
      this.session.clearLocal(); // as sessões foram encerradas no servidor
      this.view.set('done');
    } catch (e) {
      const err = toApiError(e);
      this.failure.set(err);
      this.view.set(err.code === 'TOKEN_INVALID_OR_EXPIRED' ? 'invalid' : 'error');
    }
  }
}
