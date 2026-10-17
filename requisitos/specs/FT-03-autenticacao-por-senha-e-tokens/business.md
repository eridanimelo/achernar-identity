# Business Spec — FT-03 Autenticação por senha e tokens

## 0. Identificação
FT-03 (UC-020, UC-021, UC-022) · `NEW` · Módulo M3 Autenticação e tokens · `APPROVED` (documental)

## 1. Objetivo
Autenticar por e-mail e senha, emitir access token JWT e refresh token rotativo vinculados a uma sessão por aplicação, renovar a sessão com segurança (inclusive com várias abas) e encerrá-la.

## 2. História
**Como** usuário **quero** entrar e permanecer autenticado sem reenviar a senha **para** usar a aplicação com segurança. **Como** equipe da aplicação **quero** tokens verificáveis sem consultar o Identity a cada chamada.

## 3. Fluxos

### UC-020 — Autenticar com e-mail e senha
1. A aplicação envia `client_id`, e-mail, senha e, opcionalmente, as audiências desejadas.
2. O sistema verifica limites por IP, bloqueio por e-mail, aplicação ativa/origem, credenciais, estado da conta e audiências permitidas.
3. Cria uma sessão (registrando método `pwd`, instante, IP e agente resumidos), aplicando o limite de sessões ativas.
4. Emite access token e refresh token. Aplicação `BROWSER`: refresh só em cookie. `CONFIDENTIAL`: refresh no corpo.
Alternativas: A1 hash antigo ⇒ regravado; A2 sem audiências pedidas ⇒ usa as padrão da aplicação.
Exceções: E1 credenciais inválidas, conta inexistente ou bloqueada ⇒ mesma resposta; E2 conta pendente (senha correta) ⇒ `EMAIL_NOT_VERIFIED`; E3 conta desativada (senha correta) ⇒ `ACCOUNT_DISABLED`; E4 audiência não permitida; E5 excesso de tentativas por IP.

### UC-021 — Renovar sessão
1. A aplicação apresenta o refresh (cookie ou corpo) e, opcionalmente, subconjunto de audiências.
2. O sistema valida sessão, aplicação, conta, expiração e cliente; rotaciona o refresh e emite novo access.
3. Reuso do antecessor imediato dentro da tolerância ⇒ novo access sem novo refresh. Reuso fora da tolerância ⇒ revoga a sessão.
Exceções: refresh inválido/expirado/revogado ⇒ mesmo erro; teto absoluto atingido ⇒ novo login.

### UC-022 — Encerrar a sessão atual
Apresenta o refresh; o sistema revoga a sessão e, no navegador, limpa o cookie. Sempre sucesso (idempotente).

## 4. Critérios de aceite

- **CA-FT03-01 — Login com sucesso (BROWSER).** Dado conta `ACTIVE`, aplicação `BROWSER` ativa, origem permitida e credenciais corretas, Quando login, Então 200 com access token, `session_id`, `expires_in`, `audiences`, **sem** `refresh_token` no corpo e com cookie `__Secure-achrt_<client_id>` (HttpOnly, Secure, SameSite=Strict, Path=/auth); sessão criada; evento `LOGIN_SUCCEEDED` e `TOKEN_ISSUED`. Ref.: RF-03, RF-05, RN-AUT-01, 04, 06, 08
- **CA-FT03-02 — Login com sucesso (CONFIDENTIAL).** Dado cliente com segredo válido (Basic), Quando login, Então o refresh vem no corpo e não há cookie. Ref.: RN-AUT-04
- **CA-FT03-03 — Respostas idênticas.** Dado e-mail inexistente, senha errada e conta bloqueada, Quando login, Então `401 INVALID_CREDENTIALS` idêntico e tempo equivalente (≤ 15 % de diferença). Ref.: RN-AUT-02, RNF-16, D-48
- **CA-FT03-04 — Bloqueio por tentativas.** Dado 5 falhas em 15 min para o mesmo e-mail normalizado, Quando nova tentativa (inclusive correta), Então `INVALID_CREDENTIALS` e `ACCOUNT_LOCKED_THROTTLE` registrado; bloqueio de 15 min, dobrando (30/60/120) em reincidência em 24 h; e-mail TPL-08 ao titular existente. Ref.: RN-AUT-03, P-30
- **CA-FT03-05 — Estados da conta.** Conta pendente com senha correta ⇒ `EMAIL_NOT_VERIFIED`; desativada ⇒ `ACCOUNT_DISABLED`; ambos sem tokens e só após provar a senha. Ref.: RN-ACC-09, 12, SEC-08
- **CA-FT03-06 — Cliente e origem.** `client_id` desconhecido/inativo ⇒ `INVALID_CLIENT` (indistinguível); segredo errado (CONFIDENTIAL) ⇒ `INVALID_CLIENT`; origem fora da lista ⇒ `ORIGIN_NOT_ALLOWED`. Ref.: RN-AUT-04, RN-APP-05
- **CA-FT03-07 — Audiências.** Audiência não permitida/ inativa ⇒ `AUDIENCE_NOT_ALLOWED`; sem pedido ⇒ padrão da aplicação; `aud` do token = interseção + audiência do Identity se `identity_api_access`. Ref.: RN-AUT-05, RN-APP-17
- **CA-FT03-08 — Conteúdo do access token.** Token `typ=at+jwt`, RS256, `kid` presente, claims `iss, sub, aud[], exp, iat, nbf, jti, sid, azp, amr, auth_time`; `roles` somente se a audiência do Identity está presente; validade = P-01 ou TTL da aplicação (5–15 min). Ref.: RN-TOK-01, D-05
- **CA-FT03-09 — Limite de sessões.** Dado 10 sessões ativas, Quando novo login, Então a menos recente é revogada com motivo `SESSION_LIMIT`. Ref.: RN-AUT-06, P-05
- **CA-FT03-10 — Rehash.** Hash com parâmetros antigos é regravado após login correto. Ref.: RN-AUT-07
- **CA-FT03-11 — Limite por IP.** 20/min ou 200/h por IP ⇒ `RATE_LIMITED` + `Retry-After`. Ref.: P-31
- **CA-FT03-12 — Cabeçalhos.** Respostas com tokens têm `Cache-Control: no-store`; CORS com credenciais apenas nas rotas previstas. Ref.: RN-AUT-08, SEC-35
- **CA-FT03-13 — Refresh rotativo.** Dado refresh válido, Quando renova, Então novo access e novo refresh (cookie/corpo); o anterior fica rotacionado; `refresh_rotated=true`; validade = mín(agora+P-02, criação da sessão+P-03). Ref.: RF-06, RN-TOK-02, 05
- **CA-FT03-14 — Tolerância de reuso.** Dado reuso do antecessor imediato em ≤ 10 s, Então novo access **sem** novo refresh, `refresh_rotated=false`, evento `REFRESH_REUSE_TOLERATED`. Ref.: RN-TOK-04
- **CA-FT03-15 — Reuso detectado.** Dado reuso fora da tolerância ou de token mais antigo que o antecessor, Então `INVALID_REFRESH_TOKEN`, sessão revogada (`REUSE_DETECTED`), evento `REFRESH_REUSE_DETECTED`. Ref.: RN-TOK-03
- **CA-FT03-16 — Falhas de refresh.** Ausente, desconhecido, expirado, revogado, de outro `client_id`, conta ≠ `ACTIVE`, aplicação inativa ⇒ `INVALID_REFRESH_TOKEN` (único código). Ref.: RN-TOK-06, 08
- **CA-FT03-17 — Subconjunto de audiências.** Pedido ⊆ audiências da sessão é atendido; ampliar ⇒ `AUDIENCE_NOT_ALLOWED`; API desativada omitida. Ref.: RN-TOK-07
- **CA-FT03-18 — Teto absoluto.** Em P-03 após a criação da sessão, o refresh falha e é exigido novo login. Ref.: RN-TOK-05
- **CA-FT03-19 — Defesa CSRF do refresh no navegador.** Sem `Origin` permitido, sem `X-Achernar-Client` igual ao cliente do cookie ou sem JSON ⇒ recusa sem efeito. Ref.: SEC-14
- **CA-FT03-20 — Concorrência de refresh.** Duas renovações simultâneas com o mesmo refresh ⇒ uma rotaciona; a outra recebe a resposta tolerada; nunca dois refreshes correntes. Ref.: RN-TOK-04, DM-04, RNF-20
- **CA-FT03-21 — Logout.** Com refresh válido, inválido ou ausente ⇒ `204`; sessão revogada quando válido; cookie limpo; evento `LOGOUT`. Ref.: RF-07, RN-AUT-09
- **CA-FT03-22 — Revogação não invalida access emitido.** Após logout, o access segue válido até `exp` para APIs que não introspectam; a API do Identity o recusa (`SESSION_REVOKED`). Ref.: RN-TOK-09, 15
- **CA-FT03-23 — Logs.** Corpo de login/senha/tokens jamais em logs ou eventos. Ref.: RN-AUT-11, RNF-06
- **CA-FT03-24 — Login conta como reautenticação.** Após login, operações que exigem reautenticação nos próximos 5 min não a pedem. Ref.: RN-AUT-10

## 5. Regras
RN-AUT-01…11, RN-TOK-01…08, 15, RN-SES-01, RN-ACC-09/12, RN-APP-17 (em `business-rules.md`).

## 6. Dados funcionais
`client_id`, e-mail, senha, audiências (lista opcional), refresh (cookie/corpo), cabeçalho `X-Achernar-Client`. Resposta: access token, `expires_in`, `session_id`, `audiences`, `refresh_*` (CONFIDENTIAL), `refresh_rotated`.

## 7. Perfis: visitante (login), titular da sessão (refresh/logout). Aplicações `BROWSER` e `CONFIDENTIAL`.
## 8. Rastreabilidade
UC-020 → RF-03, 05, 16 · CA-FT03-01…12, 22…24 · UC-021 → RF-06 · CA-FT03-13…20 · UC-022 → RF-07 · CA-FT03-21, 22.
## 9. Pontos a validar: nenhum. Divergências: nenhuma. LEGACY: `NOT_APPLICABLE`.
## 10. Resultado esperado
Autenticação robusta contra enumeração, força bruta, roubo de refresh e corridas entre abas.
