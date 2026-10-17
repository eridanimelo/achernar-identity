import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { LocaleService } from '../services/locale.service';
import { ReauthService } from './reauth.service';
import { SessionService } from './session.service';
import { TokenStore } from './token-store';
import { apiInterceptor } from './api.interceptor';

describe('apiInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let store: TokenStore;
  const session = {
    refresh: vi.fn<() => Promise<'ok' | 'denied' | 'unavailable'>>(),
    ensureFresh: vi.fn(async () => undefined),
    expire: vi.fn(),
  };
  const reauth = { request: vi.fn<() => Promise<boolean>>() };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
        { provide: SessionService, useValue: session },
        { provide: ReauthService, useValue: reauth },
        { provide: LocaleService, useValue: { lang: () => 'pt-BR' } },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    store = TestBed.inject(TokenStore);
    store.set('tok-1', 900);
  });
  afterEach(() => ctrl.verify());

  it('anexa Bearer e Accept-Language em /api/ e não envia X-Achernar-Client', async () => {
    const p = firstValueFrom(http.get('/api/v1/account'));
    await Promise.resolve();
    const req = ctrl.expectOne('/api/v1/account');
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok-1');
    expect(req.request.headers.get('Accept-Language')).toBeTruthy();
    expect(req.request.headers.has('X-Achernar-Client')).toBe(false);
    req.flush({});
    await p;
  });

  it('envia X-Achernar-Client em /auth/ e nunca Bearer', async () => {
    const p = firstValueFrom(http.post('/auth/login', {}));
    await Promise.resolve();
    const req = ctrl.expectOne('/auth/login');
    expect(req.request.headers.get('X-Achernar-Client')).toBe('achernar-identity-ui');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
    await p;
  });

  it('não toca em ativos estáticos (URL relativa)', async () => {
    const p = firstValueFrom(http.get('i18n/pt-BR.json'));
    const req = ctrl.expectOne('i18n/pt-BR.json');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
    await p;
  });

  it('401 ⇒ um refresh e uma repetição com o novo token', async () => {
    session.refresh.mockImplementation(async () => {
      store.set('tok-2', 900);
      return 'ok';
    });
    const p = firstValueFrom(http.get('/api/v1/account'));
    await Promise.resolve();
    ctrl.expectOne('/api/v1/account').flush({ code: 'UNAUTHENTICATED' }, { status: 401, statusText: 'Unauthorized' });
    await vi.waitFor(() => expect(session.refresh).toHaveBeenCalledTimes(1));
    await vi.waitFor(() => {
      const retry = ctrl.expectOne('/api/v1/account');
      expect(retry.request.headers.get('Authorization')).toBe('Bearer tok-2');
      retry.flush({ ok: true });
    });
    expect(await p).toEqual({ ok: true });
  });

  it('401 repetido depois do refresh não entra em laço', async () => {
    session.refresh.mockResolvedValue('ok');
    const p = firstValueFrom(http.get('/api/v1/account')).catch((e) => e);
    await Promise.resolve();
    ctrl.expectOne('/api/v1/account').flush({}, { status: 401, statusText: 'x' });
    await vi.waitFor(() => ctrl.expectOne('/api/v1/account').flush({}, { status: 401, statusText: 'x' }));
    const err = await p;
    expect(err.status).toBe(401);
    expect(session.refresh).toHaveBeenCalledTimes(1);
  });

  it('refresh negado ⇒ expira a sessão e propaga o erro', async () => {
    session.refresh.mockResolvedValue('denied');
    const p = firstValueFrom(http.get('/api/v1/account')).catch((e) => e);
    await Promise.resolve();
    ctrl.expectOne('/api/v1/account').flush({}, { status: 401, statusText: 'x' });
    const err = await p;
    expect(err.code).toBe('UNAUTHENTICATED');
    expect(session.expire).toHaveBeenCalledTimes(1);
  });

  it('REAUTH_REQUIRED ⇒ pede reautenticação e repete uma vez', async () => {
    reauth.request.mockResolvedValue(true);
    const p = firstValueFrom(http.delete('/api/v1/account/sessions/1'));
    await Promise.resolve();
    ctrl.expectOne('/api/v1/account/sessions/1').flush({ code: 'REAUTH_REQUIRED' }, { status: 403, statusText: 'Forbidden' });
    await vi.waitFor(() => ctrl.expectOne('/api/v1/account/sessions/1').flush(null, { status: 204, statusText: 'No Content' }));
    await p;
    expect(reauth.request).toHaveBeenCalledTimes(1);
  });

  it('reautenticação cancelada ⇒ propaga REAUTH_REQUIRED sem repetir', async () => {
    reauth.request.mockResolvedValue(false);
    const p = firstValueFrom(http.delete('/api/v1/account/sessions/1')).catch((e) => e);
    await Promise.resolve();
    ctrl.expectOne('/api/v1/account/sessions/1').flush({ code: 'REAUTH_REQUIRED' }, { status: 403, statusText: 'Forbidden' });
    expect((await p).code).toBe('REAUTH_REQUIRED');
  });

  it('chamadas fora de /api/ não disparam refresh em 401 (ex.: login inválido)', async () => {
    const p = firstValueFrom(http.post('/auth/login', {})).catch((e) => e);
    await Promise.resolve();
    ctrl.expectOne('/auth/login').flush({ code: 'INVALID_CREDENTIALS' }, { status: 401, statusText: 'x' });
    expect((await p).code).toBe('INVALID_CREDENTIALS');
    expect(session.refresh).not.toHaveBeenCalled();
  });
});
