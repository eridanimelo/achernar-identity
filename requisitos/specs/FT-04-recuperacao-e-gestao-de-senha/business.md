# Business Spec — FT-04 Recuperação e gestão de senha

## 0. Identificação
FT-04 (UC-030, UC-031, UC-032, UC-033, UC-037) · `NEW` · Módulo M2 Contas e credenciais · `APPROVED` (documental)

## 1. Objetivo
Permitir recuperar acesso por e-mail, alterar a senha estando autenticado, definir a primeira senha em conta social e reautenticar o usuário antes de operações sensíveis — sem enumerar contas e encerrando sessões quando o risco exige.

## 2. História
**Como** titular **quero** recuperar minha senha e trocá-la com segurança **para** manter o acesso e reduzir risco após suspeita de vazamento.

## 3. Fluxos
### UC-030 — Solicitar recuperação
Informa e-mail; resposta sempre neutra. Conta `ACTIVE` ⇒ token (30 min, uso único) e e-mail TPL-02, invalidando o anterior. Conta pendente ⇒ reenvia verificação (TPL-01). Desativada/anonimizada/inexistente ⇒ nada. Conta social sem senha também recebe (permite definir a primeira senha).
### UC-031 — Redefinir senha
Com o token (fragmento da URL) e nova senha: valida política, grava hash, revoga **todas** as sessões, zera bloqueio de login, invalida outros tokens de recuperação e de troca de e-mail, notifica (TPL-06). Não inicia sessão.
### UC-032 — Alterar senha (autenticado)
Exige reautenticação recente (ou senha atual correta no próprio pedido), senha nova diferente e dentro da política; revoga as demais sessões, mantém a atual, notifica.
### UC-033 — Reautenticar com senha
Usuário autenticado informa a senha; sucesso abre janela de 5 min registrada na sessão. Falha conta no bloqueio de login.
### UC-037 — Definir senha local em conta social
Conta `ACTIVE` sem senha, com reautenticação social recente (FT-08, UC-055), define senha dentro da política; demais sessões mantidas; notifica.

## 4. Critérios de aceite
- **CA-FT04-01 — Solicitação neutra.** Para e-mail existente ativo, pendente, desativado, anonimizado ou inexistente, a resposta é a mesma (202, corpo igual, tempo equivalente). Ref.: RF-09, RN-PWD-04, SEC-07
- **CA-FT04-02 — Envio somente quando cabível.** Conta ativa ⇒ TPL-02 e evento `PASSWORD_RESET_REQUESTED`; pendente ⇒ TPL-01; demais ⇒ nenhum e-mail. Ref.: RN-PWD-04
- **CA-FT04-03 — Um token ativo.** Nova solicitação invalida a anterior; o token antigo falha. Ref.: D-17, DM-05
- **CA-FT04-04 — Limites.** 3/h por e-mail e 10/h por IP ⇒ resposta 202 sem envio (e-mail) ou `RATE_LIMITED` (IP). Ref.: P-33, RN-EML-03
- **CA-FT04-05 — Redefinição.** Token válido + senha válida ⇒ 204; hash gravado; todas as sessões revogadas (`PASSWORD_RESET`); bloqueio de login zerado; outros tokens de recuperação/troca invalidados; TPL-06; evento `PASSWORD_RESET_COMPLETED`; sem tokens emitidos. Ref.: RF-09, RN-PWD-05, RN-AUT-03
- **CA-FT04-06 — Token inválido.** Inexistente/expirado (30 min)/usado/invalidado/outra finalidade ⇒ `TOKEN_INVALID_OR_EXPIRED`. Ref.: D-17
- **CA-FT04-07 — Política na redefinição.** Senha fora da política ⇒ `PASSWORD_POLICY_VIOLATION` e **token não é consumido**. Ref.: RN-PWD-01
- **CA-FT04-08 — Uso único concorrente.** Duas redefinições simultâneas com o mesmo token ⇒ uma vence. Ref.: DM-11, RNF-20
- **CA-FT04-09 — Conta desativada não é reativada.** Token emitido antes da desativação é invalidado; recuperação não altera o estado. Ref.: RN-ACC-12
- **CA-FT04-10 — Conta social define senha pela recuperação.** Conta ativa sem senha recebe TPL-02 e passa a ter senha; vínculo social permanece. Ref.: RN-PWD-06, RF-18
- **CA-FT04-11 — Alterar senha.** Com reautenticação válida (ou senha atual correta), nova ≠ atual e conforme política ⇒ 204; demais sessões revogadas, a atual mantida; TPL-06; `PASSWORD_CHANGED`. Ref.: RF-10, RN-PWD-02, 03
- **CA-FT04-12 — Alterar exige reautenticação.** Sem janela válida ⇒ `REAUTH_REQUIRED`; senha atual incorreta ⇒ `REAUTH_FAILED` contando no bloqueio. Ref.: RN-PWD-08
- **CA-FT04-13 — Nova senha igual à atual** ⇒ `VALIDATION_ERROR` (campo `new_password`, código `SAME_AS_CURRENT`). Ref.: RN-PWD-02
- **CA-FT04-14 — Reautenticar.** Senha correta ⇒ 204 e janela de 5 min (`REAUTH_SUCCEEDED`); incorreta ⇒ `REAUTH_FAILED`, evento `REAUTH_FAILED`; limite 10/15 min por sessão ⇒ `RATE_LIMITED`. Ref.: RF-30, RN-PWD-08, P-39
- **CA-FT04-15 — Reautenticação de conta sem senha.** Conta só social não tem reautenticação por senha (`VALIDATION_ERROR`); usa o provedor (FT-08). Ref.: RN-SOC-12
- **CA-FT04-16 — Definir senha em conta social.** Conta `ACTIVE` sem senha, com reautenticação social ≤ 5 min ⇒ 204; sessões mantidas; TPL-06; evento `PASSWORD_SET`. Sem reautenticação ⇒ `REAUTH_REQUIRED`. Conta que já tem senha tentando o fluxo sem `current_password` ⇒ `VALIDATION_ERROR`. Ref.: RF-18, RN-PWD-07
- **CA-FT04-17 — Sessão exigida.** Operações autenticadas sem Bearer válido ⇒ 401; com `sid` revogado ⇒ `SESSION_REVOKED`. Ref.: RN-TOK-15
- **CA-FT04-18 — Hash da nova senha.** Argon2id com parâmetros vigentes; senha nunca registrada. Ref.: RNF-01, RN-AUT-11
- **CA-FT04-19 — Link do e-mail.** TPL-02 usa a URL de redefinição da aplicação de origem (ou UI do Identity) com token no fragmento. Ref.: RN-EML-04

## 5. Regras: RN-PWD-01…08, RN-AUT-03, RN-EML-03/04, RN-ACC-12 · Perfis: visitante (UC-030/031); usuário autenticado (UC-032/033/037).
## 6. Dados: e-mail; token; nova senha; senha atual; janela de reautenticação (5 min, P-07).
## 7. Rastreabilidade
UC-030 → CA-FT04-01…04, 10, 19 · UC-031 → CA-FT04-05…09 · UC-032 → CA-FT04-11…13, 17, 18 · UC-033 → CA-FT04-14, 15 · UC-037 → CA-FT04-16.
## 8. Pontos a validar / divergências / LEGACY: nenhum / nenhuma / `NOT_APPLICABLE`.
## 9. Resultado esperado: recuperação e troca seguras, sem enumeração, com revogação de sessões quando devido.
