# Business Spec — FT-10 Chaves, JWKS e introspecção

## 0. Identificação
FT-10 (UC-070, UC-071, UC-072, UC-073) · `NEW` · Módulo M6 Chaves e validação · `APPROVED` (documental). Normativo: `token-lifecycle.md §6–7`.

## 1. Objetivo
Publicar as chaves públicas e metadados para que qualquer API valide tokens sem segredo compartilhado; rotacionar as chaves de assinatura sem interromper consumidores; e oferecer introspecção para bloqueio imediato de tokens.

## 2. História
**Como** equipe de uma API **quero** validar tokens com as chaves públicas e consultar o Identity quando preciso de revogação imediata. **Como** administrador **quero** rotacionar ou remover chaves em emergência.

## 3. Fluxos
### UC-070 — Publicar chaves e metadados
`GET /.well-known/jwks.json` devolve as chaves públicas (`PUBLISHED`, `ACTIVE`, `RETIRED`) com `kid`; metadados (`/.well-known/openid-configuration` e `/.well-known/oauth-authorization-server`) informam emissor, `jwks_uri` e algoritmos. Cacheáveis.
### UC-071 — Rotacionar chaves automaticamente
Job horário: publica a próxima chave 24 h antes, promove-a a ativa no prazo (90 dias), aposenta a anterior (permanece no JWKS por 48 h) e depois remove (apagando a chave privada). Primeira subida gera e ativa a primeira chave.
### UC-072 — Consultar e rotacionar chaves (administrador)
Lista chaves e estados; rotação manual (ativa nova imediatamente); remoção emergencial de chave (com opção de revogar todas as sessões).
### UC-073 — Introspectar token
A API autentica-se com audiência e segredo e envia o access token; recebe `active` e, se ativo, dados mínimos. Ativo ⇔ assinatura/emissor/validade OK, audiência da API presente, sessão ativa, conta `ACTIVE` e aplicação ativa.

## 4. Critérios de aceite
- **CA-FT10-01 — JWKS.** `GET /.well-known/jwks.json` devolve apenas parte pública (`kty=RSA`, `use=sig`, `alg=RS256`, `kid`, `n`, `e`) de chaves `PUBLISHED`, `ACTIVE`, `RETIRED`; `Cache-Control: public, max-age=300`; sem autenticação; p95 ≤ 50 ms. Ref.: RF-21, RN-TOK-13, RNF-02
- **CA-FT10-02 — `kid` coerente.** O `kid` do cabeçalho de todo token emitido existe no JWKS e é o thumbprint da chave. Ref.: D-04, RN-TOK-12
- **CA-FT10-03 — Metadados.** Os dois documentos de metadados informam `issuer` (= P-60), `jwks_uri`, `id_token_signing_alg_values_supported`/`token_endpoint`... somente campos aplicáveis; sem campos que sugiram suporte a SSO/OIDC completo (`authorization_endpoint` ausente). Ref.: RF-21
- **CA-FT10-04 — Primeira subida.** Sem chaves, a primeira subida gera e ativa uma chave; subidas simultâneas de duas instâncias resultam em uma única chave ativa. Ref.: RN-TOK-12, DM-06
- **CA-FT10-05 — Invariante.** Em todo instante há exatamente uma chave `ACTIVE`; o JWKS contém `PUBLISHED ∪ ACTIVE ∪ RETIRED`. Ref.: RN-TOK-12
- **CA-FT10-06 — Rotação automática.** Com o relógio avançado: chave seguinte publicada a `P-10 − P-11` da ativação; promovida após `P-11`; anterior `RETIRED` e removida após `P-12`; chave privada apagada ao remover; evento `SIGNING_KEY_ROTATED`/`SIGNING_KEY_REMOVED`. Ref.: UC-071, P-10…P-12
- **CA-FT10-07 — Tokens atravessam a rotação.** Tokens assinados pela chave anterior continuam válidos até `exp` enquanto ela está no JWKS (P-12 ≥ P-01 + skew + cache). Ref.: P-12
- **CA-FT10-08 — Consistência de configuração.** Se P-11 < 2 × P-14 ou P-12 < P-01 + P-06 + P-14, a aplicação não inicia. Ref.: token-lifecycle §6
- **CA-FT10-09 — Idempotência multi-instância.** Dois jobs simultâneos não geram duas chaves nem promovem duas vezes. Ref.: DM-06, RNF-14
- **CA-FT10-10 — Listar chaves (admin).** Retorna `kid`, estado, datas de publicação/ativação/aposentadoria/remoção; nunca material privado; exige ADMIN. Ref.: RF-32, RN-ADM-01
- **CA-FT10-11 — Rotação manual.** Gera nova chave, ativa-a imediatamente e aposenta a atual (permanece no JWKS); evento `SIGNING_KEY_ROTATED(mode=manual)`; requer ADMIN e reautenticação recente. Ref.: RF-32
- **CA-FT10-12 — Remoção emergencial.** `emergency=true` marca `REMOVED` imediatamente, gera e ativa substituta; tokens da chave removida são rejeitados por APIs quando atualizarem o JWKS (≤ 5 min); `revoke_all_sessions=true` revoga todas as sessões (`KEY_COMPROMISE`); evento com `emergency=true`. Ref.: RF-32, SEC-25
- **CA-FT10-13 — Proteção da chave ativa.** Remover a única chave `ACTIVE` sem `emergency` ⇒ `CONFLICT`; remover chave `PUBLISHED` ou `RETIRED` é permitido. Ref.: RN-TOK-12
- **CA-FT10-14 — Introspecção de token ativo.** API autenticada e token válido com sua audiência ⇒ `200 {active:true, sub, sid, aud, azp, client_id, exp, iat, amr}`. Ref.: RF-22, RN-TOK-11
- **CA-FT10-15 — Introspecção inativa.** Assinatura inválida, expirado, audiência diferente da API chamadora, sessão revogada, conta ≠ `ACTIVE`, aplicação desativada, token malformado ou refresh token ⇒ `200 {active:false}` sem motivo. Ref.: RN-TOK-11
- **CA-FT10-16 — Autenticação da API.** Credencial inexistente, revogada ou API desativada ⇒ `401 INVALID_CLIENT` (sem distinguir) e evento `INTROSPECTION_AUTH_FAILED`. Ref.: RN-APP-15, 16
- **CA-FT10-17 — Limite.** 600/min por API ⇒ `429`. Ref.: P-45
- **CA-FT10-18 — Desempenho.** p95 ≤ 80 ms a 50 req/s. Ref.: RNF-02
- **CA-FT10-19 — Efeito imediato.** Após revogar a sessão ou desativar a conta, a introspecção seguinte retorna `active=false` (sem cache no servidor). Ref.: RN-TOK-09, 11, RF-22
- **CA-FT10-20 — Sem vazamento.** A resposta ativa não inclui e-mail, roles de negócio ou dados pessoais além de `sub`; `Cache-Control: no-store`. Ref.: RNF-15
- **CA-FT10-21 — Guia de integração.** O guia `consumer-integration.md` descreve configuração de APIs monolíticas e microsserviços com Spring Security (emissor/JWKS, `aud`, `typ`, cache de introspecção ≤ 30 s, falha fechada para operações sensíveis). Ref.: RF-34, RN-TOK-14
- **CA-FT10-22 — Validação pela API do Identity.** A própria API do Identity valida `sid` no banco a cada chamada (efeito imediato da revogação) e confere o perfil no banco para rotas administrativas. Ref.: RN-TOK-15

## 5. Regras: RN-TOK-01, 09…15, RN-APP-15/16, SEC-25, SEC-34, P-10…P-14, P-45.
## 6. Perfis: público (JWKS/metadados), API consumidora (introspecção), ADMIN (UC-072), sistema (UC-071).
## 7. Rastreabilidade: UC-070 → CA-FT10-01…03 · UC-071 → CA-FT10-04…09 · UC-072 → CA-FT10-10…13 · UC-073 → CA-FT10-14…20, 22 · RF-34 → CA-FT10-21.
## 8. Pontos a validar / divergências / LEGACY: nenhum / nenhuma / `NOT_APPLICABLE`.
## 9. Resultado esperado: validação local confiável pelas APIs, rotação sem interrupção, revogação imediata quando necessária.
