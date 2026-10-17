# Business Spec — FT-08 Vínculos sociais

## 0. Identificação
FT-08 (UC-053, UC-054, UC-055) · `NEW` · Módulo M4 · `APPROVED` (documental). Normativo complementar: `social-login.md §6–7`.

## 1. Objetivo
Permitir que o titular vincule e desvincule Google/Facebook à sua conta autenticada e reautentique-se por um provedor já vinculado, sem nunca perder o último método de acesso.

## 2. História
**Como** titular **quero** adicionar ou remover provedores **para** ter mais formas de entrar, sem arriscar ficar sem acesso.

## 3. Fluxos
### UC-053 — Vincular provedor
Exige sessão e reautenticação recente. O usuário pede o vínculo; o sistema devolve uma URL de autorização; o fluxo no provedor retorna ao Identity, que cria o vínculo se a identidade não pertencer a outra conta, e devolve o usuário à aplicação (`result=linked`). Notifica por e-mail (TPL-10).
### UC-054 — Desvincular provedor
Exige reautenticação recente e que a conta mantenha outro método de acesso (senha ou outro vínculo).
### UC-055 — Reautenticar com provedor
Conta com vínculo reautentica-se pelo provedor (apenas a identidade já vinculada **à própria conta** vale); sucesso abre janela de 5 min.

## 4. Critérios de aceite
- **CA-FT08-01 — Intenção de vínculo.** Com Bearer e reautenticação válida, `POST …/social-links/{provider}/intent` devolve `authorization_url` (expira em 120 s) vinculada à conta e à sessão; sem reautenticação ⇒ `REAUTH_REQUIRED`; provedor desabilitado ⇒ `PROVIDER_NOT_ENABLED`; `return_url` fora das `redirect_uris` da aplicação da sessão ⇒ `REDIRECT_URI_NOT_ALLOWED`. Ref.: RF-17, RN-SOC-07
- **CA-FT08-02 — Vínculo concluído.** Fluxo concluído pelo mesmo usuário ⇒ vínculo criado; redireciona `return_url?result=linked&state=…`; TPL-10; evento `SOCIAL_LINKED`; o e-mail do provedor não é usado. Ref.: RN-SOC-07, 08, 11
- **CA-FT08-03 — Identidade já vinculada a outra conta** ou conta já vinculada ao provedor ⇒ `error=already_linked`, nada alterado. Ref.: RN-SOC-07, `SOCIAL_ALREADY_LINKED`
- **CA-FT08-04 — Sessão da transação.** Se a sessão foi revogada ou pertence a outra conta no retorno ⇒ `error=invalid_request`, nada alterado. Ref.: RN-SOC-13
- **CA-FT08-05 — Sem vínculo por e-mail.** Mesmo e-mail em outra conta não vincula nem impede o vínculo à conta autenticada. Ref.: RN-SOC-08
- **CA-FT08-06 — Desvincular.** Com reautenticação e outro método disponível ⇒ 204; vínculo removido; TPL-10; evento `SOCIAL_UNLINKED`; sessões mantidas. Ref.: RF-18, RN-SOC-09
- **CA-FT08-07 — Último método.** Conta sem senha e com um único vínculo ⇒ `LAST_ACCESS_METHOD`, nada alterado. Ref.: RN-ACC-05, DM-08
- **CA-FT08-08 — Desvincular inexistente** ⇒ `NOT_FOUND`. Ref.: RN-ACC-04
- **CA-FT08-09 — Concorrência.** Desvincular dois provedores simultaneamente em conta sem senha nunca deixa a conta sem método; vincular o mesmo provedor duas vezes cria um vínculo. Ref.: DM-03, DM-08, RNF-20
- **CA-FT08-10 — Intenção de reautenticação.** `POST …/reauth/social/{provider}/intent` (Bearer) devolve URL que força reautenticação no provedor (`prompt=login`/`auth_type=reauthenticate`). Ref.: RF-30
- **CA-FT08-11 — Reautenticação válida.** A identidade devolvida é vínculo da própria conta ⇒ `last_reauth_at` atualizado, `result=reauthenticated`, evento `REAUTH_SUCCEEDED(method=provider)`. Ref.: RN-SOC-12, RN-PWD-08
- **CA-FT08-12 — Reautenticação por identidade alheia.** Identidade diferente ou de outra conta ⇒ `error=reauth_failed`, janela não atualizada, evento `REAUTH_FAILED`. Ref.: RN-SOC-12
- **CA-FT08-13 — Conta desativada durante o fluxo** ⇒ `error=account_disabled`/`invalid_request`, nada alterado. Ref.: RN-ACC-12
- **CA-FT08-14 — Limite.** 30/min por IP no início. Ref.: P-37
- **CA-FT08-15 — Consulta de vínculos.** `GET /api/v1/account` (FT-05) lista os vínculos; a UI reflete o resultado do retorno. Ref.: RF-31

## 5. Regras: RN-SOC-07…12, RN-ACC-04/05, RN-PWD-08 · Perfil: usuário autenticado.
## 6. Dados: provedor (`google|facebook`), `return_url`, `state` do cliente.
## 7. Rastreabilidade: UC-053 → CA-FT08-01…05, 09, 13, 14 · UC-054 → CA-FT08-06…09 · UC-055 → CA-FT08-10…13 · CA-FT08-15 → RF-31.
## 8. Pontos a validar / divergências / LEGACY: nenhum / nenhuma / `NOT_APPLICABLE`.
## 9. Resultado esperado: vínculos controlados pelo titular, sem tomada de conta e sem perda de acesso.
