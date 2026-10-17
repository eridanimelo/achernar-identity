# Database Spec — FT-09 Registro de aplicações e APIs
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.4, 2.5`.

### DATA-FT09-01 — Aplicação consumidora
Identificador, `client_id` (único, imutável), nome, descrição, tipo, estado, cadastro aberto, acesso à API do Identity, provedores habilitados, TTL do access (opcional, 300–900), URLs de ação, marcação “do sistema”, criação/atualização, versão.
### DATA-FT09-02 — Origens, URLs de retorno e audiências da aplicação
Conjuntos filhos (origem; URL de retorno; API permitida com marca de padrão); chaves compostas, remoção em cascata.
### DATA-FT09-03 — Segredos da aplicação
Id, hash do segredo (único), criação, revogação, último uso. Máximo de 2 ativos.
### DATA-FT09-04 — API consumidora
Id, `audience` (única, imutável), nome, descrição, estado, versão.
### DATA-FT09-05 — Credenciais da API
Id, hash (único), criação, revogação, último uso. Máximo de 2 ativos.

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT09-01 | `client_id` e `audience` únicos; formatos por CHECK | RN-APP-01, 14 |
| INT-FT09-02 | ≤ 2 segredos ativos, serializado pela trava do registro pai | DM-09 |
| INT-FT09-03 | Aplicações e APIs não são excluídas | RN-APP-13 |
| INT-FT09-04 | Desativar aplicação e revogar suas sessões na mesma transação | RN-APP-07 |
| INT-FT09-05 | Controle otimista por versão | RN-APP-13 |
| INT-FT09-06 | Segredos só como hash; nunca recuperáveis | SEC-29 |
| INT-FT09-07 | Limites de quantidade (P-74) validados na criação | P-74 |

Eventos: `APPLICATION_*`, `API_*`, `*_ROTATED`. Rastreabilidade: DATA-FT09-01 → CA-FT09-01, 02, 06, 09 · -02 → CA-FT09-03…05 · -03/-05 → CA-FT09-12, 13, 16 · -04 → CA-FT09-14, 15.
