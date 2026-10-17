import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslocoPipe } from '@jsverse/transloco';
import { firstError } from '../../utils/form-errors';
import { passwordStrength } from '../../../core/utils/validators';

/** Campo de senha: mostrar/ocultar, colagem permitida, medidor opcional (apenas dica; a política é do servidor). */
@Component({
  selector: 'app-password-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule, TranslocoPipe],
  templateUrl: './password-field.component.html',
  styleUrl: './password-field.component.scss',
})
export class PasswordFieldComponent {
  readonly control = input.required<FormControl<string>>();
  readonly label = input.required<string>();
  readonly name = input.required<string>();
  readonly autocomplete = input<'current-password' | 'new-password'>('current-password');
  readonly showToggle = input(true);
  readonly showMeter = input(false);
  readonly hint = input<string | null>(null);

  readonly visible = signal(false);
  readonly inputType = computed(() => (this.visible() ? 'text' : 'password'));
  /** Atualizado a cada digitação pelo template. */
  readonly strength = signal<0 | 1 | 2 | 3>(0);

  toggle(): void {
    this.visible.update((v) => !v);
  }

  onInput(value: string): void {
    this.strength.set(passwordStrength(value));
  }

  error() {
    return firstError(this.control());
  }
}
