# Database Spec — FT-10 Chaves, JWKS e introspecção
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.11`.

### DATA-FT10-01 — Chave de assinatura
Id, `kid` (único; thumbprint), algoritmo (RS256), chave pública (JWK), chave privada **cifrada** (apagada quando `REMOVED`), estado (`PUBLISHED|ACTIVE|RETIRED|REMOVED`), instantes de criação/publicação/ativação/aposentadoria/remoção, motivo da remoção.
Introspecção usa leitura de Sessão (DATA-FT03-01), Conta (DATA-FT02-01), Aplicação (DATA-FT09-01) e Credencial da API (DATA-FT09-05); não cria dados.

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT10-01 | No máximo uma chave `ACTIVE` (índice único parcial) | DM-06 |
| INT-FT10-02 | Promoção/rotação em transação com trava; operação idempotente por instância | RNF-14 |
| INT-FT10-03 | Chave privada cifrada com a chave-mestra; nunca em claro, log ou backup em claro | SEC-24 |
| INT-FT10-04 | Chave `REMOVED` perde o material privado; linha purgada após P-59 | RN-RET-01 |
| INT-FT10-05 | Transições de estado só nos sentidos permitidos | RN-TOK-12 |

Eventos: `SIGNING_KEY_ROTATED`, `SIGNING_KEY_REMOVED`, `INTROSPECTION_AUTH_FAILED`. Rastreabilidade: DATA-FT10-01 → CA-FT10-04…06, 09, 12, 13.
