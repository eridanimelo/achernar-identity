# API Spec — FT-03 Autenticação por senha e tokens

Convenções: `api-conventions.md` (§1.2 resposta de tokens). `NEW` · `APPROVED` (documental).

### API-FT03-01 — Login
`POST /auth/login` · comando · cliente (`client_id` + `Origin` ou Basic).
Entrada: `client_id` (BROWSER), `email` (≤ 254), `password` (1–128), `audiences` (lista opcional ≤ 20 itens, cada ≤ 200).
Sucesso `200`: resposta de tokens (§1.2). BROWSER: `Set-Cookie` de refresh; sem `refresh_token`. `Cache-Control: no-store`.
Erros: 400 `VALIDATION_ERROR`/`AUDIENCE_NOT_ALLOWED`; 401 `INVALID_CREDENTIALS`/`INVALID_CLIENT`; 403 `EMAIL_NOT_VERIFIED`/`ACCOUNT_DISABLED`/`ORIGIN_NOT_ALLOWED`; 429 `RATE_LIMITED`.
Rastreio: CA-FT03-01…12, 22…24.

### API-FT03-02 — Renovar
`POST /auth/refresh` · comando.
Entrada: cookie `__Secure-achrt_<client_id>` (BROWSER) com cabeçalho `X-Achernar-Client` **ou** `refresh_token` no corpo (CONFIDENTIAL, autenticado por Basic); `audiences` opcional.
Sucesso `200`: resposta de tokens; `refresh_rotated=false` em reuso tolerado (sem novo refresh/cookie).
Erros: 401 `INVALID_REFRESH_TOKEN`/`INVALID_CLIENT`; 400 `AUDIENCE_NOT_ALLOWED`; 403 `ORIGIN_NOT_ALLOWED`/ recusa CSRF (`FORBIDDEN`); 429.
Rastreio: CA-FT03-13…20.

### API-FT03-03 — Logout
`POST /auth/logout` · comando idempotente. Entrada igual à de refresh (sem audiências). Sucesso `204` com `Set-Cookie` de expiração (BROWSER). Erros: apenas 403 por origem/CSRF e 429.
Rastreio: CA-FT03-21, 22.

## Integrações: nenhuma externa. FT-13 (TPL-08). FT-12 (eventos).
## Rastreabilidade
API-FT03-01 → CA-FT03-01…12 · API-FT03-02 → CA-FT03-13…20 · API-FT03-03 → CA-FT03-21.
