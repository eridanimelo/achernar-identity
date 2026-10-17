import { HttpContextToken, HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, defer, finalize, from, switchMap, tap, throwError } from 'rxjs';
import { RuntimeConfigService } from '../config/runtime-config';
import { ApiError, toApiError } from '../http/api-error';
import { ConnectivityService } from '../services/connectivity.service';
import { LoadingService } from '../services/loading.service';
import { LocaleService } from '../services/locale.service';
import { ReauthService } from './reauth.service';
import { SessionService } from './session.service';
import { TokenStore } from './token-store';

/** Marca requisições que não devem disparar renovação/reautenticação automática. */
export const SKIP_AUTH_RETRY = new HttpContextToken<boolean>(() => false);

interface CallContext {
  cfg: RuntimeConfigService;
  session: SessionService;
  store: TokenStore;
  reauth: ReauthService;
  loading: LoadingService;
  conn: ConnectivityService;
  locale: LocaleService;
  next: HttpHandlerFn;
  isApi: boolean;
  isAuthArea: boolean;
  skip: boolean;
}

interface RetryState {
  refreshed: boolean;
  reauthed: boolean;
}

function prepare(c: CallContext, r: HttpRequest<unknown>): HttpRequest<unknown> {
  const headers: Record<string, string> = { 'Accept-Language': c.locale.lang() };
  if (r.url.startsWith('/auth/')) {
    headers['X-Achernar-Client'] = c.cfg.clientId;
  }
  const token = c.store.token();
  if (c.isApi && token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const crossOrigin = c.cfg.config().apiBaseUrl !== '';
  return r.clone({ url: c.cfg.url(r.url), setHeaders: headers, withCredentials: c.isAuthArea && crossOrigin });
}

/** Tenta renovar o token (401) ou reautenticar (REAUTH_REQUIRED) e repete a chamada no máximo uma vez. */
function recover(c: CallContext, r: HttpRequest<unknown>, err: ApiError, state: RetryState): Observable<HttpEvent<unknown>> | null {
  if (!c.isApi || c.skip) {
    return null;
  }
  if (err.status === 401 && !state.refreshed) {
    return from(c.session.refresh()).pipe(
      switchMap((result) => {
        if (result === 'ok') {
          return attempt(c, r, { ...state, refreshed: true });
        }
        if (result === 'denied') {
          c.session.expire();
        }
        return throwError(() => err);
      }),
    );
  }
  if (err.code === 'REAUTH_REQUIRED' && !state.reauthed) {
    return from(c.reauth.request()).pipe(
      switchMap((ok) => (ok ? attempt(c, r, { ...state, reauthed: true }) : throwError(() => err))),
    );
  }
  return null;
}

function attempt(c: CallContext, r: HttpRequest<unknown>, state: RetryState): Observable<HttpEvent<unknown>> {
  return defer(() => from(c.isApi ? c.session.ensureFresh() : Promise.resolve())).pipe(
    switchMap(() => c.next(prepare(c, r))),
    tap(() => c.conn.markAvailable()),
    catchError((e) => {
      const err = toApiError(e);
      if (err.status === 503 || err.status === 0) {
        c.conn.markUnavailable();
      }
      return recover(c, r, err, state) ?? throwError(() => err);
    }),
  );
}

/**
 * Interceptor único da API do Identity:
 *  - resolve caminhos relativos contra a base configurada e injeta Accept-Language e X-Achernar-Client;
 *  - anexa o Bearer nas rotas /api/;
 *  - 401 ⇒ uma tentativa de refresh e repetição; falha ⇒ sessão expirada;
 *  - REAUTH_REQUIRED ⇒ diálogo de reautenticação e repetição única;
 *  - alimenta a barra de progresso global e o estado de disponibilidade.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/')) {
    return next(req); // ativos estáticos (i18n, config)
  }
  const c: CallContext = {
    cfg: inject(RuntimeConfigService),
    session: inject(SessionService),
    store: inject(TokenStore),
    reauth: inject(ReauthService),
    loading: inject(LoadingService),
    conn: inject(ConnectivityService),
    locale: inject(LocaleService),
    next,
    isApi: req.url.startsWith('/api/'),
    isAuthArea: req.url.startsWith('/auth/') || req.url.startsWith('/setup/'),
    skip: req.context.get(SKIP_AUTH_RETRY),
  };
  c.loading.begin();
  return attempt(c, req, { refreshed: false, reauthed: false }).pipe(finalize(() => c.loading.end()));
};
