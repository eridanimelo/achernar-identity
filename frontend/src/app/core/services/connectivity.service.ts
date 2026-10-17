import { Injectable, NgZone, inject, signal } from '@angular/core';

/** Estado de rede e do serviço (SCR-31): banner offline e banner de indisponibilidade (503). */
@Injectable({ providedIn: 'root' })
export class ConnectivityService {
  private readonly zone = inject(NgZone);
  readonly online = signal(typeof navigator === 'undefined' ? true : navigator.onLine);
  readonly serviceUnavailable = signal(false);

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => this.zone.run(() => this.online.set(true)));
      window.addEventListener('offline', () => this.zone.run(() => this.online.set(false)));
    }
  }

  markUnavailable(): void {
    this.serviceUnavailable.set(true);
  }

  markAvailable(): void {
    if (this.serviceUnavailable()) {
      this.serviceUnavailable.set(false);
    }
  }
}
