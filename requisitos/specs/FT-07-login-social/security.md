# Security Spec — FT-07 Login social
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT07-01 | `state` 256 bits, uso único, 10 min, amarrado ao navegador (cookie Lax) | SEC-17 |
| SEC-FT07-02 | PKCE S256 ao provedor, `nonce` no Google, validação completa do ID token | SEC-17 |
| SEC-FT07-03 | Código próprio 256 bits, 60 s, uso único, atado a `client_id`/`redirect_uri`/PKCE do cliente; reuso revoga a sessão | SEC-18 |
| SEC-FT07-04 | `redirect_uri` exata; erros anteriores à validação não redirecionam | SEC-16 |
| SEC-FT07-05 | Sem vinculação por e-mail; substituição de conta pendente | SEC-19, SEC-20 |
| SEC-FT07-06 | Provedor só em hosts fixos, timeouts 5 s | SEC-40 |
| SEC-FT07-07 | Segredos do provedor só no backend; tokens do provedor descartados | RN-SOC-14 |
| SEC-FT07-08 | Limites de início e troca | SEC-05 |

Riscos: RSK-FT07-01 login CSRF; RSK-FT07-02 interceptação do código na URL; RSK-FT07-03 open redirect; RSK-FT07-04 pré-sequestro por e-mail; RSK-FT07-05 provedor devolve e-mail não confiável (Facebook tratado como não verificado); RSK-FT07-06 SSRF (hosts fixos).
Rastreabilidade: SEC-FT07-01 → CA-FT07-11 · -02 → CA-FT07-01, 12 · -03 → CA-FT07-15, 16 · -04 → CA-FT07-02 · -05 → CA-FT07-08…10 · -06 → CA-FT07-12 · -07 → CA-FT07-13 · -08 → CA-FT07-03, 19.
