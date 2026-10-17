import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { SigningKey } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminKeysService {
  private readonly http = inject(HttpClient);

  list(): Observable<SigningKey[]> {
    return this.http.get<{ items: SigningKey[] }>('/api/v1/admin/signing-keys').pipe(map((r) => r.items));
  }

  rotate(): Observable<SigningKey> {
    return this.http.post<SigningKey>('/api/v1/admin/signing-keys/rotate', {});
  }

  remove(kid: string, body: { emergency: boolean; revoke_all_sessions: boolean; reason?: string }): Observable<void> {
    return this.http.post<void>(`/api/v1/admin/signing-keys/${encodeURIComponent(kid)}/remove`, body);
  }
}
