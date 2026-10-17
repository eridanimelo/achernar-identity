# API Spec — FT-02 Cadastro e verificação de e-mail

## 0. Identificação
FT-02 · `NEW` · `APPROVED` (documental). Convenções: `api-conventions.md`.

## 1. Operações

### API-FT02-01 — Cadastrar conta
**Contrato:** `POST /auth/register` · comando · autenticação de cliente (`client_id` + `Origin`, ou Basic para `CONFIDENTIAL`).
**Entrada (JSON):**
| Campo | Obrig. | Restrição | Ref. |
|---|---|---|---|
| `client_id` | sim (BROWSER) | `^[a-z0-9-]{3,64}$` | RN-AUT-04 |
| `email` | sim | válido, ≤ 254 | RN-ACC-02 |
| `password` | sim | 1–128 após NFKC; política | RN-PWD-01 |
| `locale` | não | `pt-BR`\|`en` | RN-ACC-08 |
**Sucesso:** `202` `{ "status": "verification_email_sent_if_applicable" }` (idêntico em todos os casos aceitos). `Cache-Control: no-store`.
**Erros:** 400 `VALIDATION_ERROR`/`PASSWORD_POLICY_VIOLATION`/`MALFORMED_REQUEST`; 401 `INVALID_CLIENT`; 403 `ORIGIN_NOT_ALLOWED`/`REGISTRATION_DISABLED`; 413; 415; 429 `RATE_LIMITED` (por IP). Limite por e-mail ⇒ 202 sem efeito.
**Segurança:** SEC-FT02-01…04. **Rastreio:** CA-FT02-01…07, 14, 15.

### API-FT02-02 — Verificar e-mail
**Contrato:** `POST /auth/email/verify` · comando.
**Entrada:** `client_id` (BROWSER), `token` (string 43–128, obrigatório).
**Sucesso:** `204` sem corpo.
**Erros:** 400 `TOKEN_INVALID_OR_EXPIRED`/`VALIDATION_ERROR`; 401 `INVALID_CLIENT`; 403 `ORIGIN_NOT_ALLOWED`; 429.
**Rastreio:** CA-FT02-08…10.

### API-FT02-03 — Reenviar verificação
**Contrato:** `POST /auth/email/resend-verification` · comando.
**Entrada:** `client_id`, `email` (válido, ≤ 254).
**Sucesso:** `202` corpo neutro (igual ao do cadastro).
**Erros:** 400 validação; 401 `INVALID_CLIENT`; 403 `ORIGIN_NOT_ALLOWED`; 429 (somente limite por IP).
**Rastreio:** CA-FT02-11.

## 2. Filtros/paginação: `NOT_APPLICABLE`.
## 3. Integrações: SMTP via outbox (FT-13: TPL-01, TPL-05).
## 4. Pontos a validar: nenhum.
## 5. Rastreabilidade
| Operação | Critérios |
|---|---|
| API-FT02-01 | CA-FT02-01…07, 14, 15 |
| API-FT02-02 | CA-FT02-08…10 |
| API-FT02-03 | CA-FT02-11 |
