# Security Spec — FT-03 Autenticação por senha e tokens

`NEW` · `APPROVED` (documental).

## Controles
| ID | Controle | Ref. |
|---|---|---|
| SEC-FT03-01 | Erros e tempos equivalentes (hash fictício para conta inexistente) | SEC-04, D-48 |
| SEC-FT03-02 | Bloqueio por e-mail + limites por IP (P-30, P-31) em PostgreSQL | SEC-05 |
| SEC-FT03-03 | Argon2id, rehash, semáforo de hash | SEC-02, SEC-06 |
| SEC-FT03-04 | Refresh opaco 256 bits só como hash; rotação; reuso revoga sessão; tolerância só ao antecessor | SEC-10, SEC-11 |
| SEC-FT03-05 | Cookie `__Secure-achrt_*` HttpOnly/Secure/SameSite=Strict/Path=/auth | SEC-13 |
| SEC-FT03-06 | CSRF: Origin, `X-Achernar-Client`, JSON | SEC-14 |
| SEC-FT03-07 | Access token em memória; RS256; `typ`, `kid`, `aud` | SEC-12, SEC-25 |
| SEC-FT03-08 | Estados específicos só após provar a senha | SEC-08 |
| SEC-FT03-09 | Logs redigidos | SEC-31 |

## Riscos
| ID | Risco | Controle |
|---|---|---|
| RSK-FT03-01 | Força bruta/credential stuffing | SEC-FT03-02 (nota: chave por e-mail pode bloquear a vítima — risco aceito, mitigado por escalonamento e TPL-08) |
| RSK-FT03-02 | Roubo de refresh | rotação + detecção de reuso + cookie HttpOnly |
| RSK-FT03-03 | CSRF no refresh/logout | SEC-FT03-05/06 |
| RSK-FT03-04 | Enumeração por tempo | SEC-FT03-01 |
| RSK-FT03-05 | Corrida entre abas gera falso positivo de reuso | tolerância P-04 + Web Locks |
Pontos a validar: nenhum.
Rastreabilidade: SEC-FT03-01 → CA-FT03-03 · -02 → CA-FT03-04, 11 · -03 → CA-FT03-10 · -04 → CA-FT03-13…16 · -05/06 → CA-FT03-19 · -07 → CA-FT03-08 · -08 → CA-FT03-05 · -09 → CA-FT03-23.
