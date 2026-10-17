# Security Spec — FT-14 Interface base, tema e sessão no navegador
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT14-01 | Access token somente em memória; sem armazenamento persistente | SEC-12 |
| SEC-FT14-02 | Refresh em cookie HttpOnly gerido pelo servidor; cliente nunca o lê | SEC-13 |
| SEC-FT14-03 | CSP restritiva, sem scripts inline/terceiros, `frame-ancestors 'none'`, `nosniff`, `Referrer-Policy: no-referrer` | SEC-23, SEC-35 |
| SEC-FT14-04 | Fragmentos com tokens removidos da barra imediatamente; sem token em logs de cliente | SEC-09 |
| SEC-FT14-05 | Guardas de rota são conveniência; a autorização real é do servidor | SEC-27 |
| SEC-FT14-06 | Tratamento seguro de erros: mensagens por `code`, sem detalhes técnicos | SEC-37 |
| SEC-FT14-07 | Saída escapada (sem `innerHTML` com dados externos); dependências com varredura | SEC-36 |
| SEC-FT14-08 | Cabeçalho `X-Achernar-Client` e `Origin` nas rotas com cookie | SEC-14 |

Riscos: RSK-FT14-01 XSS roubando o access token (CSP, escape, vida curta); RSK-FT14-02 falso positivo de reuso por corrida entre abas (Web Locks + tolerância); RSK-FT14-03 clickjacking (`frame-ancestors`); RSK-FT14-04 vazamento por Referer (política de referrer).
Rastreabilidade: SEC-FT14-01 → CA-FT14-04 · -02 → CA-FT14-05 · -03 → CA-FT14-16 · -04 → CA-FT14-17 · -05 → CA-FT14-02 · -06 → CA-FT14-15 · -07 → CA-FT14-16 · -08 → CA-FT14-06, 07.
