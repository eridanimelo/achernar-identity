import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';
import { FormControl, FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslocoPipe } from '@jsverse/transloco';

/** Lista de valores (origens/redirects) com validação por função e limite. */
@Component({
  selector: 'app-chips-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatChipsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, TranslocoPipe],
  templateUrl: './chips-input.component.html',
  styleUrl: './chips-input.component.scss',
})
export class ChipsInputComponent {
  readonly label = input.required<string>();
  readonly name = input.required<string>();
  readonly control = input.required<FormControl<string[]>>();
  readonly values = signal<string[]>([]);
  readonly validate = input<(v: string) => boolean>(() => true);
  readonly max = input(20);
  readonly invalidKey = input('validation.invalid');
  readonly placeholder = input('');

  constructor() {
    effect((onCleanup) => {
      const c = this.control();
      this.values.set(c.value);
      const sub = c.valueChanges.subscribe((v) => this.values.set(v));
      onCleanup(() => sub.unsubscribe());
    });
  }

  readonly draft = signal('');
  readonly error = signal<string | null>(null);
  readonly full = computed(() => this.values().length >= this.max());

  add(): void {
    const v = this.draft().trim();
    if (!v) {
      return;
    }
    if (!this.validate()(v)) {
      this.error.set(this.invalidKey());
      return;
    }
    if (this.values().includes(v)) {
      this.draft.set('');
      return;
    }
    if (this.full()) {
      this.error.set('validation.tooMany');
      return;
    }
    this.error.set(null);
    this.control().setValue([...this.values(), v]);
    this.control().markAsDirty();
    this.draft.set('');
  }

  remove(v: string): void {
    this.control().setValue(this.values().filter((x) => x !== v));
    this.control().markAsDirty();
  }
}
