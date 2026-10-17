# Security Spec — FT-12 Eventos de segurança e retenção
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT12-01 | Eventos imutáveis para a aplicação; privilégios mínimos no banco | SEC-30, RN-AUD-02 |
| SEC-FT12-02 | Lista permitida de `details`; nenhum segredo | SEC-31, RN-AUD-04 |
| SEC-FT12-03 | Consulta somente ADMIN verificado no banco; filtros por lista permitida; cursor opaco | SEC-27, SEC-32 |
| SEC-FT12-04 | Minimização: IP/agente anulados em 90 dias; e-mail redigido após anonimização | RNF-15, D-27 |
| SEC-FT12-05 | Alertas gerando log e métrica | RN-AUD-06 |
| SEC-FT12-06 | Gravação independente para eventos de falha (não se perdem com rollback) | RN-AUD-01 |
| SEC-FT12-07 | Retenção idempotente e segura sob concorrência | RNF-14 |

Riscos: RSK-FT12-01 injeção de dados em `details` (whitelist e tipos); RSK-FT12-02 crescimento ilimitado (retenção, limite de amostragem); RSK-FT12-03 adulteração (privilégios); RSK-FT12-04 vazamento de dados pessoais na consulta (só ADMIN, minimização).
Rastreabilidade: SEC-FT12-01 → CA-FT12-04 · -02 → CA-FT12-03 · -03 → CA-FT12-08, 10 · -04 → CA-FT12-12, 11 · -05 → CA-FT12-06 · -06 → CA-FT12-02 · -07 → CA-FT12-18.
