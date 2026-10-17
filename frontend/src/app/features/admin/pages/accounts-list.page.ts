import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { Page } from '../../../core/models/api.types';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { AdminAccountSummary } from '../models/admin.models';
import { AdminAccountsService } from '../services/admin-accounts.service';
import { accountStatusTone } from '../components/admin-tones';

/** SCR-18: lista paginada de contas com filtros. */
@Component({
  selector: 'app-accounts-list-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatTableModule,
    MatPaginatorModule, TranslocoPipe, LocalDatePipe, PageHeaderComponent, SkeletonComponent, EmptyStateComponent,
    FormAlertComponent, StatusChipComponent,
  ],
  templateUrl: './accounts-list.page.html',
})
export class AccountsListPage implements OnInit {
  private readonly api = inject(AdminAccountsService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly errors = inject(ErrorMessageService);

  readonly columns = ['email', 'status', 'role', 'methods', 'created'];
  readonly data = signal<Page<AdminAccountSummary> | null>(null);
  readonly loading = signal(true);
  readonly failure = signal<ApiError | null>(null);
  readonly message = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });
  readonly pageIndex = signal(0);
  readonly pageSize = 25;
  readonly tone = accountStatusTone;

  readonly filters = this.fb.nonNullable.group({ q: [''], status: [''], role: [''], method: [''] });

  ngOnInit(): void {
    void this.load();
  }

  apply(): void {
    this.pageIndex.set(0);
    void this.load();
  }

  clear(): void {
    this.filters.reset();
    this.apply();
  }

  paged(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    void this.load();
  }

  open(row: AdminAccountSummary): void {
    void this.router.navigate(['/admin/accounts', row.id]);
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      const f = this.filters.getRawValue();
      this.data.set(await firstValueFrom(this.api.list({ ...f, q: f.q.trim(), page: this.pageIndex(), size: this.pageSize })));
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }
}
