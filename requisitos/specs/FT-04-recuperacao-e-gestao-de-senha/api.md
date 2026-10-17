# API Spec — FT-04 Recuperação e gestão de senha
Convenções: `api-conventions.md`. `NEW` · `APPROVED` (documental).

### API-FT04-01 — Solicitar recuperação
`POST /auth/password/forgot` · cliente. Entrada: `client_id`, `email`. Sucesso `202` corpo neutro. Erros: 400; 401 `INVALID_CLIENT`; 403 `ORIGIN_NOT_ALLOWED`; 429 (IP). Rastreio: CA-FT04-01…04, 10, 19.
### API-FT04-02 — Redefinir senha
`POST /auth/password/reset` · cliente + token. Entrada: `client_id`, `token`, `new_password`. Sucesso `204`. Erros: 400 `TOKEN_INVALID_OR_EXPIRED`/`PASSWORD_POLICY_VIOLATION`/`VALIDATION_ERROR`; 429. Rastreio: CA-FT04-05…09.
### API-FT04-03 — Reautenticar com senha
`POST /api/v1/account/reauth` · Bearer. Entrada: `password`. Sucesso `200` `{ "reauthenticated_until": "<ISO>" }`. Erros: 403 `REAUTH_FAILED`; 400 `VALIDATION_ERROR` (conta sem senha); 401; 429. Rastreio: CA-FT04-14, 15, 17.
### API-FT04-04 — Alterar ou definir senha
`POST /api/v1/account/password` · Bearer + reautenticação. Entrada: `current_password` (obrigatória quando a conta tem senha e não há janela de reautenticação), `new_password`. Sucesso `204`. Erros: 400 `PASSWORD_POLICY_VIOLATION`/`VALIDATION_ERROR`; 403 `REAUTH_REQUIRED`/`REAUTH_FAILED`; 401 `SESSION_REVOKED`; 429. Rastreio: CA-FT04-11…13, 16…18.

Rastreabilidade: API-FT04-01 → CA-FT04-01…04 · -02 → CA-FT04-05…09 · -03 → CA-FT04-14 · -04 → CA-FT04-11…13, 16.
Pontos a validar: nenhum.
