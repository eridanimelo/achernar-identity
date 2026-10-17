import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { RuntimeConfigService } from '../config/runtime-config';
import { ApiError, toApiError } from '../http/api-error';
import { AccountView, AuthState, TokenResponse } from './auth.models';
import { RefreshCoordinator } from './refresh-coordinator';
import { TokenStore } from './token-store';

export type RefreshResult = 'ok' | 'denied' | 'unavailable';

const CHANNEL = 'achernar-identity-session';

/**
 * Estado de autenticação da UI (ui-map §7): unknown → authenticated | anonymous.
 * Sem armazenamento de tokens (SEC-12): o access token vive em memória e o refresh é o cookie HttpOnly.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly store = inject(TokenStore);
  private readonly coordinator = inject(RefreshCoordinator);
  private readonly cfg = inject(RuntimeConfigService);
  private readonly router = inject(Router);
  private channel: BroadcastChannel | null = null;

  readonly state = signal<AuthState>('unknown');
  readonly account = signal<AccountView | null>(null);
  readonly sessionId = signal<string | null>(null);
  readonly isAuthenticated = computed(() => this.state() === 'authenticated');
  readonly isAdmin = computed(() => this.account()?.role === 'ADMIN');

  /** Inicialização: tenta renovar silenciosamente pelo cookie de refresh. */
  async init(): Promise<void> {
    this.openChannel();
    const result = await this.refresh();
    if (result === 'ok') {
      await this.loadAccount();
    } else {
      this.state.set('anonymous');
    }
  }

  async login(email: string, password: string): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<TokenResponse>('/auth/login', { client_id: this.cfg.clientId, email, password }),
    );
    await this.establish(res);
    this.broadcast('login');
  }

  /** Conclui uma sessão obtida por outro fluxo (troca do código social). */
  async establish(res: TokenResponse): Promise<void> {
    this.store.set(res.access_token, res.expires_in);
    this.sessionId.set(res.session_id);
    await this.loadAccount();
  }

  async loadAccount(): Promise<AccountView> {
    try {
      const acc = await firstValueFrom(this.http.get<AccountView>('/api/v1/account'));
      this.account.set(acc);
      this.state.set('authenticated');
      return acc;
    } catch (e) {
      this.clearLocal();
      throw toApiError(e);
    }
  }

  /** Renova o access token. 'denied' = refresh recusado (sessão acabou); 'unavailable' = falha transitória. */
  refresh(): Promise<RefreshResult> {
    return this.coordinator.run(async () => {
      try {
        const res = await firstValueFrom(this.http.post<TokenResponse>('/auth/refresh', {}));
        this.store.set(res.access_token, res.expires_in);
        this.sessionId.set(res.session_id);
        return 'ok' as const;
      } catch (e) {
        const err = toApiError(e);
        return err.status === 401 || err.status === 403 || err.status === 400 ? ('denied' as const) : ('unavailable' as const);
      }
    });
  }

  /** Garante um access token válido para a próxima chamada (renova se faltar pouco). */
  async ensureFresh(): Promise<void> {
    if (this.store.hasToken() && this.store.expiresSoon()) {
      await this.refresh();
    }
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/auth/logout', {}));
    } catch {
      // o logout local acontece mesmo se a rede falhar
    }
    this.clearLocal();
    this.broadcast('logout');
    await this.router.navigateByUrl('/login');
  }

  /** Sessão perdida (refresh recusado): volta a anônimo e explica em SCR-23. */
  expire(): void {
    if (this.state() !== 'authenticated') {
      return;
    }
    this.clearLocal();
    void this.router.navigateByUrl('/session-expired');
  }

  clearLocal(): void {
    this.store.clear();
    this.account.set(null);
    this.sessionId.set(null);
    this.state.set('anonymous');
  }

  private openChannel(): void {
    if (this.channel || typeof BroadcastChannel === 'undefined') {
      return;
    }
    this.channel = new BroadcastChannel(CHANNEL);
    this.channel.onmessage = (ev: MessageEvent<string>) => {
      if (ev.data === 'logout' && this.state() === 'authenticated') {
        this.clearLocal();
        void this.router.navigateByUrl('/login');
      } else if (ev.data === 'login' && this.state() !== 'authenticated') {
        void this.init().then(() => {
          if (this.isAuthenticated()) {
            void this.router.navigateByUrl('/account');
          }
        });
      }
    };
  }

  private broadcast(msg: 'login' | 'logout'): void {
    this.channel?.postMessage(msg);
  }
}

export function isRetryable(err: ApiError): boolean {
  return err.status === 0 || err.status >= 500;
}
