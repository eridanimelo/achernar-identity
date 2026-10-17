# Database Spec — FT-11 Administração de contas
`NEW` · `APPROVED` (documental). Usa estruturas de FT-02 (conta, credencial), FT-03 (sessão), FT-08 (vínculos), FT-12 (eventos).

### DATA-FT11-01 — Campos administrativos da conta
Perfil, estado, instante e motivo da desativação, instante da anonimização, versão (definidos em DATA-FT02-01); índices para filtros (prefixo do e-mail, estado, perfil, criação) e para localizar administradores ativos.
### DATA-FT11-02 — Valor de e-mail anonimizado
Valor derivado do identificador da conta em domínio reservado não entregável (`anon-<id>@anonymized.invalid`); mantém a unicidade e libera o endereço original.

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT11-01 | Contagem de administradores ativos e a mudança ocorrem sob trava consultiva global na mesma transação | DM-07 |
| INT-FT11-02 | Desativação: estado, revogação de sessões, invalidação de tokens, outbox e evento atômicos | RN-ADM-02 |
| INT-FT11-03 | Anonimização atômica; apenas `DISABLED`; irreversível | RN-ACC-13 |
| INT-FT11-04 | Verificação de perfil/estado feita no banco a cada requisição administrativa | RN-ADM-01 |
| INT-FT11-05 | Redação do e-mail em eventos antigos feita ao anonimizar (campo `details`/exibição) | RN-ACC-13 |

Eventos: `ACCOUNT_DISABLED/ENABLED`, `ADMIN_GRANTED/REVOKED`, `ACCOUNT_ANONYMIZED`. Rastreabilidade: DATA-FT11-01 → CA-FT11-01, 02, 07 · -02 → CA-FT11-11, 13.
