import { Injectable, computed, signal } from '@angular/core';

/** Barra de progresso global: aparece apenas se a requisição passar de 300 ms (SCR-31). */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  static readonly DELAY_MS = 300;
  private readonly pending = signal(0);
  private readonly shown = signal(false);
  private timer: ReturnType<typeof setTimeout> | null = null;

  readonly visible = computed(() => this.shown() && this.pending() > 0);

  begin(): void {
    this.pending.update((n) => n + 1);
    if (this.pending() === 1 && this.timer === null) {
      this.timer = setTimeout(() => {
        this.timer = null;
        if (this.pending() > 0) {
          this.shown.set(true);
        }
      }, LoadingService.DELAY_MS);
    }
  }

  end(): void {
    this.pending.update((n) => Math.max(0, n - 1));
    if (this.pending() === 0) {
      if (this.timer !== null) {
        clearTimeout(this.timer);
        this.timer = null;
      }
      this.shown.set(false);
    }
  }
}
