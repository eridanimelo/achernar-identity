import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../../../core/models/api.types';
import { ApplicationDetail, ApplicationPayload, ApplicationSummary } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminApplicationsService {
  private readonly http = inject(HttpClient);

  list(q: string, page: number, size = 25): Observable<Page<ApplicationSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<Page<ApplicationSummary>>('/api/v1/admin/applications', { params });
  }

  get(id: string): Observable<ApplicationDetail> {
    return this.http.get<ApplicationDetail>(`/api/v1/admin/applications/${id}`);
  }

  create(body: ApplicationPayload): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>('/api/v1/admin/applications', body);
  }

  update(id: string, body: Partial<ApplicationPayload> & { version: number }): Observable<ApplicationDetail> {
    return this.http.patch<ApplicationDetail>(`/api/v1/admin/applications/${id}`, body);
  }

  createSecret(id: string): Observable<{ secret_id: string; client_secret: string; created_at: string }> {
    return this.http.post<{ secret_id: string; client_secret: string; created_at: string }>(`/api/v1/admin/applications/${id}/secrets`, {});
  }

  revokeSecret(id: string, secretId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/admin/applications/${id}/secrets/${secretId}`);
  }
}
