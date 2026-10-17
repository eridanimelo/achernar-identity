import { Injectable, computed, signal } from '@angular/core';

/**
 * Access token apenas em memória (SEC-12): nunca em localStorage/sessionStorage/cookie legível.
 * Recarregar a página descarta o token e a UI o recupera pelo cookie HttpOnly de refresh.
 */
@Injectable({ providedIn: 'root' })
export class TokenStore {
  private readonly _token = signal<string | null>(null);
  private readonly _expiresAt = signal<number>(0);

  readonly token = this._token.asReadonly();
  readonly hasToken = computed(() => this._token() !== null);

  set(token: string, expiresInSeconds: number, now: number = Date.now()): void {
    this._token.set(token);
    this._expiresAt.set(now + expiresInSeconds * 1000);
  }

  clear(): void {
    this._token.set(null);
    this._expiresAt.set(0);
  }

  /** Verdadeiro se o token expira em até `skewMs` (padrão 15 s) — renovar antes de usar. */
  expiresSoon(skewMs = 15_000, now: number = Date.now()): boolean {
    return this._token() !== null && this._expiresAt() - now <= skewMs;
  }
}
