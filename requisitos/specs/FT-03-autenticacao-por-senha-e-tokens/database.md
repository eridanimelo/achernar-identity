# Database Spec — FT-03 Autenticação por senha e tokens

Modelo físico: `data-model.md §2.6, 2.7, 2.14`. `NEW` · `APPROVED` (documental).

### DATA-FT03-01 — Sessão (e audiências da sessão)
Sessão: id (`sid`), conta, aplicação, método (`pwd|google|facebook`), criada/`auth_time`, último uso, expira (criação+P-03), última reautenticação, revogada em/motivo (domínio fechado), IP (inet, truncável), agente (≤ 255). Audiências: conjunto de textos por sessão.
### DATA-FT03-02 — Refresh token
Hash SHA-256, sessão, criado, expira, rotacionado em, substituído por. Exatamente um corrente (não rotacionado) por sessão.
### DATA-FT03-03 — Contadores de limite e bloqueio de login
Contador por chave (regra+IP/e-mail-hash) e janela; bloqueio por hash do e-mail normalizado: falhas, janela, bloqueado até, nível, último bloqueio. Sem e-mail em claro.

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT03-01 | Um refresh corrente por sessão (índice único parcial) | DM-04 |
| INT-FT03-02 | Rotação: linha da sessão/refresh travada (`FOR UPDATE`) durante a troca | RN-TOK-04 |
| INT-FT03-03 | Limite de sessões serializado por conta (trava consultiva) | DM-10 |
| INT-FT03-04 | Contadores com incremento atômico | D-16 |
| INT-FT03-05 | Revogação por motivo em domínio fechado | RN-TOK-10 |
| INT-FT03-06 | Login: sessão, refresh e evento na mesma transação; falhas em transação independente | RN-AUD-01 |

Retenção: sessões/refresh encerrados purgados após P-52; contadores após P-56. Histórico: `LOGIN_*`, `TOKEN_ISSUED`, `REFRESH_*`, `LOGOUT`, `SESSION_REVOKED`, `ACCOUNT_LOCKED_THROTTLE`.
Rastreabilidade: DATA-FT03-01 → CA-FT03-01, 09, 18, 21 · -02 → CA-FT03-13…16, 20 · -03 → CA-FT03-04, 11. LEGACY/Pontos: nenhum.
