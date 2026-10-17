# API Spec — FT-11 Administração de contas
`NEW` · `APPROVED` (documental). Todas: Bearer + ADMIN, `no-store`.

### API-FT11-01 — Listar contas
`GET /api/v1/admin/accounts?q=&status=&role=&method=&created_from=&created_to=&page=&size=&sort=` (sort: `created_at`, `email`, `status`). `200 {items:[{id,email,status,role,email_verified,has_password,providers[],created_at}],page,size,total}`. Rastreio: CA-FT11-01, 02, 15.
### API-FT11-02 — Detalhe
`GET /api/v1/admin/accounts/{id}` ⇒ `{…campos da lista, locale, disabled_at, disabled_reason, anonymized_at, active_sessions, social_links[], recent_events[20], version}`. 404. Rastreio: CA-FT11-03.
### API-FT11-03 — Desativar
`POST /api/v1/admin/accounts/{id}/disable` · `{reason?≤200}` ⇒ `204`. Erros: 403 `FORBIDDEN` (a si mesmo)/`REAUTH_REQUIRED`; 409 `LAST_ADMIN_PROTECTED`; 404. Rastreio: CA-FT11-04…07, 19.
### API-FT11-04 — Reativar
`POST /api/v1/admin/accounts/{id}/enable` ⇒ `204`; 409 `CONFLICT` se anonimizada. Rastreio: CA-FT11-08.
### API-FT11-05 — Alterar perfil
`PUT /api/v1/admin/accounts/{id}/role` · `{role:"ADMIN"|"USER", version}` ⇒ `200` conta. Erros: 409 `LAST_ADMIN_PROTECTED`/`CONFLICT`; 403 `REAUTH_REQUIRED`. Rastreio: CA-FT11-09, 10, 14.
### API-FT11-06 — Anonimizar
`POST /api/v1/admin/accounts/{id}/anonymize` · `{confirm_email}` ⇒ `204`. Erros: 400 `VALIDATION_ERROR`; 409 `CONFLICT`/`LAST_ADMIN_PROTECTED`; 403 `REAUTH_REQUIRED`. Rastreio: CA-FT11-11…13.

Rastreabilidade: API-FT11-01 → CA-FT11-01, 02 · -02 → CA-FT11-03 · -03 → CA-FT11-04 · -04 → CA-FT11-08 · -05 → CA-FT11-09, 10 · -06 → CA-FT11-11. Pontos a validar: nenhum.
