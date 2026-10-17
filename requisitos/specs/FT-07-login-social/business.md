# Business Spec — FT-07 Login social

## 0. Identificação
FT-07 (UC-050, UC-051, UC-052) · `NEW` · Módulo M4 Login social · `APPROVED` (documental). Normativo complementar: `social-login.md`.

## 1. Objetivo
Permitir login e cadastro com Google e Facebook sem nunca vincular contas apenas por coincidência de e-mail, sem expor tokens do provedor e sem que a aplicação consumidora manipule segredos do provedor.

## 2. História
**Como** visitante **quero** entrar com minha conta Google ou Facebook **para** não criar mais uma senha. **Como** operador **quero** que contas existentes não possam ser tomadas por e-mail coincidente.

## 3. Fluxos
### UC-050 — Iniciar login/cadastro social
A aplicação gera `code_verifier`/`code_challenge` (S256) e `state` e leva o navegador ao início do fluxo no Identity com `client_id`, `redirect_uri` registrada, `state`, `code_challenge` e audiências opcionais. O Identity valida e redireciona ao provedor.
### UC-051 — Processar o retorno do provedor
O provedor retorna ao Identity; este valida a transação (uso único, expiração, amarração ao navegador), obtém a identidade do provedor, decide a conta pela árvore de decisão (`social-login.md §4`) e redireciona à `redirect_uri` com um **código próprio** (ou `error`).
### UC-052 — Concluir o login (trocar código)
A aplicação envia `code`, `code_verifier`, `redirect_uri` e `client_id`; o Identity valida e emite tokens e sessão (`amr` = provedor), com refresh em cookie (BROWSER) ou no corpo (CONFIDENTIAL).

## 4. Critérios de aceite
- **CA-FT07-01 — Início válido.** `client_id` ativo, provedor habilitado (global e na aplicação), `redirect_uri` registrada e `code_challenge` S256 ⇒ 302 ao provedor com `state`, `nonce` (Google), PKCE S256 e escopos corretos; cookie de amarração `__Secure-achsoc` (HttpOnly, Secure, SameSite=Lax). Ref.: RF-04, RN-SOC-01, 02, 13
- **CA-FT07-02 — Início inválido não redireciona.** `client_id` desconhecido/inativo, `redirect_uri` não registrada ou parâmetro ausente ⇒ página de erro do Identity (400), **sem** redirecionar; provedor desabilitado ⇒ `PROVIDER_NOT_ENABLED`; `code_challenge_method` ≠ S256 ⇒ erro. Ref.: RN-SOC-02, SEC-16
- **CA-FT07-03 — Limite de início.** 30/min por IP ⇒ 429. Ref.: P-37
- **CA-FT07-04 — Vínculo existente.** Identidade já vinculada a conta `ACTIVE` ⇒ redireciona com `code` e `state` do cliente; `DISABLED` ⇒ `error=account_disabled`; `PENDING_VERIFICATION` ⇒ `error=email_verification_required`. Ref.: RN-SOC-03
- **CA-FT07-05 — Conta nova com e-mail verificado.** Identidade nova, e-mail verificado pelo provedor (Google) e sem conta local ⇒ cria conta `ACTIVE` com e-mail verificado, vínculo e evento `SOCIAL_ACCOUNT_CREATED`; segue com `code`. Ref.: RN-SOC-04
- **CA-FT07-06 — Facebook cria conta pendente.** Identidade nova do Facebook (e-mail não verificado) e sem conta local ⇒ cria conta `PENDING_VERIFICATION` + vínculo, envia TPL-01, `error=email_verification_required`. Ref.: RN-SOC-04, D-20
- **CA-FT07-07 — Sem e-mail.** Provedor não devolve e-mail ⇒ `error=email_unavailable`, nada criado. Ref.: RN-SOC-04
- **CA-FT07-08 — Sem vinculação por e-mail (verificado).** Identidade nova com e-mail verificado igual ao de conta `ACTIVE` ou `DISABLED` ⇒ nada vinculado/alterado, `error=account_link_required`, evento `SOCIAL_LINK_CONFLICT`. Ref.: RN-SOC-05, RN-SOC-08
- **CA-FT07-09 — E-mail não verificado coincidente.** Identidade nova com e-mail não verificado igual ao de conta existente (qualquer estado) ⇒ nada alterado, TPL-05 ao endereço e `error=email_verification_required` (mesma resposta que para e-mail inexistente). Ref.: RN-SOC-05
- **CA-FT07-10 — Substituição de conta pendente.** E-mail verificado pelo provedor igual ao de conta `PENDING_VERIFICATION` ⇒ a conta pendente (e sua credencial) é substituída pela conta social `ACTIVE`; evento `PENDING_ACCOUNT_SUPERSEDED`. Ref.: RN-SOC-06, SEC-20
- **CA-FT07-11 — Transação de uso único.** `state` inexistente ⇒ página de erro; reutilizado ⇒ página de erro; expirado (10 min) ou cookie de amarração ausente/diferente ⇒ redirecionamento com `error=invalid_request`; nada emitido. Ref.: RN-SOC-13, SEC-17
- **CA-FT07-12 — Cancelamento e falhas do provedor.** Negação ⇒ `error=access_denied`; timeout/indisponibilidade (5 s) ⇒ `error=temporarily_unavailable`; ID token inválido (assinatura, `iss`, `aud`, `exp`, `nonce`) ⇒ `error=invalid_request`; eventos `SOCIAL_LOGIN_FAILED`. Ref.: RN-SOC-14
- **CA-FT07-13 — Tokens do provedor nunca vazam.** Não persistidos, não logados, não presentes em URLs, respostas ou eventos. Ref.: RN-SOC-14, SEC-18
- **CA-FT07-14 — Troca do código.** Código válido, `client_id`, `redirect_uri` e `code_verifier` coerentes ⇒ 200 com tokens (mesma forma do login), sessão com `amr` do provedor, evento `SOCIAL_LOGIN_SUCCEEDED`/`TOKEN_ISSUED`. Ref.: RF-04, RF-05, RN-SOC-14
- **CA-FT07-15 — Código inválido.** Inexistente, expirado (60 s), usado, `client_id`/`redirect_uri` diferentes, `code_verifier` errado ⇒ `INVALID_GRANT` (único). Ref.: SEC-18
- **CA-FT07-16 — Reuso do código.** Segundo uso do mesmo código ⇒ `INVALID_GRANT` e revogação da sessão criada pelo primeiro uso. Ref.: SEC-18, RFC 6749 §4.1.2
- **CA-FT07-17 — Concorrência na troca.** Trocas simultâneas do mesmo código ⇒ uma sessão. Ref.: DM-11
- **CA-FT07-18 — Conta ainda ativa.** Conta desativada entre o callback e a troca ⇒ `INVALID_GRANT`. Ref.: RN-ACC-12
- **CA-FT07-19 — Limite da troca.** P-31 por IP. Ref.: P-31
- **CA-FT07-20 — Audiências.** Audiências pedidas seguem RN-APP-17; fora da política ⇒ erro de início. Ref.: RN-APP-17
- **CA-FT07-21 — `state` do cliente devolvido intacto** (≤ 500 caracteres; maior ⇒ erro de início). Ref.: RN-SOC-14
- **CA-FT07-22 — Provedor global desabilitado.** Sem credenciais do provedor na instalação, o botão não aparece (configuração pública) e o início responde `PROVIDER_NOT_ENABLED`. Ref.: RN-APP-11

## 5. Regras: RN-SOC-01…06, 08, 11, 13, 14, RN-APP-11/17, RN-ACC-09/12, SEC-16…20, SEC-40 · Perfis: visitante; aplicação (BROWSER/CONFIDENTIAL).
## 6. Dados: ver `social-login.md`; parâmetros de início: `client_id`, `redirect_uri`, `state`, `code_challenge`, `code_challenge_method`, `audiences`.
## 7. Rastreabilidade: UC-050 → CA-FT07-01…03, 20…22 · UC-051 → CA-FT07-04…13 · UC-052 → CA-FT07-14…19.
## 8. Pontos a validar: nenhum. Premissa A-04 (apps criados nos consoles). Divergências: nenhuma. LEGACY: `NOT_APPLICABLE`.
## 9. Resultado esperado: login social seguro, sem vinculação por e-mail e sem exposição de tokens.
