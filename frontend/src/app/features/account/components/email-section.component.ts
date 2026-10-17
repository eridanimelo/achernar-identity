import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { ApiError } from '../../../core/http/api-error';
import { emailValidator } from '../../../core/utils/validators';
import { TextFieldComponent } from '../../../shared/components/text-field/text-field.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { applyServerErrors } from '../../../shared/utils/server-errors';

/** UC-035: solicitar troca de e-mail (o servidor envia o link ao novo endereço). */
@Component({
  selector: 'app-email-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, TextFieldComponent, FocusInvalidDirective],
  template: `
    <section class="section" aria-labelledby="em-title">
      <h2 id="em-title">{{ 'security.email.title' | transloco }}</h2>
      <p>{{ 'security.email.current' | transloco }}: <strong>{{ currentEmail() }}</strong></p>
      @if (sent()) {
        <p role="status">{{ 'security.email.sent' | transloco }}</p>
      }
      <form class="form-column" [formGroup]="form" (ngSubmit)="submit()" appFocusInvalid novalidate>
        <app-text-field name="new_email" type="email" inputmode="email" autocomplete="email" [maxlength]="254"
          [control]="form.controls.email" [label]="'security.email.new' | transloco" [required]="true" />
        <div class="actions">
          <button mat-stroked-button type="submit" [disabled]="busy()" [attr.aria-busy]="busy()">
            @if (busy()) { <mat-spinner diameter="18" /> }
            {{ 'security.email.submit' | transloco }}
          </button>
        </div>
      </form>
    </section>
  `,
})
export class EmailSectionComponent {
  private readonly fb = inject(FormBuilder);
  readonly currentEmail = input.required<string>();
  readonly busy = input(false);
  readonly sent = input(false);
  readonly failure = input<ApiError | null>(null);
  readonly submitted = output<string>();
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, emailValidator]] });

  constructor() {
    effect(() => {
      const f = this.failure();
      if (f) {
        applyServerErrors(this.form, f, { new_email: 'email' });
      }
    });
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy()) {
      return;
    }
    const value = this.form.controls.email.value.trim();
    if (value.toLowerCase() === this.currentEmail().toLowerCase()) {
      this.form.controls.email.setErrors({ different: true });
      return;
    }
    this.submitted.emit(value);
  }
}
