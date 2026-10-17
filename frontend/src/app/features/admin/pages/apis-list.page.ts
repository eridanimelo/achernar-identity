import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
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
import { registryStatusTone } from '../components/admin-tones';
import { ApiSummary } from '../models/admin.models';
import { AdminApisService } from '../services/admin-apis.service';

/** SCR-27: lista de APIs. */
@Component({
  selector: 'app-apis-list-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatPaginatorModule, MatTableModule,
    TranslocoPipe, PageHeaderComponent, SkeletonComponent, EmptyStateComponent, FormAlertComponent, StatusChipComponent,
  ],
  templateUrl: './apis-list.page.html',
})
export class ApisListPage implements OnInit {
  private readonly api = inject(AdminApisService);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ErrorMessageService);

  readonly columns = ['name', 'audience', 'status'];
  readonly data = signal<Page<ApiSummary> | null>(null);
  readonly loading = signal(true);
  readonly failure = signal<ApiError | null>(null);
  readonly pageIndex = signal(0);
  readonly message = computed(() => {
    const f = this.failure();
    return f ? this.errors.message(f) : '';
  });
  readonly tone = registryStatusTone;
  readonly search = this.fb.nonNullable.control('');

  ngOnInit(): void {
    void this.load();
  }

  apply(): void {
    this.pageIndex.set(0);
    void this.load();
  }

  paged(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.failure.set(null);
    try {
      this.data.set(await firstValueFrom(this.api.list(this.search.value.trim(), this.pageIndex())));
    } catch (e) {
      this.failure.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }
}
