# Business Spec — FT-06 Sessões do usuário

## 0. Identificação
FT-06 (UC-040, UC-041, UC-042) · `NEW` · Módulo M3 · `APPROVED` (documental)

## 1. Objetivo
Dar ao titular visibilidade e controle sobre suas sessões ativas: listar, revogar uma e encerrar todas.

## 2. História
**Como** titular **quero** ver onde estou autenticado e encerrar sessões **para** reagir a perda de dispositivo ou suspeita de acesso indevido.

## 3. Fluxos
### UC-040 — Listar minhas sessões
Lista as sessões ativas da conta (não revogadas e não expiradas): aplicação (nome), criada em, último uso, IP aproximado (truncado), agente resumido, método de autenticação e marca “esta sessão”. Ordenadas pelo último uso (mais recente primeiro).
### UC-041 — Revogar uma sessão
O titular escolhe uma sessão e a revoga. Se for a atual, equivale a logout (o cookie é limpo pelo cliente). Sessão inexistente ou de outra conta ⇒ não encontrada.
### UC-042 — Encerrar todas as sessões
Exige reautenticação recente. Revoga todas as sessões da conta, com opção de manter a atual. Notifica por e-mail (TPL-11).

## 4. Critérios de aceite
- **CA-FT06-01 — Listagem.** Retorna apenas sessões ativas da própria conta, com `id`, aplicação (`client_id` e nome), `created_at`, `last_used_at`, `ip_approx`, `user_agent_summary`, `amr`, `current`; ordem por último uso decrescente; sem tokens/hashes. Ref.: RF-12, RN-SES-01, 02
- **CA-FT06-02 — Isolamento.** Nenhuma sessão de outra conta aparece; consultar/revogar id de outra conta ⇒ `NOT_FOUND` (mesma resposta que id inexistente). Ref.: RN-SES-04
- **CA-FT06-03 — Revogar uma.** Sessão própria ativa ⇒ 204; sessão marcada revogada (`USER`); seu refresh deixa de funcionar; evento `SESSION_REVOKED`. Repetir a chamada ⇒ 204 idempotente para sessão própria já revogada. Ref.: RF-12, RN-SES-03, RN-TOK-10
- **CA-FT06-04 — Revogar a atual.** Equivale a logout; a resposta indica `current=true` no evento; o cliente limpa o estado. Ref.: RN-SES-03
- **CA-FT06-05 — Efeito sobre access tokens.** Após revogação, a API do Identity recusa o access da sessão (`SESSION_REVOKED`); APIs consumidoras com introspecção recusam; sem introspecção, o access vale até `exp` (≤ 10 min). Ref.: RN-TOK-09, 11, 15
- **CA-FT06-06 — Encerrar todas.** Com reautenticação válida ⇒ 204; todas revogadas (`USER_ALL`), exceto a atual quando `keep_current=true`; TPL-11; evento `ALL_SESSIONS_REVOKED` com contagem. Ref.: RF-08, RN-SES-03
- **CA-FT06-07 — Reautenticação exigida.** Sem janela válida ⇒ `REAUTH_REQUIRED` e nada é revogado. Ref.: RN-PWD-08
- **CA-FT06-08 — Sessão expirada não é listada.** Sessões com `expires_at` vencido ou revogadas não aparecem. Ref.: RN-SES-02
- **CA-FT06-09 — Concorrência.** Revogações simultâneas da mesma sessão ⇒ idempotentes, um único evento; “encerrar todas” simultâneo a novo login não deixa sessão ativa anterior à operação. Ref.: RNF-20, DM-10
- **CA-FT06-10 — Autenticação.** Sem Bearer ⇒ 401; `sid` revogado ⇒ `SESSION_REVOKED`. Ref.: RN-TOK-15
- **CA-FT06-11 — Privacidade.** O IP exibido é aproximado (IPv4 /24, IPv6 /48) e o agente resumido (navegador/sistema). Ref.: RNF-15

## 5. Regras: RN-SES-01…04, RN-TOK-09/10/15, RN-PWD-08 · Perfil: usuário autenticado.
## 6. Dados exibidos: ver CA-FT06-01.
## 7. Rastreabilidade: UC-040 → CA-FT06-01, 02, 08, 10, 11 · UC-041 → CA-FT06-02…05, 09 · UC-042 → CA-FT06-06, 07, 09.
## 8. Pontos a validar / divergências / LEGACY: nenhum / nenhuma / `NOT_APPLICABLE`.
## 9. Resultado esperado: titular controla suas sessões; revogação imediata para o Identity e limitada por `exp` para APIs sem introspecção.
