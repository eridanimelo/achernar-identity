# Achernar Identity — Especificação do Login Social (Google e Facebook)

Normativo para FT-07 e FT-08. Decisões: D-19, D-20, D-21, D-22, D-45. Regras: RN-SOC-01…14. Controles: SEC-16…20, SEC-40.

## 1. Visão geral

O **backend do Identity** é o cliente OAuth do provedor (confidencial). O navegador do usuário só visita o provedor e volta ao Identity; a aplicação consumidora
recebe um **código próprio de uso único** e o troca por tokens do Identity. Tokens do Google/Facebook são usados apenas para obter a identidade e **descartados**.

| Propósito (`purpose`) | Quem inicia | Resultado |
|---|---|---|
| `LOGIN` | visitante (aplicação `BROWSER` ou `CONFIDENTIAL`) | login (ou criação de conta) + código de troca |
| `LINK` | usuário autenticado com reautenticação recente | vínculo do provedor à própria conta |
| `REAUTH` | usuário autenticado | atualiza `session.last_reauth_at` |

## 2. Configuração dos provedores

| Item | Google (OpenID Connect) | Facebook (OAuth 2.0 + Graph API) |
|---|---|---|
| Habilitação | `ACHERNAR_GOOGLE_CLIENT_ID/SECRET` | `ACHERNAR_FACEBOOK_APP_ID/SECRET` (+ `_GRAPH_VERSION`) |
| Endpoints | obtidos do documento de descoberta do Google (`https://accounts.google.com/.well-known/openid-configuration`) | autorização/token no host do Facebook para a versão do Graph configurada |
| Escopos | `openid email profile` | `public_profile email` |
| Identidade | `sub` do ID token | `id` retornado por `GET /me?fields=id,email,name` (`appsecret_proof` obrigatório) |
| E-mail verificado? | `email_verified == true` ⇒ verificado | **não** tratado como verificado (D-20) |
| PKCE | S256 | S256 (enviado; se ignorado pelo provedor, `state` + segredo + cookie continuam protegendo) |
| `nonce` | sim | não aplicável |
| Reautenticação forçada | `prompt=login` + `max_age=0` | `auth_type=reauthenticate` |
| Seleção de conta | `prompt=select_account` (LOGIN/LINK) | — |
| URL de retorno registrada no provedor | `<ACHERNAR_ISSUER>/auth/social/google/callback` | `<ACHERNAR_ISSUER>/auth/social/facebook/callback` |
| Biblioteca | Spring Security OAuth2 Client (ClientRegistration; resolvedor de authorization request customizado; repositório de requests em PostgreSQL); validação de ID token por `JwtDecoder` com JWKS do Google | Spring Security OAuth2 Client (`CommonOAuth2Provider.FACEBOOK` ajustado à versão do Graph) |

Checklist de implantação (A-04): criar apps nos consoles; registrar URL de retorno; política de privacidade e URL de instruções de exclusão de dados (exigida pelo Facebook) apontando para a documentação do operador/projeto; publicar o app (modo “live”) antes de produção.

## 3. Fluxo `LOGIN` (normativo)

```mermaid
sequenceDiagram
  autonumber
  participant U as Navegador
  participant APP as Aplicação (SPA/BFF)
  participant ID as Identity
  participant P as Provedor (Google/Facebook)
  APP->>APP: gera code_verifier (≥43 chars) e code_challenge=S256; gera state do cliente
  APP->>U: navega para ID /auth/social/{p}/start?client_id&redirect_uri&state&code_challenge&code_challenge_method=S256[&audiences]
  U->>ID: GET start
  ID->>ID: valida client_id ativo, provedor habilitado (global+app), redirect_uri exata; cria social_transaction (state, nonce, PKCE do provedor); Set-Cookie achsoc (Lax)
  ID-->>U: 302 → autorização do provedor (state, nonce, code_challenge, scopes)
  U->>P: autentica e consente
  P-->>U: 302 → ID /auth/social/{p}/callback?code&state
  U->>ID: GET callback (cookie achsoc)
  ID->>ID: valida state, expiração, uso único (consome), cookie de amarração
  ID->>P: troca code (server→server, segredo + PKCE)
  P-->>ID: tokens do provedor
  ID->>ID: valida ID token (Google) ou consulta Graph (Facebook); descarta tokens; decide conta (§4)
  ID-->>U: 302 → redirect_uri?code=<código Identity>&state=<state do cliente>
  U->>APP: carrega redirect_uri
  APP->>ID: POST /auth/social/exchange {client_id, code, code_verifier, redirect_uri}
  ID->>ID: valida código (uso único, 60 s, client+redirect+PKCE), cria sessão, emite tokens
  ID-->>APP: 200 tokens (+ Set-Cookie refresh se BROWSER)
```

Notas normativas:
- O `state` do **cliente** (≤ 500 chars) é apenas devolvido; o `state` do **provedor** é interno e aleatório. O cliente DEVE comparar o `state` devolvido.
- `audiences` (opcional, lista separada por espaço) segue RN-APP-17.
- Erros antes de validar `client_id`/`redirect_uri`: **página de erro do Identity (HTML, 400)** — nunca redirecionar.
- O redirecionamento final usa construtor de URI seguro (acrescenta `code`/`error`/`state` aos parâmetros existentes) e só para `redirect_uri` registrada.
- Respostas de `start`/`callback`: `Cache-Control: no-store`; `Referrer-Policy: no-referrer`.

## 4. Decisão de conta no callback `LOGIN`

```text
identidade = (provider, provider_subject) · email_social · verificado = e-mail verificado pelo provedor (Google email_verified=true)
│
├─ existe social_link(identidade)?
│    └─ sim ─ conta ACTIVE → OK (código de troca)
│             DISABLED → error=account_disabled · PENDING_VERIFICATION → error=email_verification_required
│
└─ não (identidade nova)
     ├─ sem email_social ............................................ error=email_unavailable
     ├─ verificado = SIM
     │    ├─ existe conta com o mesmo email_normalized
     │    │     ├─ ACTIVE ou DISABLED .......... error=account_link_required (NÃO vincula; evento SOCIAL_LINK_CONFLICT)
     │    │     └─ PENDING_VERIFICATION ........ substitui a conta pendente (evento PENDING_ACCOUNT_SUPERSEDED) e cria conta social ACTIVE → OK
     │    └─ não existe conta ..................... cria conta ACTIVE (e-mail verificado) + social_link → OK
     └─ verificado = NÃO (ex.: Facebook)
          ├─ existe conta com o mesmo email_normalized (qualquer estado)
          │        ... não altera nada; envia TPL-05 ao endereço; error=email_verification_required
          └─ não existe conta ..... cria conta PENDING_VERIFICATION + social_link + TPL-01 → error=email_verification_required
```

Racional: o e-mail só decide o caminho quando o **provedor o garante**. Com e-mail não verificado a resposta ao navegador é a mesma
(`email_verification_required`) exista ou não conta, evitando enumeração e pré-sequestro; o titular real do endereço recebe a mensagem (TPL-01 ou TPL-05).
Após verificar o e-mail da conta social pendente, o usuário repete o login social (vínculo já existe) ou define senha por “Esqueci a senha”.
`account_link_required` só é devolvido a quem acaba de provar, via provedor, controle do e-mail (Google verificado); a conta existente continua inalterada.

## 5. Troca do código (`POST /auth/social/exchange`)

Entradas: `client_id` (+ Basic para `CONFIDENTIAL`), `code`, `code_verifier` (43–128 chars `[A-Za-z0-9-._~]`), `redirect_uri`. Validações, nesta ordem: cliente; limite P-31; código existe (hash); `consumed_at` nulo e `expires_at` futuro; `client_id` e `redirect_uri` iguais aos do código; `BASE64URL(SHA256(code_verifier)) = code_challenge`; consumo atômico; conta ainda `ACTIVE`; cria sessão (`amr`=provedor); emite tokens; evento `SOCIAL_LOGIN_SUCCEEDED`. Qualquer falha ⇒ `400 INVALID_GRANT` (corpo único). Se um código **já consumido** for reapresentado, revoga a sessão criada por ele (`REUSE_DETECTED`) e registra `SOCIAL_LOGIN_FAILED(reason=code_reuse)`.

## 6. Fluxos `LINK` e `REAUTH`

1. Front chama `POST /api/v1/account/social-links/{provider}/intent` (LINK; exige Bearer **e reautenticação ≤ P-07**) ou `POST /api/v1/account/reauth/social/{provider}/intent` (REAUTH; Bearer).
   Corpo: `{ "return_url": "<uma redirect_uri registrada da aplicação da sessão>" , "state": "…" }`. Resposta: `{ "authorization_url": "<issuer>/auth/social/{p}/start?intent=<opaco>", "expires_in": 120 }`.
2. O SPA navega para `authorization_url`; `start` (com `intent`) liga a transação ao navegador (cookie) e redireciona ao provedor (com `prompt=login`/`auth_type=reauthenticate` em REAUTH).
3. `callback` valida como em `LOGIN`; além disso: a sessão da transação deve continuar ativa e pertencer à `account_id`.
   - `LINK`: identidade já vinculada a **outra** conta ⇒ `error=already_linked`; conta já tem vínculo desse provedor ⇒ `error=already_linked`; caso contrário cria `social_link` e redireciona `return_url?result=linked&state=…`, evento `SOCIAL_LINKED`, TPL-10.
   - `REAUTH`: a identidade deve ser um vínculo existente **da própria conta** ⇒ `session.last_reauth_at = now`, `?result=reauthenticated`; caso contrário `error=reauth_failed`.
4. O e-mail do provedor **não** é comparado nem usado para decidir (RN-SOC-08/11).

## 7. Desvincular e senha local

- `DELETE /api/v1/account/social-links/{provider}`: reautenticação recente; falha com `409 LAST_ACCESS_METHOD` se a conta ficaria sem senha e sem outro vínculo. TPL-10; `SOCIAL_UNLINKED`.
- Conta sem senha: **definir senha local** (UC-037) por `POST /api/v1/account/password` sem `current_password`, exigindo reautenticação **social** ≤ P-07; ou via recuperação de senha por e-mail (RN-PWD-06).

## 8. Falhas e cancelamentos

| Situação | Tratamento |
|---|---|
| Usuário cancela / nega consentimento (`error=access_denied` do provedor) | redirect `error=access_denied` + `state`; evento `SOCIAL_LOGIN_FAILED(reason=denied)` |
| `state` inexistente | página de erro do Identity; evento (reason=unknown_state) |
| `state` expirado | redirect `error=invalid_request` (redirect já validada) |
| `state` já consumido (replay) | página de erro; evento `SOCIAL_LOGIN_FAILED(reason=replay)` |
| Cookie de amarração ausente/diferente | redirect `error=invalid_request`; transação consumida; evento (reason=binding) |
| Falha/timeout do provedor (5 s) ou ID token inválido (assinatura, `iss`, `aud`, `exp`, `nonce`) | redirect `error=temporarily_unavailable` (rede) ou `invalid_request` (token inválido); sem detalhes |
| Provedor não devolve e-mail | `error=email_unavailable` |
| Conta desativada | `error=account_disabled` |
| Código de troca inválido | `400 INVALID_GRANT` |
| Limite de taxa | `429` (start) / página de erro com `Retry-After` |

## 9. Ameaças específicas e mitigação

| Ameaça | Mitigação |
|---|---|
| Login CSRF (atacante injeta seu `code`/`state` na vítima) | `state` ligado a cookie de amarração por navegador; transação de uso único |
| Injeção/interceptação de código de autorização | PKCE ao provedor; código só aceito no callback do Identity; segredo no backend |
| Interceptação do **código do Identity** na URL de retorno | uso único, 60 s, vinculado a PKCE do cliente (`code_verifier` só no cliente), `redirect_uri` exata |
| Open redirect | `redirect_uri` registrada e comparada exatamente; erros pré-validação não redirecionam |
| Vinculação por e-mail / pré-sequestro | RN-SOC-05/06/08; vínculo exige reautenticação + prova no provedor |
| Reuso do `state` | consumo atômico |
| Conta social sem e-mail verificado | Facebook ⇒ pendente + verificação própria |
| Vazamento de tokens do provedor | nunca persistidos, nunca logados, nunca em URL |
