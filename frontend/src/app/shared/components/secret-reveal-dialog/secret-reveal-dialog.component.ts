import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';

export interface SecretRevealData {
  title: string;
  secret: string;
  note?: string;
}

/** SCR-26: exibição única do segredo; só fecha após a confirmação "Guardei em local seguro". */
@Component({
  selector: 'app-secret-reveal-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatCheckboxModule, MatIconModule, TranslocoPipe],
  templateUrl: './secret-reveal-dialog.component.html',
  styleUrl: './secret-reveal-dialog.component.scss',
})
export class SecretRevealDialogComponent {
  readonly data = inject<SecretRevealData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<SecretRevealDialogComponent>);
  readonly saved = signal(false);
  readonly copied = signal(false);

  constructor() {
    this.ref.disableClose = true;
  }

  async copy(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.data.secret);
      this.copied.set(true);
    } catch {
      this.copied.set(false);
    }
  }

  close(): void {
    if (this.saved()) {
      this.ref.close();
    }
  }
}
