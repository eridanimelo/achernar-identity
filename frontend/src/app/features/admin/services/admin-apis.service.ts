import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../../../core/models/api.types';
import { ApiSummary } from '../models/admin.models';

export interface ApiCreated extends ApiSummary {
  credential: { credential_id: string; secret: string };
}

@Injectable({ providedIn: 'root' })
export class AdminApisService {
  private readonly http = inject(HttpClient);

  list(q: string, page: number, size = 25): Observable<Page<ApiSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<Page<ApiSummary>>('/api/v1/admin/apis', { params });
  }

  get(id: string): Observable<ApiSummary> {
    return this.http.get<ApiSummary>(`/api/v1/admin/apis/${id}`);
  }

  create(body: { audience: string; name: string; description: string | null }): Observable<ApiCreated> {
    return this.http.post<ApiCreated>('/api/v1/admin/apis', body);
  }

  update(id: string, body: { name?: string; description?: string | null; status?: string; version: number }): Observable<ApiSummary> {
    return this.http.patch<ApiSummary>(`/api/v1/admin/apis/${id}`, body);
  }

  createCredential(id: string): Observable<{ credential_id: string; secret: string; created_at: string }> {
    return this.http.post<{ credential_id: string; secret: string; created_at: string }>(`/api/v1/admin/apis/${id}/credentials`, {});
  }

  revokeCredential(id: string, credentialId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/admin/apis/${id}/credentials/${credentialId}`);
  }
}
