import { Injectable } from '@angular/core';

interface LockManagerLike {
  request<T>(name: string, callback: () => Promise<T>): Promise<T>;
}

/**
 * Serializa a renovação de sessão: uma só chamada em voo por aba (single-flight) e, entre abas,
 * exclusão mútua por Web Locks. O cookie de refresh é compartilhado; a rotação do servidor tolera a
 * reapresentação imediata (P-04), então a trava apenas reduz corridas desnecessárias.
 */
@Injectable({ providedIn: 'root' })
export class RefreshCoordinator {
  static readonly LOCK_NAME = 'achernar-identity-refresh';
  private inflight: Promise<unknown> | null = null;

  run<T>(task: () => Promise<T>): Promise<T> {
    if (this.inflight) {
      return this.inflight as Promise<T>;
    }
    const p = this.withLock(task).finally(() => {
      this.inflight = null;
    });
    this.inflight = p;
    return p;
  }

  private withLock<T>(task: () => Promise<T>): Promise<T> {
    const locks = (globalThis.navigator as { locks?: LockManagerLike } | undefined)?.locks;
    return locks ? locks.request(RefreshCoordinator.LOCK_NAME, task) : task();
  }
}
