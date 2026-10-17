# Database Spec — FT-05 Conta e alteração de e-mail
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.1, 2.8`.

### DATA-FT05-01 — Token de troca de e-mail
Finalidade `EMAIL_CHANGE`; hash, conta, aplicação de origem, novo e-mail (exibido e normalizado — obrigatórios nesta finalidade), criação, expiração (1 h), consumo, invalidação.
Estruturas reutilizadas: Conta (DATA-FT02-01), Credencial (DATA-FT02-02), Vínculos sociais (DATA-FT08-01), Sessão (DATA-FT03-01).

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT05-01 | Confirmação: consumo do token, atualização de e-mail, revogação de sessões, invalidação de tokens e evento na mesma transação | RN-ACC-15 |
| INT-FT05-02 | Unicidade do novo e-mail revalidada na confirmação (violação ⇒ falha sem efeito) | DM-01 |
| INT-FT05-03 | Um pedido pendente por conta | DM-05 |
| INT-FT05-04 | Pedido não reserva o endereço | RN-ACC-02 |
| INT-FT05-05 | Novo e-mail guardado apenas enquanto o token existe (purga P-51) | RNF-15 |

Eventos: `EMAIL_CHANGE_REQUESTED`, `EMAIL_CHANGED`. Rastreabilidade: DATA-FT05-01 → CA-FT05-04…06, 08…11, 14.
