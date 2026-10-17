import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { SocialFlowService } from '../../../core/auth/social-flow.service';
import { toApiError } from '../../../core/http/api-error';
import { NotifyService } from '../../../core/services/notify.service';
import { safeNext } from '../../../core/utils/safe-redirect';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';
import { AuthApiService } from '../services/auth-api.service';

/** Erros devolvidos pelo Identity no redirecionamento (social-login.md §3) ⇒ chave i18n. */
const KNOWN_ERRORS = [
  'access_denied', 'account_link_required', 'account_disabled', 'email_verification_required', 'email_unavailable',
  'temporarily_unavailable', 'already_linked', 'reauth_failed', 'invalid_request',
];

export function socialErrorKey(code: string | null): string {
  return `social.error.${code && KNOWN_ERRORS.includes(code) ? code : 'invalid_request'}`;
}

/** SCR-16: retorno do login/vínculo/reautenticação social. Valida `state` e troca o código com PKCE. */
@Component({
  selector: 'app-social-callback-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, AuthLayoutComponent],
  template: `
    <app-auth-layout [title]="'social.callback.title' | transloco">
      @if (errorKey(); as key) {
        <p role="alert">{{ key | transloco }}</p>
        <a mat-flat-button routerLink="/login">{{ 'auth.backToLogin' | transloco }}</a>
      } @else {
        <p role="status"><mat-spinner diameter="20" /> {{ 'social.callback.working' | transloco }}</p>
      }
    </app-auth-layout>
  `,
})
export class SocialCallbackPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly social = inject(SocialFlowService);
  private readonly api = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly notify = inject(NotifyService);
  private readonly failureKey = signal<string | null>(null);
  readonly errorKey = computed(() => this.failureKey());

  async ngOnInit(): Promise<void> {
    const q = this.route.snapshot.queryParamMap;
    const pending = this.social.consume(q.get('state'));
    const error = q.get('error');
    if (!pending) {
      this.failureKey.set(socialErrorKey('invalid_request'));
      return;
    }
    if (error) {
      await this.failBack(pending.purpose, error, pending.returnTo);
      return;
    }
    try {
      if (pending.purpose === 'login') {
        const code = q.get('code');
        if (!code || !pending.verifier) {
          this.failureKey.set(socialErrorKey('invalid_request'));
          return;
        }
        const tokens = await firstValueFrom(this.api.exchangeSocial(code, pending.verifier, this.social.redirectUri));
        await this.session.establish(tokens);
        await this.router.navigateByUrl(safeNext(pending.next));
      } else if (pending.purpose === 'link') {
        this.notify.success('social.linked');
        await this.router.navigateByUrl('/account/security');
      } else {
        this.notify.success('social.reauthenticated');
        await this.router.navigateByUrl(safeNext(pending.returnTo, '/account/security'));
      }
    } catch (e) {
      const err = toApiError(e);
      this.failureKey.set(err.code === 'INVALID_GRANT' ? socialErrorKey('invalid_request') : `error.${err.code}`);
    }
  }

  private async failBack(purpose: string, error: string, returnTo?: string): Promise<void> {
    if (purpose === 'login') {
      this.failureKey.set(socialErrorKey(error));
      return;
    }
    this.notify.failure(socialErrorKey(error));
    await this.router.navigateByUrl(safeNext(returnTo, '/account/security'));
  }
}
