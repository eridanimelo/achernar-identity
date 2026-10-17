# API Spec — FT-14 Interface base, tema e sessão no navegador
`NEW` · `APPROVED` (documental).

Sem endpoints próprios de negócio. Consome contratos de outras Specs:
- **API-FT14-01 — Configuração da aplicação:** `GET /auth/applications/{client_id}/config` (FT-09). Rastreio: CA-FT14-05, 09.
- **API-FT14-02 — Estado do setup:** `GET /setup/status` (FT-01). Rastreio: CA-FT14-03.
- **API-FT14-03 — Renovação e logout:** `POST /auth/refresh`, `POST /auth/logout` (FT-03) com `X-Achernar-Client` e cookie. Rastreio: CA-FT14-05…07.
- **API-FT14-04 — Chamadas autenticadas:** Bearer em `/api/v1/**`; tratamento padronizado de `problem+json` (`REAUTH_REQUIRED`, `SESSION_REVOKED`, `RATE_LIMITED`, `FORBIDDEN`, 503). Rastreio: CA-FT14-08, 14.
- **API-FT14-05 — Entrega da interface:** recursos estáticos sob P-61 (`/app`), *fallback* para `index.html`, cabeçalhos de segurança (CSP, `nosniff`, `Referrer-Policy: no-referrer`, `frame-ancestors 'none'`), cache longo para recursos com *hash* e `no-cache` para o documento principal. Rastreio: CA-FT14-16, 20.

Rastreabilidade: API-FT14-01 → CA-FT14-05 · -02 → CA-FT14-03 · -03 → CA-FT14-06, 07 · -04 → CA-FT14-08 · -05 → CA-FT14-16, 20. Pontos a validar: nenhum.
