import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface SetupAdminRequest {
  bootstrap_token: string;
  email: string;
  password: string;
  locale: string;
}

@Injectable({ providedIn: 'root' })
export class SetupService {
  private readonly http = inject(HttpClient);

  create(body: SetupAdminRequest): Observable<{ account_id: string; email: string }> {
    return this.http.post<{ account_id: string; email: string }>('/setup/admin', body);
  }
}
