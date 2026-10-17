import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../../../core/models/api.types';
import { AccountFilters, AdminAccountDetail, AdminAccountSummary } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminAccountsService {
  private readonly http = inject(HttpClient);

  list(f: AccountFilters): Observable<Page<AdminAccountSummary>> {
    let params = new HttpParams().set('page', f.page).set('size', f.size);
    for (const key of ['q', 'status', 'role', 'method', 'sort'] as const) {
      const v = f[key];
      if (v) {
        params = params.set(key, v);
      }
    }
    return this.http.get<Page<AdminAccountSummary>>('/api/v1/admin/accounts', { params });
  }

  get(id: string): Observable<AdminAccountDetail> {
    return this.http.get<AdminAccountDetail>(`/api/v1/admin/accounts/${id}`);
  }

  disable(id: string, reason?: string): Observable<void> {
    return this.http.post<void>(`/api/v1/admin/accounts/${id}/disable`, reason ? { reason } : {});
  }

  enable(id: string): Observable<void> {
    return this.http.post<void>(`/api/v1/admin/accounts/${id}/enable`, {});
  }

  changeRole(id: string, role: 'USER' | 'ADMIN', version: number): Observable<unknown> {
    return this.http.put(`/api/v1/admin/accounts/${id}/role`, { role, version });
  }

  anonymize(id: string, confirmEmail: string): Observable<void> {
    return this.http.post<void>(`/api/v1/admin/accounts/${id}/anonymize`, { confirm_email: confirmEmail });
  }
}
