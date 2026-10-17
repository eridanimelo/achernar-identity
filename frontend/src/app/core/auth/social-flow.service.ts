import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { RuntimeConfigService } from '../config/runtime-config';
import { challengeFor, randomToken } from '../utils/pkce';
import { safeGet, safeRemove, safeSet } from '../utils/safe-storage';
import { Provider } from './auth.models';

export type SocialPurpose = 'login' | 'link' | 'reauth';

export interface PendingSocial {
  state: string;
  verifier?: string;
  purpose: SocialPurpose;
  next?: string;
  returnTo?: string;
  ts: number;
}

const KEY = 'achernar.social';
const TTL_MS = 10 * 60 * 1000;
export const CALLBACK_PATH = '/auth/callback';

/**
 * Login social (SCR-15/16): gera `state` e `code_verifier` (PKCE S256), guarda-os em sessionStorage por no
 * máximo 10 min (exceção documentada do ui-map §7; nunca tokens) e navega até o Identity.
 */
@Injectable({ providedIn: 'root' })
export class SocialFlowService {
  private readonly http = inject(HttpClient);
  private readonly cfg = inject(RuntimeConfigService);

  get redirectUri(): string {
    return this.cfg.uiOrigin + CALLBACK_PATH;
  }

  async startLogin(provider: Provider, next?: string): Promise<void> {
    const state = randomToken(24);
    const verifier = randomToken(48);
    const challenge = await challengeFor(verifier);
    this.save({ state, verifier, purpose: 'login', next, ts: Date.now() });
    const q = new URLSearchParams({
      client_id: this.cfg.clientId,
      redirect_uri: this.redirectUri,
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });
    window.location.assign(this.cfg.url(`/auth/social/${provider}/start?${q.toString()}`));
  }

  /** Vincular (link) ou reautenticar (reauth) por um provedor: obtém a intenção e navega. */
  async startIntent(kind: 'link' | 'reauth', provider: Provider, returnTo: string): Promise<void> {
    const state = randomToken(24);
    const path =
      kind === 'link'
        ? `/api/v1/account/social-links/${provider}/intent`
        : `/api/v1/account/reauth/social/${provider}/intent`;
    const res = await firstValueFrom(this.http.post<{ authorization_url: string }>(path, { return_url: this.redirectUri, state }));
    this.save({ state, purpose: kind, returnTo, ts: Date.now() });
    window.location.assign(res.authorization_url);
  }

  /** Lê e consome o estado pendente; devolve null se ausente, expirado ou com `state` diferente. */
  consume(returnedState: string | null): PendingSocial | null {
    const raw = safeGet('session', KEY);
    safeRemove('session', KEY);
    if (!raw || !returnedState) {
      return null;
    }
    try {
      const p = JSON.parse(raw) as PendingSocial;
      if (p.state !== returnedState || Date.now() - p.ts > TTL_MS) {
        return null;
      }
      return p;
    } catch {
      return null;
    }
  }

  private save(p: PendingSocial): void {
    safeSet('session', KEY, JSON.stringify(p));
  }
}
