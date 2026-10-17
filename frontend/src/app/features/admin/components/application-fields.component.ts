import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TranslocoPipe } from '@jsverse/transloco';
import { isValidAbsoluteUrl, isValidOrigin } from '../../../core/utils/validators';
import { ChipsInputComponent } from '../../../shared/components/chips-input/chips-input.component';
import { TextFieldComponent } from '../../../shared/components/text-field/text-field.component';
import { firstError } from '../../../shared/utils/form-errors';
import { ApiSummary } from '../models/admin.models';
import { ApplicationFormGroup } from './application-form';

/** Campos do formulário de aplicação (SCR-25): geral, origens, retornos, URLs de ação, APIs e opções. */
@Component({
  selector: 'app-application-fields',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule,
    TranslocoPipe, TextFieldComponent, ChipsInputComponent,
  ],
  templateUrl: './application-fields.component.html',
  styleUrl: './application-fields.component.scss',
})
export class ApplicationFieldsComponent {
  readonly form = input.required<ApplicationFormGroup>();
  readonly isNew = input(false);
  readonly apis = input<ApiSummary[]>([]);
  readonly addApi = output<string>();
  readonly removeApi = output<number>();

  readonly pendingApi = signal('');
  readonly originOk = isValidOrigin;
  readonly urlOk = (v: string) => isValidAbsoluteUrl(v) && !v.includes('#');
  readonly availableApis = computed(() => {
    const used = new Set(this.form().controls.audiences.controls.map((g) => g.controls.audience.value));
    return this.apis().filter((a) => a.status === 'ACTIVE' && !used.has(a.audience));
  });

  err(name: 'client_id' | 'name' | 'description') {
    return firstError(this.form().controls[name]);
  }

  groupOf(i: number): FormGroup {
    return this.form().controls.audiences.at(i);
  }

  choose(audience: string): void {
    if (audience) {
      this.addApi.emit(audience);
      this.pendingApi.set('');
    }
  }
}
