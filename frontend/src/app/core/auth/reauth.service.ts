import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';

/** Orquestra o diálogo de reautenticação (SCR-11) pedido por `REAUTH_REQUIRED`. */
@Injectable({ providedIn: 'root' })
export class ReauthService {
  private readonly dialog = inject(MatDialog);
  private pending: Promise<boolean> | null = null;

  /** Abre o diálogo (um só por vez). Resolve `true` se o usuário reautenticou. */
  request(): Promise<boolean> {
    if (this.pending) {
      return this.pending;
    }
    const p = import('./reauth-dialog/reauth-dialog.component').then(async ({ ReauthDialogComponent }) => {
      const ref = this.dialog.open<unknown, unknown, boolean>(ReauthDialogComponent, {
        width: '440px',
        maxWidth: '100vw',
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        ariaLabelledBy: 'reauth-title',
      });
      return (await ref.afterClosed().toPromise()) === true;
    });
    this.pending = p.finally(() => {
      this.pending = null;
    });
    return this.pending;
  }
}
