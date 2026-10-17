# Achernar Identity — Convenções de API e Índice de Endpoints

Contratos detalhados por feature em `specs/FT-xx/api.md`. Este documento fixa o que é comum (D-32, D-33, D-41, D-51).

## 1. Convenções

| Tema | Regra |
|---|---|
| Formato | JSON UTF-8 (`application/json`); erros `application/problem+json`; **snake_case** (D-51) |
| Datas/IDs | ISO-8601 UTC (`2026-10-17T12:00:00Z`); UUID textual |
| Prefixos | `/auth/*` (telas das aplicações), `/api/v1/*` (conta, sessões, admin, introspecção), `/setup/*`, `/.well-known/*` |
| Autenticação | `/auth/*` e `/setup/*`: pública (limites + CORS/Origin quando aplicável). `/api/v1/*` (exceto introspecção): `Authorization: Bearer <access_token>` com `aud` contendo a audiência do Identity (P-63). Introspecção: HTTP Basic `audience:segredo` da API. Admin: perfil `ADMIN` (RN-ADM-01). |
| Identificação do cliente | `BROWSER`: `client_id` no corpo (ou query nas rotas GET) + `Origin` permitido. `CONFIDENTIAL`: `Authorization: Basic base64(client_id:client_secret)`; se `client_id` também vier no corpo, deve coincidir. |
| Cabeçalhos de requisição | `Content-Type: application/json` (POST/PATCH/PUT com corpo); `Accept-Language: pt-BR\|en`; `X-Request-Id` (opcional, ≤ 64 chars, reaproveitado como `trace_id`); **`X-Achernar-Client: <client_id>` obrigatório** em `/auth/refresh` e `/auth/logout` de aplicações `BROWSER` (CSRF/D-07). |
| Cabeçalhos de resposta | `X-Request-Id`; `Cache-Control: no-store` em tudo que carrega tokens/dados de conta; `Retry-After` em 429/503; `Vary: Origin`; `X-Content-Type-Options: nosniff`; `Referrer-Policy: no-referrer`. |
| CORS | D-41. Pré-voo `OPTIONS` respondido sem autenticação; `Access-Control-Allow-Credentials: true` apenas em `/auth/refresh`, `/auth/logout`, `/auth/login`, `/auth/register`, `/auth/social/exchange` (BROWSER) com origem validada. |
| Paginação (listas pequenas) | `?page=0&size=25` (máx. 100, P-57) ⇒ `{ "items": [...], "page": 0, "size": 25, "total": 137 }` |
| Paginação (eventos) | `?limit=50&cursor=<opaco>` ⇒ `{ "items": [...], "next_cursor": "…" \| null }` |
| Ordenação | `?sort=campo,asc\|desc` com **lista permitida** por endpoint; fora da lista ⇒ `VALIDATION_ERROR` |
| Concorrência | PATCH exige `version`; divergente ⇒ `409 CONFLICT` |
| Idempotência | operações são idempotentes por desenho (cadastro, logout, revogação) ou usam token de uso único; não há `Idempotency-Key` |
| Limites | corpo ≤ P-48; strings com `maxLength` conforme contrato; campos desconhecidos no corpo ⇒ ignorados (exceto nos PATCH, que rejeitam desconhecidos com `VALIDATION_ERROR`) |
| Erros | `error-catalog.md`; sem stack trace; `trace_id` sempre presente |
| Versionamento | D-33 |

### 1.1 Objeto de erro (`application/problem+json`)

```json
{
  "type": "https://id.exemplo.com.br/problems/validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "Um ou mais campos são inválidos.",
  "instance": "/auth/register",
  "trace_id": "9f1c…",
  "errors": [{ "field": "password", "code": "TOO_SHORT", "message": "Use ao menos 12 caracteres." }]
}
```

### 1.2 Resposta de tokens (`/auth/login`, `/auth/refresh`, `/auth/social/exchange`)

```json
{
  "token_type": "Bearer",
  "access_token": "<jwt>",
  "expires_in": 600,
  "session_id": "6b0d…",
  "audiences": ["https://api.exemplo.com.br", "achernar-identity-api"],
  "refresh_token": "<apenas CONFIDENTIAL>",
  "refresh_expires_in": 604800,
  "refresh_rotated": true
}
```
`BROWSER`: **sem** `refresh_token` no corpo; `Set-Cookie: __Secure-achrt_<client_id>=<token>; Path=/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=<s>`.
`refresh_rotated=false` quando a resposta decorre de reuso tolerado (RN-TOK-04): nenhum novo refresh é emitido.

### 1.3 Autenticação de API do Identity

Token inválido/expirado/assinatura inválida/`aud` ausente ⇒ `401` + `WWW-Authenticate: Bearer error="invalid_token"`. Sessão revogada ⇒ `401 SESSION_REVOKED`.
Sem perfil ⇒ `403 FORBIDDEN`. Reautenticação exigida ⇒ `403 REAUTH_REQUIRED`.

## 2. Índice de endpoints

| Método e caminho | UC | Spec | Autenticação | Taxa (P-xx) |
|---|---|---|---|---|
| `GET /setup/status` | UC-001 | FT-01 | pública | P-38 |
| `POST /setup/admin` | UC-001 | FT-01 | pública + bootstrap token | P-38 |
| `GET /auth/applications/{client_id}/config` | UC-068 | FT-09 | pública (origem) | P-31 |
| `POST /auth/register` | UC-010 | FT-02 | cliente | P-32 |
| `POST /auth/email/verify` | UC-011 | FT-02 | cliente | P-31 |
| `POST /auth/email/resend-verification` | UC-012 | FT-02 | cliente | P-34 |
| `POST /auth/login` | UC-020 | FT-03 | cliente | P-30, P-31 |
| `POST /auth/refresh` | UC-021 | FT-03 | cookie/refresh + cliente | P-36 |
| `POST /auth/logout` | UC-022 | FT-03 | cookie/refresh + cliente | P-36 |
| `POST /auth/password/forgot` | UC-030 | FT-04 | cliente | P-33 |
| `POST /auth/password/reset` | UC-031 | FT-04 | cliente + token | P-31 |
| `POST /auth/email-change/confirm` | UC-036 | FT-05 | cliente + token | P-31 |
| `GET /auth/social/{provider}/start` | UC-050 | FT-07 | cliente (query) | P-37 |
| `GET /auth/social/{provider}/callback` | UC-051 | FT-07 | `state` + cookie de amarração | P-37 |
| `POST /auth/social/exchange` | UC-052 | FT-07 | cliente + `code_verifier` | P-31 |
| `GET /.well-known/jwks.json` | UC-070 | FT-10 | pública (cacheável) | — |
| `GET /.well-known/openid-configuration` · `/.well-known/oauth-authorization-server` | UC-070 | FT-10 | pública | — |
| `GET /api/v1/account` | UC-034 | FT-05 | Bearer | — |
| `POST /api/v1/account/reauth` | UC-033 | FT-04 | Bearer | P-39 |
| `POST /api/v1/account/reauth/social/{provider}/intent` | UC-055 | FT-08 | Bearer | P-37 |
| `POST /api/v1/account/password` | UC-032, UC-037 | FT-04 | Bearer + reauth | P-39 |
| `POST /api/v1/account/email-change` | UC-035 | FT-05 | Bearer + reauth | P-35 |
| `POST /api/v1/account/social-links/{provider}/intent` | UC-053 | FT-08 | Bearer + reauth | P-37 |
| `DELETE /api/v1/account/social-links/{provider}` | UC-054 | FT-08 | Bearer + reauth | — |
| `GET /api/v1/sessions` | UC-040 | FT-06 | Bearer | — |
| `DELETE /api/v1/sessions/{id}` | UC-041 | FT-06 | Bearer | — |
| `DELETE /api/v1/sessions` | UC-042 | FT-06 | Bearer + reauth | — |
| `GET/POST /api/v1/admin/applications` | UC-061/060 | FT-09 | Bearer + ADMIN | — |
| `GET/PATCH /api/v1/admin/applications/{id}` | UC-061/062 | FT-09 | Bearer + ADMIN | — |
| `POST /api/v1/admin/applications/{id}/secrets` · `DELETE …/secrets/{secret_id}` | UC-063 | FT-09 | Bearer + ADMIN | — |
| `GET/POST /api/v1/admin/apis` | UC-065/064 | FT-09 | Bearer + ADMIN | — |
| `GET/PATCH /api/v1/admin/apis/{id}` | UC-065/066 | FT-09 | Bearer + ADMIN | — |
| `POST /api/v1/admin/apis/{id}/credentials` · `DELETE …/credentials/{credential_id}` | UC-067 | FT-09 | Bearer + ADMIN | — |
| `GET /api/v1/admin/accounts` · `GET …/{id}` | UC-080 | FT-11 | Bearer + ADMIN | — |
| `POST /api/v1/admin/accounts/{id}/disable` · `/enable` | UC-081/082 | FT-11 | Bearer + ADMIN | — |
| `PUT /api/v1/admin/accounts/{id}/role` | UC-083 | FT-11 | Bearer + ADMIN | — |
| `POST /api/v1/admin/accounts/{id}/anonymize` | UC-084 | FT-11 | Bearer + ADMIN | — |
| `GET /api/v1/admin/signing-keys` | UC-072 | FT-10 | Bearer + ADMIN | — |
| `POST /api/v1/admin/signing-keys/rotate` · `POST …/{kid}/remove` | UC-072 | FT-10 | Bearer + ADMIN | — |
| `GET /api/v1/admin/security-events` | UC-090 | FT-12 | Bearer + ADMIN | — |
| `POST /api/v1/tokens/introspect` | UC-073 | FT-10 | Basic (API) | P-45 |
| `GET /actuator/health/{liveness,readiness}` | — | FND-01 | porta de gestão | — |

Total: 40 operações HTTP de produto (+ health).
