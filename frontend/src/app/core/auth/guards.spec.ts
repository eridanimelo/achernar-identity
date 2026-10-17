import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { adminGuard, authGuard, guestGuard, SetupStatusService } from './guards';
import { SessionService } from './session.service';

describe('guards', () => {
  const authenticated = signal(false);
  const admin = signal(false);
  const setupRequired = vi.fn(async () => false);
  const route = {} as ActivatedRouteSnapshot;
  const state = (url: string) => ({ url }) as RouterStateSnapshot;

  const run = (guard: typeof authGuard, url = '/account/security') =>
    TestBed.runInInjectionContext(() => guard(route, state(url)));

  beforeEach(() => {
    authenticated.set(false);
    admin.set(false);
    setupRequired.mockResolvedValue(false);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: { isAuthenticated: authenticated, isAdmin: admin } },
        { provide: SetupStatusService, useValue: { setupRequired } },
      ],
    });
  });

  const asString = (r: unknown) => TestBed.inject(Router).serializeUrl(r as UrlTree);

  it('authGuard: anônimo vai para /login?next=…', () => {
    expect(asString(run(authGuard))).toBe('/login?next=%2Faccount%2Fsecurity');
    authenticated.set(true);
    expect(run(authGuard)).toBe(true);
  });

  it('guestGuard: logado vai para /account; setup pendente vai para /setup', async () => {
    expect(await run(guestGuard)).toBe(true);
    setupRequired.mockResolvedValue(true);
    expect(asString(await run(guestGuard))).toBe('/setup');
    authenticated.set(true);
    expect(asString(await run(guestGuard))).toBe('/account');
  });

  it('adminGuard: USER recebe /403; ADMIN passa; anônimo vai ao login', () => {
    expect(asString(run(adminGuard, '/admin/keys'))).toBe('/login?next=%2Fadmin%2Fkeys');
    authenticated.set(true);
    expect(asString(run(adminGuard))).toBe('/403');
    admin.set(true);
    expect(run(adminGuard)).toBe(true);
  });
});
