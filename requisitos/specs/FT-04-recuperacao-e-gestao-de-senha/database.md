# Database Spec — FT-04 Recuperação e gestão de senha
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.2, 2.8, 2.6 (last_reauth_at)`.

### DATA-FT04-01 — Token de recuperação
Finalidade `PASSWORD_RESET`; hash, conta, aplicação de origem, criação, expiração (30 min), consumo, invalidação (estrutura única `one_time_token`, ver DATA-FT02-03).
### DATA-FT04-02 — Reautenticação
Instante da última reautenticação por sessão (`last_reauth_at`; definido no login e na reautenticação).
Estruturas reutilizadas: Credencial de senha (DATA-FT02-02), Sessão (DATA-FT03-01), Contadores/bloqueio (DATA-FT03-03).

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT04-01 | Redefinição: troca de hash, revogação de sessões, invalidação de tokens, zeragem do bloqueio e evento em uma transação | RN-PWD-05 |
| INT-FT04-02 | Consumo do token atômico; falha de política não consome | DM-11 |
| INT-FT04-03 | Um token de recuperação ativo por conta | DM-05 |
| INT-FT04-04 | Troca de senha e criação da primeira senha serializadas pela trava da conta (sem duas credenciais) | DM-08 |

Retenção: tokens P-51. Eventos: `PASSWORD_RESET_*`, `PASSWORD_CHANGED`, `PASSWORD_SET`, `REAUTH_*`.
Rastreabilidade: DATA-FT04-01 → CA-FT04-03, 05, 06, 08 · DATA-FT04-02 → CA-FT04-12, 14.
