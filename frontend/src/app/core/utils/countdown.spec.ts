import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { createCountdown } from './countdown';

@Component({ selector: 'app-host', template: '' })
class HostComponent {
  readonly c = createCountdown();
}

describe('createCountdown', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('conta regressivamente até zero e para', () => {
    const f = TestBed.createComponent(HostComponent);
    f.componentInstance.c.start(3);
    expect(f.componentInstance.c.remaining()).toBe(3);
    vi.advanceTimersByTime(2000);
    expect(f.componentInstance.c.remaining()).toBe(1);
    vi.advanceTimersByTime(5000);
    expect(f.componentInstance.c.remaining()).toBe(0);
  });

  it('limpa o temporizador ao destruir o componente', () => {
    const f = TestBed.createComponent(HostComponent);
    f.componentInstance.c.start(10);
    f.destroy();
    vi.advanceTimersByTime(5000);
    expect(f.componentInstance.c.remaining()).toBe(10);
  });
});
