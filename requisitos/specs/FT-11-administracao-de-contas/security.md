# Security Spec — FT-11 Administração de contas
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT11-01 | Perfil, estado e sessão do administrador verificados no banco a cada requisição | SEC-27 |
| SEC-FT11-02 | Último administrador protegido (trava consultiva + contagem) | SEC-28, DM-07 |
| SEC-FT11-03 | Administrador não se auto-desativa; sem personificação nem acesso a segredos | RN-ADM-04, 08 |
| SEC-FT11-04 | Reautenticação para ações destrutivas/privilegiadas | RN-ADM-09 |
| SEC-FT11-05 | Auditoria completa com ator, alvo, resultado | RN-AUD-01 |
| SEC-FT11-06 | Anonimização com confirmação explícita e irreversível | RN-ADM-06 |
| SEC-FT11-07 | Respostas administrativas sem hashes/segredos; filtro por lista permitida | SEC-32 |
| SEC-FT11-08 | Recomendação de restringir rede `/api/v1/admin/**` | SEC-28 |

Riscos: RSK-FT11-01 administrador comprometido (reautenticação, auditoria, alerta `ALERT`); RSK-FT11-02 corrida entre administradores (trava); RSK-FT11-03 anonimização indevida (confirmação + irreversível documentado); RSK-FT11-04 vazamento de dados pessoais em listagens (campos mínimos, paginação).
Rastreabilidade: SEC-FT11-01 → CA-FT11-15 · -02 → CA-FT11-07 · -03 → CA-FT11-06, 17 · -04 → CA-FT11-18 · -05 → CA-FT11-16 · -06 → CA-FT11-12 · -07 → CA-FT11-01, 02.
