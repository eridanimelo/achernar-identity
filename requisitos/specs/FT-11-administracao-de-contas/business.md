# Business Spec — FT-11 Administração de contas

## 0. Identificação
FT-11 (UC-080…084) · `NEW` · Módulo M7 Administração · `APPROVED` (documental)

## 1. Objetivo
Permitir ao administrador consultar contas, desativar/reativar, conceder/revogar o perfil administrador e anonimizar contas desativadas, preservando sempre ao menos um administrador ativo e auditando tudo.

## 2. História
**Como** administrador **quero** gerir as contas da instalação **para** bloquear acessos indevidos, atender pedidos de privacidade e delegar administração.

## 3. Fluxos
### UC-080 — Consultar contas
Lista paginada com filtros (texto no início do e-mail, estado, perfil, método de acesso, período de criação) e detalhe: dados da conta, vínculos, quantidade de sessões ativas e linha do tempo de eventos recentes. Nunca hashes, segredos ou tokens.
### UC-081 — Desativar conta
Revoga todas as sessões, invalida tokens de uso único, impede login e refresh, notifica o titular (TPL-09). Motivo opcional. Não permite desativar a si mesmo nem o último administrador ativo.
### UC-082 — Reativar conta: volta a `ACTIVE`; sessões não retornam; notifica.
### UC-083 — Conceder/revogar perfil administrador: alvo `ACTIVE` com e-mail verificado; rebaixar exige que reste outro administrador ativo.
### UC-084 — Anonimizar conta desativada: irreversível; exige confirmação digitando o e-mail; remove senha e vínculos, troca o e-mail por valor não entregável único e preserva `sub` e eventos.

## 4. Critérios de aceite
- **CA-FT11-01 — Listagem.** Paginada (25/100), ordenação permitida (`created_at`, `email`, `status`), filtros combináveis; apenas ADMIN verificado no banco; resposta sem dados secretos. Ref.: RF-13, RN-ADM-01, 07
- **CA-FT11-02 — Filtros.** Texto = prefixo do e-mail normalizado (≥ 3 caracteres), estado, perfil, método (`password|google|facebook`), período; combinação por AND; parâmetros inválidos ⇒ `VALIDATION_ERROR`. Ref.: RN-ADM-07
- **CA-FT11-03 — Detalhe.** Dados da conta, vínculos, contagem de sessões ativas e últimos 20 eventos; `NOT_FOUND` para id inexistente. Ref.: RN-ADM-07
- **CA-FT11-04 — Desativar.** Conta `ACTIVE`/`PENDING` ⇒ 204; estado `DISABLED`; sessões revogadas (`ADMIN_DISABLED`); tokens de uso único invalidados; TPL-09; evento `ACCOUNT_DISABLED` com motivo; introspecção passa a `active=false`. Ref.: RN-ADM-02, RN-ACC-12
- **CA-FT11-05 — Efeitos da conta desativada.** Login (senha/social), refresh e recuperação de senha não funcionam; o e-mail continua reservado. Ref.: RN-ACC-12
- **CA-FT11-06 — Auto-desativação proibida.** Admin desativa a si mesmo ⇒ `FORBIDDEN`. Ref.: RN-ADM-04
- **CA-FT11-07 — Último administrador.** Desativar, rebaixar ou anonimizar o último administrador ativo ⇒ `LAST_ADMIN_PROTECTED`, mesmo sob concorrência (dois administradores se desativando mutuamente: ao menos um permanece ativo). Ref.: RN-ADM-03, DM-07
- **CA-FT11-08 — Reativar.** `DISABLED` não anonimizada ⇒ 204, `ACTIVE`, sessões não retornam, TPL-09, evento `ACCOUNT_ENABLED`; conta anonimizada ⇒ `CONFLICT`. Ref.: RN-ADM-05
- **CA-FT11-09 — Conceder perfil.** Alvo `ACTIVE` com e-mail verificado ⇒ 200 com novo perfil; evento `ADMIN_GRANTED`; alvo pendente/desativado ⇒ `CONFLICT`. Ref.: RF-27, RN-ADM-04
- **CA-FT11-10 — Revogar perfil.** Com outro administrador ativo ⇒ perfil `USER`, evento `ADMIN_REVOKED`; o rebaixado perde acesso administrativo imediatamente (verificação no banco), sem precisar de novo login. Ref.: RN-ADM-03, RN-TOK-15
- **CA-FT11-11 — Anonimizar.** Conta `DISABLED` não administradora ativa, com `confirm_email` igual ao e-mail atual ⇒ 204; e-mail substituído por valor único não entregável; senha e vínculos removidos; sessões revogadas (`ANONYMIZED`); `anonymized_at` preenchido; eventos anteriores com e-mail redigido; evento `ACCOUNT_ANONYMIZED`. Ref.: RF-28, RN-ACC-13, RN-ADM-06
- **CA-FT11-12 — Anonimizar exige confirmação.** `confirm_email` ausente ou diferente ⇒ `VALIDATION_ERROR`; conta não desativada ⇒ `CONFLICT`. Ref.: RN-ADM-06
- **CA-FT11-13 — Irreversibilidade.** Conta anonimizada não pode ser reativada, ter senha definida ou receber e-mails; o endereço original fica livre para novo cadastro. Ref.: RN-ACC-13, RN-ACC-02
- **CA-FT11-14 — Controle otimista.** Alterações usam `version`; divergência ⇒ `CONFLICT`. Ref.: D-43
- **CA-FT11-15 — Autorização.** Usuário comum ⇒ `FORBIDDEN`; sem Bearer ⇒ 401; ADMIN com sessão revogada ⇒ `SESSION_REVOKED`. Ref.: RN-ADM-01, SEC-27
- **CA-FT11-16 — Auditoria.** Cada ação registra ator, alvo e resultado; falhas por regra de negócio também registram (outcome `FAILURE`). Ref.: RN-ADM-08
- **CA-FT11-17 — Sem personificação.** Nenhum endpoint permite ao administrador definir senha de outro usuário, ver tokens ou assumir sessões. Ref.: RN-ADM-08
- **CA-FT11-18 — Reautenticação.** Desativar, conceder/revogar perfil e anonimizar exigem reautenticação recente do administrador. Ref.: RN-PWD-08 (extensão INFERRED), SEC-28
- **CA-FT11-19 — Idempotência.** Desativar conta já desativada e reativar conta já ativa ⇒ 204 sem novo efeito (um único evento). Ref.: RNF-20

## 5. Regras: RN-ADM-01…08, RN-ACC-12/13, RN-TOK-10/15, DM-07 · Perfil: ADMIN.
## 6. Dados: ver `api.md`.
## 7. Rastreabilidade: UC-080 → CA-FT11-01…03, 15 · UC-081 → CA-FT11-04…07, 14, 16…19 · UC-082 → CA-FT11-08 · UC-083 → CA-FT11-09, 10 · UC-084 → CA-FT11-11…13.
## 8. Pontos a validar / divergências: nenhum / CA-FT11-18 estende a reautenticação a ações administrativas (decisão registrada em `business-rules.md` RN-ADM-09). LEGACY: `NOT_APPLICABLE`.
## 9. Resultado esperado: administração segura, auditável e sem risco de ficar sem administrador.
