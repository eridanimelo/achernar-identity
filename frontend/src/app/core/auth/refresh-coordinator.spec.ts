import { TestBed } from '@angular/core/testing';
import { RefreshCoordinator } from './refresh-coordinator';

describe('RefreshCoordinator', () => {
  let coordinator: RefreshCoordinator;
  beforeEach(() => {
    coordinator = TestBed.inject(RefreshCoordinator);
  });

  it('executa uma só tarefa em voo (single-flight) para chamadas simultâneas', async () => {
    let calls = 0;
    let release!: (v: string) => void;
    const gate = new Promise<string>((r) => (release = r));
    const task = () => {
      calls++;
      return gate;
    };
    const a = coordinator.run(task);
    const b = coordinator.run(task);
    const c = coordinator.run(task);
    release('ok');
    expect(await Promise.all([a, b, c])).toEqual(['ok', 'ok', 'ok']);
    expect(calls).toBe(1);
  });

  it('permite nova execução depois que a anterior termina', async () => {
    let calls = 0;
    await coordinator.run(async () => ++calls);
    await coordinator.run(async () => ++calls);
    expect(calls).toBe(2);
  });

  it('libera o voo mesmo quando a tarefa falha', async () => {
    await expect(coordinator.run(() => Promise.reject(new Error('x')))).rejects.toThrow('x');
    await expect(coordinator.run(async () => 'depois')).resolves.toBe('depois');
  });
});
