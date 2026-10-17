# Achernar Identity — Política de Segurança

Documento normativo transversal (RNF-01, RNF-04, RNF-06, RNF-16, RNF-19). Cada controle tem ID `SEC-xx`, rastreável a requisitos/regras e a cenários de teste
(`CT-SEC-*` em `test-plan.md`). As Specs `FT-xx/security.md` detalham o que se aplica a cada feature.

## 1. Ativos, atacantes e fronteiras de confiança

**Ativos:** credenciais (hash), refresh tokens, chaves privadas de assinatura, chave-mestra, segredos de aplicações/APIs, tokens de uso único, dados de contas (e-mail), trilha de eventos.
**Atacantes considerados:** anônimo na internet; usuário autenticado malicioso; script injetado (XSS) em uma aplicação consumidora; aplicação consumidora comprometida; API consumidora comprometida; administrador comprometido; operador com acesso ao banco (leitura); provedor de e-mail/rede interceptando mensagens.

```text
[Navegador do usuário] ── HTTPS ──▶ [App SPA (origem A)] ── HTTPS ──▶ [API de negócio]
        │                                   │                              │
        └──────── HTTPS (CORS+cookie) ──────┴──▶ [IDENTITY] ◀── JWKS/introspecção ──┘
                                              │   ├─▶ PostgreSQL (confiança total)
                                              │   ├─▶ SMTP (confiança limitada: conteúdo dos e-mails é visível ao provedor)
                                              │   └─▶ Google / Facebook (confiança para identidade, nunca para autorização das nossas APIs)
```

Fronteiras: (1) navegador↔serviços: **não confiável** (toda entrada validada); (2) aplicação `BROWSER`: não confiável para segredos; (3) aplicação `CONFIDENTIAL`/API: confiável *depois* de autenticada por segredo, limitada ao que o registro permite; (4) Identity↔PostgreSQL: confiável; (5) provedores sociais: confiáveis somente para afirmar identidade de suas próprias contas.

## 2. Ameaças e controles principais

| Ameaça | Controles |
|---|---|
| T-01 Credential stuffing / força bruta | SEC-03, SEC-04, SEC-05, SEC-06 |
| T-02 Roubo de token por XSS | SEC-12, SEC-13, SEC-23 |
| T-03 CSRF em refresh/logout | SEC-14, SEC-15 |
| T-04 Replay de refresh token | SEC-10, SEC-11 |
| T-05 Enumeração de contas | SEC-07, SEC-08 |
| T-06 CSRF/injeção de código no login social | SEC-17, SEC-18 |
| T-07 Open redirect | SEC-16 |
| T-08 Pré-sequestro / vinculação indevida | SEC-19, SEC-20 |
| T-09 Interceptação do link de e-mail | SEC-09, SEC-21 |
| T-10 Administrador comprometido | SEC-27, SEC-28 |
| T-11 Vazamento de chave privada | SEC-24, SEC-25 |
| T-12 Vazamento do banco | SEC-02, SEC-09, SEC-24, SEC-30 |
| T-13 Vazamento em logs | SEC-31 |
| T-14 Injeção de host em links de e-mail | SEC-22 |
| T-15 DoS por custo de hash/bloqueio | SEC-05, SEC-06, SEC-32 |
| T-16 Ataque de temporização | SEC-04, SEC-33 |
| T-17 Token vivo após desativação | SEC-34 (D-10) |
| T-18 Cadeia de suprimentos | SEC-36 |

## 3. Catálogo de controles

| ID | Controle | Requisito / Regra | Verificação |
|---|---|---|---|
| SEC-01 | **HTTPS obrigatório** em `prod` (TLS 1.2+; preferir 1.3); redirecionamento HTTP→HTTPS no proxy; `Strict-Transport-Security: max-age=31536000; includeSubDomains`; cookies `Secure`; `ACHERNAR_ISSUER` `https://` | RNF-04, PV-02 | CT-SEC-01 |
| SEC-02 | Senhas: Argon2id (P-16), `DelegatingPasswordEncoder`, rehash no login; nenhuma senha em log/evento/e-mail | RNF-01, RN-PWD-01, RN-AUT-07 | CT-SEC-02 |
| SEC-03 | Política de senha NIST (mín. 12, lista de comuns, sem composição, sem expiração) | RN-PWD-01 | CT-FT04-* |
| SEC-04 | Respostas e tempos equivalentes no login (hash fictício para conta inexistente) | RN-AUT-02, D-48 | CT-RNF-16 |
| SEC-05 | Bloqueio por conta (P-30) + limites por IP/rota (P-31…P-39) em PostgreSQL | RN-AUT-03, D-16 | CT-FT03-* |
| SEC-06 | Semáforo de concorrência no hash (≤ 2×núcleos; fila com timeout de 2 s ⇒ `429`/`503`) para evitar exaustão de CPU/memória | D-15 | CT-SEC-06 |
| SEC-07 | Anti-enumeração: cadastro, recuperação, reenvio, troca de e-mail e login com respostas idênticas | RN-ACC-06, D-13 | CT-SEC-07 |
| SEC-08 | Estados específicos (`EMAIL_NOT_VERIFIED`, `ACCOUNT_DISABLED`) somente após prova de posse | D-13 | CT-FT03-* |
| SEC-09 | Tokens de uso único: 256 bits, hash SHA-256 no banco, uso único atômico, validade curta, um ativo por finalidade, no **fragmento** da URL | D-17 | CT-FT02-*, CT-FT04-* |
| SEC-10 | Refresh rotativo, hash no banco, família = sessão, reuso fora da tolerância revoga | D-06 | CT-FT03-* |
| SEC-11 | Tolerância de reuso restrita ao antecessor imediato por P-04 | RN-TOK-04 | CT-FT03-* |
| SEC-12 | Access token **somente em memória** no navegador; proibido `localStorage`/`sessionStorage`/IndexedDB/URL | D-07 | CT-FT14-* |
| SEC-13 | Refresh do navegador em cookie `__Secure-achrt_<client_id>`: `HttpOnly; Secure; SameSite=Strict; Path=/auth; sem Domain` | D-07 | CT-FT03-* |
| SEC-14 | CSRF: `SameSite=Strict` + validação de `Origin` (obrigatório e igual às origens da aplicação) + `X-Achernar-Client` + `Content-Type: application/json` | D-07 | CT-SEC-14 |
| SEC-15 | Rotas de API Bearer não usam cookie ⇒ CSRF desabilitado conscientemente; rotas com cookie ⇒ SEC-14 | D-07 | CT-SEC-14 |
| SEC-16 | `redirect_uri` e URLs de ação: correspondência **exata** no registro; nunca redirecionar a URL não validada; nunca refletir parâmetros em HTML | RN-APP-04, §6 | CT-FT07-* |
| SEC-17 | Login social: `state` 256 bits (hash no banco), PKCE S256 ao provedor, `nonce` (Google), uso único, expiração 10 min, amarração ao navegador por cookie `Lax` | RN-SOC-13, D-19 | CT-FT07-* |
| SEC-18 | Código de troca próprio: 256 bits, 60 s, uso único, vinculado a `client_id`+`redirect_uri`+`code_challenge`; tokens do provedor nunca expostos | RN-SOC-14 | CT-FT07-* |
| SEC-19 | Nenhuma vinculação por e-mail; vincular exige sessão + reautenticação + prova no provedor | RN-SOC-05..08 | CT-FT07-*, CT-FT08-* |
| SEC-20 | Substituição de conta pendente por conta social verificada (anti pré-sequestro) | RN-SOC-06 | CT-FT07-* |
| SEC-21 | Links de e-mail via HTTPS; tokens expiram rápido (30 min–24 h); notificações de segurança ao titular em eventos críticos | RN-EML-01 | CT-FT13-* |
| SEC-22 | Links e redirecionamentos construídos **só** de configuração (P-60..P-62, URLs registradas), nunca de `Host`/`X-Forwarded-Host` do pedido | D-41 | CT-SEC-22 |
| SEC-23 | **CSP** na UI do Identity: `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'; object-src 'none'`; sem scripts de terceiros; fontes auto-hospedadas. Recomendação equivalente às aplicações consumidoras | RNF-19 | CT-SEC-HDR |
| SEC-24 | Chaves privadas e payload de outbox cifrados (AES-256-GCM) com `ACHERNAR_MASTER_KEY`; chave-mestra fora do banco/repositório/imagem | D-25 | CT-FT10-* |
| SEC-25 | Rotação de chaves (P-10) e remoção emergencial; apenas RS256 aceito; `alg`/`typ` verificados | D-04, D-25 | CT-FT10-* |
| SEC-26 | CORS dinâmico restrito às origens registradas; sem `*`; `Vary: Origin` | D-41 | CT-SEC-26 |
| SEC-27 | Autorização administrativa verificada no banco a cada requisição (perfil+estado+sessão); sem confiar só no claim | RN-ADM-01, RN-TOK-15 | CT-FT11-* |
| SEC-28 | Proteção do último admin; admin não se auto-desativa; ações auditadas; recomenda-se restringir `/api/v1/admin/**` por rede/IP no proxy | RN-ADM-03/04/08 | CT-FT11-* |
| SEC-29 | Segredos de aplicação/API: 256 bits, exibidos uma vez, SHA-256, comparação em tempo constante, até 2 ativos, nunca em respostas de consulta | RN-APP-06 | CT-FT09-* |
| SEC-30 | Privilégios mínimos no banco: papel de migração (DDL) ≠ papel de aplicação (DML); aplicação sem `UPDATE` em `security_event` (exceto `ip`,`user_agent`) | D-26 | CT-OPS-* |
| SEC-31 | Logs JSON com redação (`password`, `*_password`, `token`, `access_token`, `refresh_token`, `code`, `code_verifier`, `state`, `client_secret`, `secret`, `authorization`, `cookie`, `set-cookie`, `bootstrap_token`); e-mail mascarado (`u***@d***.com`) | RNF-06 | CT-RNF-06 |
| SEC-32 | Validação de entrada: tamanhos máximos, formatos, lista permitida para ordenação/filtros, JSON estrito; consultas parametrizadas; limite de corpo P-48 | §8 | CT-SEC-32 |
| SEC-33 | Comparações de segredo em tempo constante (bootstrap, segredos de cliente/API, hashes) | RN-SET-02 | CT-FT01-* |
| SEC-34 | Política de revogação/bloqueio imediato via introspecção; API do Identity valida `sid` no banco | D-10 | CT-FT10-* |
| SEC-35 | Cabeçalhos: `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store` em respostas sensíveis, `X-Frame-Options: DENY`/`frame-ancestors 'none'`, `Permissions-Policy` mínimo | RNF-19 | CT-SEC-HDR |
| SEC-36 | Dependências: versões fixadas, verificação de vulnerabilidades (OWASP Dependency-Check/`npm audit`) no pipeline, imagens base oficiais atualizadas, SBOM opcional | RNF-13 | CT-OPS-* |
| SEC-37 | Tratamento de erros sem stack trace nem detalhes internos; `trace_id` | D-32 | CT-SEC-37 |
| SEC-38 | E-mail: recomenda-se SPF/DKIM/DMARC no domínio remetente (checklist de implantação); sem credenciais no repositório | RN-EML-08 | CT-OPS-* |
| SEC-39 | Backups cifrados em repouso e acesso restrito; após restauração, revogar todas as sessões (runbook) | RNF-12 | CT-OPS-* |
| SEC-40 | Fontes externas de identidade (Google/Facebook) acessadas somente em hosts fixos (sem URL fornecida pelo usuário ⇒ sem SSRF); timeouts 5 s; falhas ⇒ `temporarily_unavailable` | §6 | CT-FT07-* |

## 4. Estratégia de sessão no navegador (resumo comparativo)

| Opção | Escolhida? | Racional |
|---|---|---|
| Access em memória + refresh em cookie HttpOnly `SameSite=Strict` (mesmo site) | **Sim (BROWSER)** | resiste a exfiltração por XSS do refresh; sem armazenamento persistente do access; compatível com telas próprias das aplicações |
| Tokens em `localStorage` | Não | XSS lê credenciais de longa duração |
| Cookie `SameSite=None` entre sites | Não | bloqueio de cookies de terceiros; CSRF mais difícil de mitigar |
| BFF/servidor guarda tokens (cookie de sessão próprio da aplicação) | **Sim (para sites diferentes / SSR)** como `CONFIDENTIAL` | permite domínios distintos |

## 5. Pressupostos de segurança da integração e limites de confiança

1. As aplicações `BROWSER` e o Identity pertencem ao **mesmo site**; se não pertencerem, usar BFF `CONFIDENTIAL`.
2. O administrador do Identity é confiável e **registra** somente aplicações/APIs do mesmo projeto; registrar aplicação de terceiros equivale a confiar nela.
3. Todas as APIs listadas no `aud` de um token **confiam entre si**: o token é *bearer* e pode ser reapresentado por uma API comprometida a outra audiência presente. Para zonas de confiança diferentes, usar aplicações distintas (cada uma com suas audiências).
4. XSS em uma aplicação `BROWSER` permite agir como o usuário enquanto a página está aberta (chamar APIs e refresh); não permite exfiltrar o refresh nem persistir acesso após fechar a página. Mitigação: CSP, dependências, TTL curto, reautenticação em operações sensíveis do Identity.
5. APIs consumidoras validam tokens **localmente** e tratam `sub` como identificador opaco; o e-mail não é identificador.
6. O Identity não autoriza operações de negócio; cada API aplica suas permissões.
7. A rede entre proxy e Identity é privada; o proxy remove `X-Forwarded-*` vindos do cliente e define os próprios (P-47).
8. O operador protege `ACHERNAR_MASTER_KEY` e `ACHERNAR_BOOTSTRAP_TOKEN` (cofre/segredos do orquestrador) e restringe a rede de gestão (porta 9090).

## 6. Riscos residuais aceitos

| ID | Risco | Justificativa |
|---|---|---|
| RR-01 | Access token utilizável por até 10–15 min após revogação | custo/benefício; introspecção para sensíveis |
| RR-02 | Sem MFA nesta versão | fora de escopo (D-50); mitigações: bloqueio, reautenticação, recomendações de rede para admin |
| RR-03 | XSS da aplicação consumidora usa a sessão enquanto aberta | inerente a SPAs |
| RR-04 | Chave-mestra em variável de ambiente (sem HSM/KMS) | fora de escopo; documentado; evoluível |
| RR-05 | E-mail em trânsito/descanso no provedor de SMTP | tokens de curta validade e uso único |
| RR-06 | Bloqueio por tentativas permite DoS temporário a um titular | teto 120 min + recuperação de senha |
