import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Provider } from '../../../core/auth/auth.models';
import { ChangePasswordBody, SessionItem } from '../models/account.models';

/** Chamadas da área "Minha conta" (FT-05/06/08). A reautenticação é tratada pelo interceptor. */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  changePassword(body: ChangePasswordBody): Observable<void> {
    return this.http.post<void>('/api/v1/account/password', body);
  }

  requestEmailChange(newEmail: string, clientId: string): Observable<unknown> {
    return this.http.post('/api/v1/account/email-change', { new_email: newEmail, client_id: clientId });
  }

  unlinkSocial(provider: Provider): Observable<void> {
    return this.http.delete<void>(`/api/v1/account/social-links/${provider}`);
  }

  sessions(): Observable<SessionItem[]> {
    return this.http.get<{ items: SessionItem[] }>('/api/v1/sessions').pipe(map((r) => r.items));
  }

  revokeSession(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/sessions/${id}`);
  }

  endAll(keepCurrent: boolean): Observable<void> {
    const params = new HttpParams().set('keep_current', String(keepCurrent));
    return this.http.delete<void>('/api/v1/sessions', { params });
  }
}
