import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { ChipTone, StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { EVENT_TYPES, EventFilters, SecurityEventItem } from '../models/admin.models';
import { AdminEventsService } from '../services/admin-events.service';

const MAX_PERIOD_DAYS = 90;
const DAY_MS = 86_400_000;

/** SCR-21: consulta de eventos de segurança (somente leitura, cursor). */
@Component({
  selector: 'app-events-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, TranslocoPipe, LocalDatePipe,
    PageHeaderComponent, SkeletonComponent, EmptyStateComponent, FormAlertComponent, StatusChipComponent,
  ],
  templateUrl: './events.page.html',
  styleUrl: './events.page.scss',
})
export class EventsPage implements OnInit {
  private readonly api = inject(AdminEventsService);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ErrorMessageService);

  readonly types = EVENT_TYPES;
  readonly severities = ['INFO', 'WARNING', 'CRITICAL'];
  readonly outcomes = ['SUCCESS', 'FAILURE', 'DENIED'];

  readonly form = this.fb.nonNullable.group({
    from: [''],
    to: [''],
    type: [[] as string[]],
    account: [''],
    client_id: [''],
    outcome: [''],
    severity: [''],
  });

  readonly items = signal<SecurityEventItem[]>([]);
  readonly nextCursor = signal<string | undefined>(undefined);
  readonly loading = signal(false);
  readonly loaded = signal(false);
  readonly failure = signal<ApiError | null>(null);
  readonly periodError = signal(false);
  readonly expanded = signal<ReadonlySet<string>>(new Set());
  readonly message = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });

  private applied: EventFilters = {};

  ngOnInit(): void {
    void this.apply();
  }

  tone(severity: string): ChipTone {
    return severity === 'CRITICAL' ? 'error' : severity === 'WARNING' ? 'warning' : 'info';
  }

  isOpen(id: string): boolean {
    return this.expanded().has(id);
  }

  toggle(id: string): void {
    const next = new Set(this.expanded());
    if (!next.delete(id)) {
      next.add(id);
    }
    this.expanded.set(next);
  }

  detailsOf(e: SecurityEventItem): string {
    return JSON.stringify(e.details ?? {}, null, 2);
  }

  async apply(): Promise<void> {
    const v = this.form.getRawValue();
    this.periodError.set(false);
    if (v.from && v.to) {
      const span = new Date(v.to).getTime() - new Date(v.from).getTime();
      if (span < 0 || span > MAX_PERIOD_DAYS * DAY_MS) {
        this.periodError.set(true);
        return;
      }
    }
    this.applied = {
      from: v.from ? new Date(v.from).toISOString() : undefined,
      to: v.to ? new Date(v.to).toISOString() : undefined,
      type: v.type,
      account: v.account.trim() || undefined,
      client_id: v.client_id.trim() || undefined,
      outcome: v.outcome || undefined,
      severity: v.severity || undefined,
    };
    this.items.set([]);
    this.nextCursor.set(undefined);
    this.expanded.set(new Set());
    await this.fetch();
  }

  clear(): void {
    this.form.reset();
    void this.apply();
  }

  async more(): Promise<void> {
    await this.fetch(this.nextCursor());
  }

  private async fetch(cursor?: string): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      const page = await firstValueFrom(this.api.list({ ...this.applied, cursor }));
      this.items.update((cur) => [...cur, ...page.items]);
      this.nextCursor.set(page.next_cursor ?? undefined);
      this.loaded.set(true);
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }
}
