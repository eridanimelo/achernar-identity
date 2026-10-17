import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EventFilters, SecurityEventItem } from '../models/admin.models';

export interface EventsPage {
  items: SecurityEventItem[];
  next_cursor?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminEventsService {
  private readonly http = inject(HttpClient);

  list(f: EventFilters): Observable<EventsPage> {
    let params = new HttpParams();
    for (const key of ['from', 'to', 'account', 'client_id', 'outcome', 'severity', 'cursor'] as const) {
      const v = f[key];
      if (v) {
        params = params.set(key, v);
      }
    }
    for (const t of f.type ?? []) {
      params = params.append('type', t);
    }
    params = params.set('limit', f.limit ?? 50);
    return this.http.get<EventsPage>('/api/v1/admin/security-events', { params });
  }
}
