import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TokenResponse } from '../../../core/auth/auth.models';
import { RuntimeConfigService } from '../../../core/config/runtime-config';
import { LocaleService } from '../../../core/services/locale.service';
import { PublicConfig } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly cfg = inject(RuntimeConfigService);
  private readonly locale = inject(LocaleService);

  config(): Observable<PublicConfig> {
    return this.http.get<PublicConfig>(`/auth/applications/${encodeURIComponent(this.cfg.clientId)}/config`);
  }

  register(email: string, password: string): Observable<unknown> {
    return this.http.post('/auth/register', {
      client_id: this.cfg.clientId,
      email,
      password,
      locale: this.locale.lang(),
    });
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>('/auth/email/verify', { client_id: this.cfg.clientId, token });
  }

  resendVerification(email: string): Observable<unknown> {
    return this.http.post('/auth/email/resend-verification', { client_id: this.cfg.clientId, email });
  }

  forgotPassword(email: string): Observable<unknown> {
    return this.http.post('/auth/password/forgot', { client_id: this.cfg.clientId, email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/auth/password/reset', {
      client_id: this.cfg.clientId,
      token,
      new_password: newPassword,
    });
  }

  confirmEmailChange(token: string): Observable<void> {
    return this.http.post<void>('/auth/email-change/confirm', { client_id: this.cfg.clientId, token });
  }

  exchangeSocial(code: string, verifier: string, redirectUri: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/auth/social/exchange', {
      client_id: this.cfg.clientId,
      code,
      code_verifier: verifier,
      redirect_uri: redirectUri,
    });
  }
}
