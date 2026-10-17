import { DestroyRef, inject, signal } from '@angular/core';

/** Contagem regressiva reativa (bloqueios por Retry-After). Limpa o temporizador ao destruir o componente. */
export function createCountdown() {
  const destroyRef = inject(DestroyRef);
  const remaining = signal(0);
  let timer: ReturnType<typeof setInterval> | null = null;

  const stop = () => {
    if (timer !== null) {
      clearInterval(timer);
      timer = null;
    }
  };

  const start = (seconds: number) => {
    stop();
    remaining.set(Math.max(0, Math.ceil(seconds)));
    if (remaining() === 0) {
      return;
    }
    timer = setInterval(() => {
      remaining.update((n) => Math.max(0, n - 1));
      if (remaining() === 0) {
        stop();
      }
    }, 1000);
  };

  destroyRef.onDestroy(stop);
  return { remaining: remaining.asReadonly(), start, stop };
}
