# API Spec — FT-09 Registro de aplicações e APIs
`NEW` · `APPROVED` (documental). Todas `/api/v1/admin/**`: Bearer + ADMIN, `Cache-Control: no-store`. Paginação/ordenação: `api-conventions.md §1`.

### API-FT09-01 — Listar aplicações
`GET /api/v1/admin/applications?q=&status=&page=&size=&sort=` (sort permitido: `name`, `client_id`, `created_at`, `status`). `200` `{items,page,size,total}`; item: `id, client_id, name, type, status, allow_registration, identity_api_access, created_at`. Rastreio: CA-FT09-08, 21.
### API-FT09-02 — Cadastrar aplicação
`POST /api/v1/admin/applications`. Entrada: `client_id`, `name` (≤ 100), `description` (≤ 500), `type`, `origins[]`, `redirect_uris[]`, `verify_email_url`, `reset_password_url`, `confirm_email_change_url`, `allow_registration`, `identity_api_access`, `google_enabled`, `facebook_enabled`, `access_token_ttl_seconds` (300–900, opcional), `audiences: [{audience, is_default}]`. `201` aplicação completa + `client_secret` (só `CONFIDENTIAL`). Erros: 400 `VALIDATION_ERROR` (com `errors[]` por campo); 409 `CONFLICT`. Rastreio: CA-FT09-01…07, 20, 23.
### API-FT09-03 — Consultar aplicação
`GET /api/v1/admin/applications/{id}` ⇒ campos acima + `status`, `is_system`, `secrets: [{id, created_at, last_used_at, revoked_at}]` (sem valores), `version`. 404 `NOT_FOUND`. Rastreio: CA-FT09-08.
### API-FT09-04 — Alterar aplicação
`PATCH /api/v1/admin/applications/{id}` com `version` e campos mutáveis (todos os acima exceto `client_id`, `type`) mais `status`. `200`. Erros: 400; 409 `CONFLICT`; 409 se violar regra da aplicação do sistema. Rastreio: CA-FT09-09…11, 23.
### API-FT09-05 — Criar segredo
`POST /api/v1/admin/applications/{id}/secrets` ⇒ `201 {secret_id, client_secret, created_at}`. 409 `CONFLICT` (já há 2 ativos); 400 se `BROWSER`. Rastreio: CA-FT09-12, 24.
### API-FT09-06 — Revogar segredo
`DELETE /api/v1/admin/applications/{id}/secrets/{secret_id}` ⇒ `204`; 409 se for o último ativo de `CONFIDENTIAL`; 404. Rastreio: CA-FT09-12.
### API-FT09-07/08 — Listar e cadastrar API
`GET /api/v1/admin/apis?q=&status=&page=&size=&sort=` e `POST /api/v1/admin/apis` (`audience`, `name`, `description`) ⇒ `201` + `credential` (`credential_id`, `secret`) uma vez. 409 `CONFLICT` se audiência duplicada. Rastreio: CA-FT09-14, 20.
### API-FT09-09/10 — Consultar e alterar API
`GET /api/v1/admin/apis/{id}`; `PATCH …/{id}` (`name`, `description`, `status`, `version`). Rastreio: CA-FT09-15.
### API-FT09-11/12 — Credenciais da API
`POST /api/v1/admin/apis/{id}/credentials` ⇒ `201 {credential_id, secret}`; `DELETE …/credentials/{credential_id}` ⇒ `204`. Rastreio: CA-FT09-16.
### API-FT09-13 — Configuração pública
`GET /auth/applications/{client_id}/config` (pública, `Origin` quando enviado). `200 {name, type, allow_registration, providers: ["google","facebook"]}`; `Cache-Control: public, max-age=60`; 404 `NOT_FOUND` genérico; 429 (P-31). Rastreio: CA-FT09-18, 19.

Rastreabilidade: API-FT09-01 → CA-FT09-08 · API-FT09-02 → CA-FT09-01…07 · API-FT09-04 → CA-FT09-09…11 · API-FT09-05/06 → CA-FT09-12 · API-FT09-07…12 → CA-FT09-14…16 · API-FT09-13 → CA-FT09-18. Pontos a validar: nenhum.
