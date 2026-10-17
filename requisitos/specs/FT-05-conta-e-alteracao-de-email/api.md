# API Spec — FT-05 Conta e alteração de e-mail
`NEW` · `APPROVED` (documental). Convenções: `api-conventions.md`.

### API-FT05-01 — Consultar minha conta
`GET /api/v1/account` · Bearer (audiência do Identity). Sucesso `200`:
`{ "id", "email", "email_verified", "status", "role", "locale", "has_password", "social_links": [{"provider","linked_at"}], "created_at" }`. Erros: 401 `SESSION_REVOKED`/token inválido. Rastreio: CA-FT05-01, 02.
### API-FT05-02 — Solicitar alteração de e-mail
`POST /api/v1/account/email-change` · Bearer + reautenticação. Entrada: `new_email` (≤ 254), `client_id` (opcional; define a URL de confirmação, padrão: aplicação da sessão). Sucesso `202` neutro. Erros: 400 `VALIDATION_ERROR`; 403 `REAUTH_REQUIRED`; 401; 429 `RATE_LIMITED` (P-35). Rastreio: CA-FT05-03…07, 12, 13.
### API-FT05-03 — Confirmar alteração
`POST /auth/email-change/confirm` · cliente + token. Entrada: `client_id`, `token`. Sucesso `204`. Erros: 400 `TOKEN_INVALID_OR_EXPIRED`; 401/403 de cliente/origem; 429. Rastreio: CA-FT05-08…11.

Rastreabilidade: API-FT05-01 → CA-FT05-01, 02 · -02 → CA-FT05-03…07 · -03 → CA-FT05-08…11. Pontos a validar: nenhum.
