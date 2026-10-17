# Security Spec — FT-09 Registro de aplicações e APIs
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT09-01 | Rotas administrativas: perfil/estado/sessão verificados no banco | SEC-27 |
| SEC-FT09-02 | Segredos 256 bits, exibidos uma vez, SHA-256, comparação em tempo constante, ≤ 2 ativos | SEC-29, SEC-33 |
| SEC-FT09-03 | Origens/URLs validadas e comparadas exatamente; sem curingas | SEC-16, SEC-26 |
| SEC-FT09-04 | Configuração pública mínima | RN-APP-12 |
| SEC-FT09-05 | `client_id`/CORS não autenticam; `BROWSER` sem segredo/introspecção | RN-APP-05 |
| SEC-FT09-06 | Todas as mudanças auditadas (sem valores sensíveis) | RN-AUD-04 |
| SEC-FT09-07 | Aplicação do sistema protegida contra desativação | D-46 |
| SEC-FT09-08 | Restrição de rede recomendada para `/api/v1/admin/**` | SEC-28 |

Riscos: RSK-FT09-01 origem/retorno mal configurados abrem redirecionamento ou CORS indevido (validação estrita); RSK-FT09-02 vazamento de segredo (exibição única, rotação, auditoria); RSK-FT09-03 administrador desativa a UI e perde o acesso (SEC-FT09-07); RSK-FT09-04 enumeração de aplicações via config pública (404 genérico, só dados mínimos).
Rastreabilidade: SEC-FT09-01 → CA-FT09-21 · -02 → CA-FT09-12, 13 · -03 → CA-FT09-03, 04, 19 · -04 → CA-FT09-18 · -05 → CA-FT09-12 · -06 → CA-FT09-25 · -07 → CA-FT09-11.
