# Business Spec — FT-05 Conta e alteração de e-mail

## 0. Identificação
FT-05 (UC-034, UC-035, UC-036) · `NEW` · Módulo M2 · `APPROVED` (documental)

## 1. Objetivo
Permitir ao titular consultar sua conta (dados, métodos de acesso, vínculos, perfil) e trocar o e-mail com verificação do novo endereço, preservando a identidade (`sub`) e a segurança.

## 2. História
**Como** titular **quero** ver meus dados e atualizar meu e-mail **para** manter meu acesso quando meu endereço mudar.

## 3. Fluxos
### UC-034 — Consultar minha conta
Usuário autenticado vê: identificador, e-mail e se está verificado, estado, perfil, idioma, se possui senha, vínculos sociais (provedor, data) e data de criação. Nunca hashes ou segredos.
### UC-035 — Solicitar alteração de e-mail
1. Exige reautenticação recente.
2. Informa o novo e-mail. A resposta é sempre neutra (202).
3. Se o e-mail é válido e diferente do atual, é criado um token (1 h) — substituindo qualquer pedido pendente — e enviado ao **novo** endereço (TPL-03) e um aviso ao endereço **atual** (TPL-04). Se o novo e-mail já pertence a outra conta, nenhum e-mail é enviado ao terceiro.
### UC-036 — Confirmar alteração
1. O titular abre o link no novo endereço (token no fragmento) e a aplicação envia o token.
2. O sistema consome o token, revalida a unicidade do endereço, grava o novo e-mail (verificado), revoga todas as sessões (RN-ACC-15), invalida tokens de recuperação pendentes e notifica o endereço antigo (TPL-07).
Exceções: token inválido/expirado/usado ⇒ erro único; endereço tomado por outra conta nesse meio-tempo ⇒ erro único sem alterar nada.

## 4. Critérios de aceite
- **CA-FT05-01 — Consulta da conta.** Com Bearer válido, retorna id, e-mail, `email_verified`, estado, perfil, idioma, `has_password`, vínculos (`provider`, `linked_at`) e `created_at`; sem hash/segredo; `Cache-Control: no-store`. Ref.: RF-31, RN-ACC-04
- **CA-FT05-02 — Autenticação da consulta.** Sem Bearer ⇒ 401; token válido de sessão revogada ⇒ `SESSION_REVOKED`; conta desativada ⇒ 401/`SESSION_REVOKED`; sem a audiência do Identity ⇒ 401. Ref.: RN-TOK-15, D-46
- **CA-FT05-03 — Solicitação exige reautenticação.** Sem janela válida ⇒ `REAUTH_REQUIRED`. Ref.: RN-ACC-14, RN-PWD-08
- **CA-FT05-04 — Solicitação neutra.** Novo e-mail livre, ocupado por outra conta, igual ao atual ou formato válido ⇒ mesma resposta 202 (inválido ⇒ 400). Ref.: D-13, SEC-07
- **CA-FT05-05 — Envio.** Novo e-mail livre ⇒ TPL-03 ao novo e TPL-04 ao atual; ocupado ⇒ nenhum e-mail ao terceiro (TPL-04 ao atual é enviado igualmente); igual ao atual ⇒ nenhum e-mail. Ref.: RN-EML-01, RN-ACC-14
- **CA-FT05-06 — Pedido pendente único.** Nova solicitação invalida a anterior; o token antigo falha. Ref.: RN-ACC-14, DM-05
- **CA-FT05-07 — Limite.** 3 solicitações/dia por conta; excedido ⇒ `RATE_LIMITED`. Ref.: P-35
- **CA-FT05-08 — Confirmação.** Token válido e endereço livre ⇒ 204; e-mail da conta alterado, verificado; todas as sessões revogadas (`EMAIL_CHANGED`); tokens de recuperação pendentes invalidados; TPL-07 ao antigo; evento `EMAIL_CHANGED`; o `sub` não muda. Ref.: RF-11, RN-ACC-14, RN-ACC-15
- **CA-FT05-09 — Token inválido.** Inexistente/expirado (1 h)/usado/invalidado/outra finalidade ⇒ `TOKEN_INVALID_OR_EXPIRED`. Ref.: D-17
- **CA-FT05-10 — Unicidade revalidada.** Se outra conta passou a ter o endereço, a confirmação falha com `TOKEN_INVALID_OR_EXPIRED` sem efeito. Ref.: RN-ACC-02
- **CA-FT05-11 — Concorrência.** Duas confirmações simultâneas ⇒ uma vence; duas contas pedindo o mesmo novo e-mail ⇒ só a primeira confirmação vence. Ref.: DM-01, DM-11
- **CA-FT05-12 — Conta social.** Conta só social pode trocar o e-mail (reautenticando pelo provedor); o vínculo social não muda. Ref.: RN-SOC-11
- **CA-FT05-13 — Validação.** E-mail inválido/ > 254 ⇒ `VALIDATION_ERROR`. Ref.: RN-ACC-02
- **CA-FT05-14 — Pedido não reserva o endereço.** Enquanto pendente, outro usuário pode cadastrar o endereço. Ref.: RN-ACC-02

## 5. Regras: RN-ACC-02, 04, 14, 15, RN-PWD-08, RN-SOC-11, RN-EML-01 · Perfis: usuário autenticado (UC-034/035); titular via link (UC-036).
## 6. Dados: novo e-mail, token, conta (campos listados em UC-034).
## 7. Rastreabilidade
UC-034 → CA-FT05-01, 02 · UC-035 → CA-FT05-03…07, 12, 13, 14 · UC-036 → CA-FT05-08…11.
## 8. Pontos a validar / divergências / LEGACY: nenhum / ver RN-ACC-15 (esclarecimento de D-47) / `NOT_APPLICABLE`.
## 9. Resultado esperado: troca de e-mail comprovada, sem enumeração e com revogação de sessões.
