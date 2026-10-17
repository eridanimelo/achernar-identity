import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoPipe } from '@jsverse/transloco';
import { ApiError } from '../../../core/http/api-error';
import { matchValidator, passwordLengthValidator } from '../../../core/utils/validators';
import { PasswordFieldComponent } from '../../../shared/components/password-field/password-field.component';
import { FocusInvalidDirective } from '../../../shared/directives/focus-invalid.directive';
import { applyServerErrors } from '../../../shared/utils/server-errors';

export interface PasswordSubmit {
  current?: string;
  next: string;
}

/** SCR-10 (alterar senha) e SCR-13 (definir senha local, sem senha atual). Apresentacional. */
@Component({
  selector: 'app-password-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatProgressSpinnerModule, TranslocoPipe, PasswordFieldComponent, FocusInvalidDirective],
  template: `
    <section class="section" aria-labelledby="pw-title">
      <h2 id="pw-title">{{ (hasPassword() ? 'security.password.title' : 'security.password.setTitle') | transloco }}</h2>
      @if (!hasPassword()) {
        <p class="muted">{{ 'security.password.setHint' | transloco }}</p>
      }
      <form class="form-column" [formGroup]="form" (ngSubmit)="submit()" appFocusInvalid novalidate>
        @if (hasPassword()) {
          <app-password-field name="current" [control]="form.controls.current" [label]="'security.password.current' | transloco" autocomplete="current-password" />
        }
        <app-password-field name="password" [control]="form.controls.password" [label]="'auth.newPassword' | transloco" [hint]="'password.hint' | transloco" autocomplete="new-password" [showMeter]="true" />
        <app-password-field name="confirm" [control]="form.controls.confirm" [label]="'auth.confirmPassword' | transloco" autocomplete="new-password" />
        @if (form.errors?.['mismatch'] && form.controls.confirm.touched) {
          <p class="err" role="alert">{{ 'validation.mismatch' | transloco }}</p>
        }
        <div class="actions">
          <button mat-flat-button type="submit" [disabled]="busy()" [attr.aria-busy]="busy()">
            @if (busy()) { <mat-spinner diameter="18" /> }
            {{ (hasPassword() ? 'security.password.submit' : 'security.password.setSubmit') | transloco }}
          </button>
        </div>
      </form>
    </section>
  `,
  styles: `
    .err {
      color: var(--ach-error);
      margin: 0 0 8px;
    }
  `,
})
export class PasswordSectionComponent {
  private readonly fb = inject(FormBuilder);
  readonly hasPassword = input.required<boolean>();
  readonly busy = input(false);
  readonly failure = input<ApiError | null>(null);
  readonly submitted = output<PasswordSubmit>();

  readonly form = this.fb.nonNullable.group(
    {
      current: [''],
      password: ['', [Validators.required, passwordLengthValidator()]],
      confirm: ['', [Validators.required]],
    },
    { validators: [matchValidator('password', 'confirm')] },
  );

  constructor() {
    effect(() => {
      const needs = this.hasPassword();
      const c = this.form.controls.current;
      c.setValidators(needs ? [Validators.required] : []);
      c.updateValueAndValidity({ emitEvent: false });
    });
    effect(() => {
      const f = this.failure();
      if (f) {
        applyServerErrors(this.form, f, { new_password: 'password', current_password: 'current' });
      }
    });
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy()) {
      return;
    }
    const v = this.form.getRawValue();
    if (this.hasPassword() && v.current === v.password) {
      this.form.controls.password.setErrors({ different: true });
      return;
    }
    this.submitted.emit({ current: this.hasPassword() ? v.current : undefined, next: v.password });
  }

  reset(): void {
    this.form.reset();
  }
}
