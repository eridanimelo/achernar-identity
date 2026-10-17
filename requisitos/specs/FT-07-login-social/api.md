# API Spec — FT-07 Login social
`NEW` · `APPROVED` (documental). Detalhes de protocolo: `social-login.md`.

### API-FT07-01 — Iniciar
`GET /auth/social/{provider}/start` (`provider` ∈ `google|facebook`) · consulta com redirecionamento. Query: `client_id`, `redirect_uri`, `state` (≤ 500), `code_challenge` (43–128), `code_challenge_method=S256`, `audiences` (opcional, separadas por espaço); ou `intent` (fluxos LINK/REAUTH de FT-08). Sucesso: `302` ao provedor + `Set-Cookie` de amarração. Erros: página HTML 400 (sem redirecionar) para cliente/redirect inválidos; `PROVIDER_NOT_ENABLED`; 429. Rastreio: CA-FT07-01…03, 20…22.
### API-FT07-02 — Retorno do provedor
`GET /auth/social/{provider}/callback` · query `code`, `state` (ou `error` do provedor) + cookie de amarração. Sucesso: `302` a `redirect_uri?code=<código Identity>&state=<state do cliente>`; falha: `302` com `error=<valor de error-catalog>`; ou página de erro quando a transação não é confiável. Rastreio: CA-FT07-04…13.
### API-FT07-03 — Trocar código
`POST /auth/social/exchange` · cliente. Entrada JSON: `client_id`, `code`, `code_verifier`, `redirect_uri`. Sucesso `200`: resposta de tokens (§1.2). Erros: 400 `INVALID_GRANT`/`VALIDATION_ERROR`; 401 `INVALID_CLIENT`; 403 `ORIGIN_NOT_ALLOWED`; 429. Rastreio: CA-FT07-14…19.

Integrações externas: Google (OIDC), Facebook (Graph) — hosts fixos, timeout 5 s, sem URLs fornecidas pelo usuário (SEC-40).
Rastreabilidade: API-FT07-01 → CA-FT07-01…03 · -02 → CA-FT07-04…13 · -03 → CA-FT07-14…19. Pontos a validar: nenhum.
