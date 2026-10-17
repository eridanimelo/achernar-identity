# API Spec — FT-08 Vínculos sociais
`NEW` · `APPROVED` (documental). Autenticação: Bearer (audiência do Identity).

### API-FT08-01 — Intenção de vínculo
`POST /api/v1/account/social-links/{provider}/intent` · comando · reautenticação exigida. Entrada: `return_url` (uma `redirect_uri` registrada da aplicação da sessão), `state` (≤ 500, opcional). Sucesso `200`: `{ "authorization_url": "…/auth/social/{provider}/start?intent=…", "expires_in": 120 }`. Erros: 400 `PROVIDER_NOT_ENABLED`/`REDIRECT_URI_NOT_ALLOWED`; 403 `REAUTH_REQUIRED`; 409 `SOCIAL_ALREADY_LINKED` (conta já tem vínculo do provedor); 429. Rastreio: CA-FT08-01, 03, 14.
### API-FT08-02 — Desvincular
`DELETE /api/v1/account/social-links/{provider}` · comando · reautenticação exigida. Sucesso `204`. Erros: 403 `REAUTH_REQUIRED`; 404 `NOT_FOUND`; 409 `LAST_ACCESS_METHOD`. Rastreio: CA-FT08-06…09.
### API-FT08-03 — Intenção de reautenticação social
`POST /api/v1/account/reauth/social/{provider}/intent` · comando. Entrada igual à API-FT08-01. Sucesso `200` igual. Erros: 400; 404 `NOT_FOUND` (sem vínculo desse provedor); 429. Rastreio: CA-FT08-10…12.
O retorno dos fluxos usa `GET /auth/social/{provider}/start?intent=` e `/callback` (FT-07), concluindo em `return_url?result=linked|reauthenticated&state=…` ou `?error=…&state=…`. Rastreio: CA-FT08-02…05, 11…13.

Rastreabilidade: API-FT08-01 → CA-FT08-01 · -02 → CA-FT08-06, 07 · -03 → CA-FT08-10. Pontos a validar: nenhum.
