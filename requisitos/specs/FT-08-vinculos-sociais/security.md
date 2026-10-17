# Security Spec — FT-08 Vínculos sociais
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT08-01 | Vincular exige sessão, reautenticação ≤ 5 min e prova no provedor pelo mesmo navegador | SEC-19 |
| SEC-FT08-02 | Transação LINK/REAUTH atada à conta e sessão; revalidada no retorno | RN-SOC-13 |
| SEC-FT08-03 | `return_url` apenas de `redirect_uris` registradas | SEC-16 |
| SEC-FT08-04 | Reautenticação social só vale para vínculo da própria conta | RN-SOC-12 |
| SEC-FT08-05 | Proteção do último método de acesso | RN-ACC-05 |
| SEC-FT08-06 | Notificação ao titular em vínculo/desvínculo | SEC-21 |

Riscos: RSK-FT08-01 atacante com sessão roubada vincula próprio provedor (reautenticação + aviso por e-mail); RSK-FT08-02 vincular identidade já usada (conflito); RSK-FT08-03 perda de acesso (último método).
Rastreabilidade: SEC-FT08-01 → CA-FT08-01 · -02 → CA-FT08-04 · -03 → CA-FT08-01 · -04 → CA-FT08-12 · -05 → CA-FT08-07, 09 · -06 → CA-FT08-02, 06.
