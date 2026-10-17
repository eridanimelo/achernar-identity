# API Spec — FT-10 Chaves, JWKS e introspecção
`NEW` · `APPROVED` (documental).

### API-FT10-01 — JWKS
`GET /.well-known/jwks.json` · pública. `200 application/jwk-set+json` `{keys:[{kty,use,alg,kid,n,e}]}`; `Cache-Control: public, max-age=300`; `ETag`. Rastreio: CA-FT10-01, 02, 05.
### API-FT10-02 — Metadados
`GET /.well-known/openid-configuration` e `GET /.well-known/oauth-authorization-server` · públicas. `200 {issuer, jwks_uri, token_endpoint_auth_methods_supported, id_token_signing_alg_values_supported:["RS256"], …}` (somente campos aplicáveis); cacheável (5 min). Rastreio: CA-FT10-03.
### API-FT10-03 — Listar chaves
`GET /api/v1/admin/signing-keys` · Bearer + ADMIN. `200 {items:[{kid,status,created_at,publish_at,activate_at,retire_at,remove_at,removal_reason}]}`. Rastreio: CA-FT10-10.
### API-FT10-04 — Rotação manual
`POST /api/v1/admin/signing-keys/rotate` · Bearer + ADMIN + reautenticação. `201` com a nova chave (pública). Erros: 403 `REAUTH_REQUIRED`/`FORBIDDEN`. Rastreio: CA-FT10-11.
### API-FT10-05 — Remover chave
`POST /api/v1/admin/signing-keys/{kid}/remove` · Bearer + ADMIN + reautenticação. Entrada: `emergency` (bool, padrão `false`), `revoke_all_sessions` (bool, só com `emergency`), `reason` (≤ 200). `204`. Erros: 404; 409 `CONFLICT` (única ativa sem emergência). Rastreio: CA-FT10-12, 13.
### API-FT10-06 — Introspecção
`POST /api/v1/tokens/introspect` · Basic `audience:segredo`. Entrada: `token` (JSON `{ "token": "…" }` ou formulário). `200 {active:false}` ou `200 {active:true, sub, sid, aud, azp, client_id, exp, iat, amr}`; `Cache-Control: no-store`. Erros: 401 `INVALID_CLIENT`; 429. Rastreio: CA-FT10-14…20.

Job de rotação (UC-071): interno, sem API. Rastreabilidade: API-FT10-01 → CA-FT10-01 · -02 → CA-FT10-03 · -03…05 → CA-FT10-10…13 · -06 → CA-FT10-14…19. Pontos a validar: nenhum.
