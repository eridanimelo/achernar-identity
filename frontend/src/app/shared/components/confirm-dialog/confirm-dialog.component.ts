import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';

export interface ConfirmData {
  title: string;
  message: string;
  confirmLabel: string;
  destructive?: boolean;
  /** Se informado, o usuário precisa digitar exatamente este texto (ex.: e-mail, kid). */
  typeToConfirm?: string;
  typeLabel?: string;
  /** Campo de motivo opcional (≤ 200). */
  reason?: boolean;
  /** Caixa de seleção extra (ex.: "revogar todas as sessões"). */
  option?: { label: string };
}

export interface ConfirmResult {
  confirmed: boolean;
  reason?: string;
  option?: boolean;
}

/** Confirmação destrutiva: título, consequência explícita e botão distinto por ícone/texto (ui-map §3). */
@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatIconModule, TranslocoPipe],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ConfirmDialogComponent, ConfirmResult>);
  readonly typed = signal('');
  readonly reason = signal('');
  readonly option = signal(false);

  canConfirm(): boolean {
    return !this.data.typeToConfirm || this.typed() === this.data.typeToConfirm;
  }

  confirm(): void {
    if (this.canConfirm()) {
      this.ref.close({ confirmed: true, reason: this.reason().trim() || undefined, option: this.option() });
    }
  }

  cancel(): void {
    this.ref.close({ confirmed: false });
  }
}
