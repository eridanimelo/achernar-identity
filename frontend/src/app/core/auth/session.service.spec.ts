import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AccountView } from './auth.models';
import { SessionService } from './session.service';
import { TokenStore } from './token-store';

const ACCOUNT: AccountView = {
  id: 'a1', email: 'ana@exemplo.com', email_verified: true, status: 'ACTIVE', role: 'ADMIN', locale: 'pt-BR',
  has_password: true, social_links: [], created_at: '2026-01-01T00:00:00Z',
};
const TOKEN = { token_type: 'Bearer' as const, access_token: 'jwt', expires_in: 900, session_id: 's1' };
const tick = () => new Promise((r) => setTimeout(r));

describe('SessionService', () => {
  let session: SessionService;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    session = TestBed.inject(SessionService);
    ctrl = TestBed.inject(HttpTestingController);
  });
  afterEach(() => ctrl.verify());

  it('init: refresh silencioso bem-sucedido autentica e carrega a conta', async () => {
    const p = session.init();
    await tick();
    ctrl.expectOne('/auth/refresh').flush(TOKEN);
    await tick();
    ctrl.expectOne('/api/v1/account').flush(ACCOUNT);
    await p;
    expect(session.isAuthenticated()).toBe(true);
    expect(session.isAdmin()).toBe(true);
    expect(TestBed.inject(TokenStore).token()).toBe('jwt');
  });

  it('init: refresh recusado deixa a sessão anônima', async () => {
    const p = session.init();
    await tick();
    ctrl.expectOne('/auth/refresh').flush({ code: 'INVALID_REFRESH_TOKEN' }, { status: 401, statusText: 'x' });
    await p;
    expect(session.state()).toBe('anonymous');
    expect(TestBed.inject(TokenStore).hasToken()).toBe(false);
  });

  it('refresh distingue negado de indisponível', async () => {
    let p = session.refresh();
    await tick();
    ctrl.expectOne('/auth/refresh').flush({}, { status: 503, statusText: 'x' });
    expect(await p).toBe('unavailable');
    p = session.refresh();
    await tick();
    ctrl.expectOne('/auth/refresh').flush({}, { status: 401, statusText: 'x' });
    expect(await p).toBe('denied');
  });

  it('refresh simultâneos viram uma única chamada HTTP', async () => {
    const a = session.refresh();
    const b = session.refresh();
    await tick();
    ctrl.expectOne('/auth/refresh').flush(TOKEN);
    expect(await Promise.all([a, b])).toEqual(['ok', 'ok']);
  });

  it('logout limpa o estado local mesmo se a rede falhar e navega ao login', async () => {
    const nav = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    TestBed.inject(TokenStore).set('jwt', 900);
    session.account.set(ACCOUNT);
    const p = session.logout();
    await tick();
    ctrl.expectOne('/auth/logout').error(new ProgressEvent('error'));
    await p;
    expect(session.state()).toBe('anonymous');
    expect(session.account()).toBeNull();
    expect(TestBed.inject(TokenStore).hasToken()).toBe(false);
    expect(nav).toHaveBeenCalledWith('/login');
  });

  it('expire só age quando autenticado e leva a /session-expired', () => {
    const nav = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    session.expire();
    expect(nav).not.toHaveBeenCalled();
    session.state.set('authenticated');
    session.expire();
    expect(nav).toHaveBeenCalledWith('/session-expired');
    expect(session.state()).toBe('anonymous');
  });
});
