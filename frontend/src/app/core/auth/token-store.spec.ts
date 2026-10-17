import { TestBed } from '@angular/core/testing';
import { TokenStore } from './token-store';

describe('TokenStore', () => {
  let store: TokenStore;
  beforeEach(() => {
    store = TestBed.inject(TokenStore);
  });

  it('começa sem token', () => {
    expect(store.token()).toBeNull();
    expect(store.hasToken()).toBe(false);
    expect(store.expiresSoon()).toBe(false);
  });

  it('guarda o token apenas em memória (nenhum storage é tocado)', () => {
    localStorage.clear();
    sessionStorage.clear();
    store.set('abc', 900);
    expect(store.token()).toBe('abc');
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    expect(document.cookie).toBe('');
  });

  it('detecta expiração iminente com a folga informada', () => {
    store.set('abc', 60, 1_000_000);
    expect(store.expiresSoon(15_000, 1_000_000)).toBe(false);
    expect(store.expiresSoon(15_000, 1_000_000 + 46_000)).toBe(true);
    expect(store.expiresSoon(15_000, 1_000_000 + 70_000)).toBe(true);
  });

  it('clear remove o token', () => {
    store.set('abc', 60);
    store.clear();
    expect(store.hasToken()).toBe(false);
  });
});
