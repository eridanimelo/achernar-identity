# Security Spec — FT-02 Cadastro e verificação de e-mail

## 0. Identificação
FT-02 · `NEW` · `APPROVED` (documental).

## 1. Aplicabilidade
Rotas públicas que criam dados e disparam e-mails: riscos de enumeração, abuso de envio, força bruta de tokens, pré-sequestro de conta.

## 2. Controles
| ID | Controle | Ref. |
|---|---|---|
| SEC-FT02-01 | Resposta, status e tempo equivalentes para e-mail novo e existente | SEC-07, SEC-04 |
| SEC-FT02-02 | Limites por IP (P-32) e por e-mail (silencioso) e no reenvio (P-34) | SEC-05 |
| SEC-FT02-03 | Token de verificação 256 bits, apenas hash no banco, uso único atômico, 24 h, no fragmento da URL | SEC-09 |
| SEC-FT02-04 | Origem validada (CORS dinâmico) e `client_id` ativo; links só de configuração, nunca de `Host` | SEC-26, SEC-22 |
| SEC-FT02-05 | Hash de senha com semáforo de concorrência; senha nunca registrada | SEC-02, SEC-06, SEC-31 |
| SEC-FT02-06 | Conta pendente não gera tokens nem sessão | SEC-08 |

## 3. Dados sensíveis: senha (hash), token (hash), e-mail (minimização: não em contadores — hash do e-mail normalizado nas chaves de limite).
## 4. Riscos
| ID | Risco | Controle |
|---|---|---|
| RSK-FT02-01 | Enumeração de e-mails | SEC-FT02-01 |
| RSK-FT02-02 | E-mail bombing a terceiros | SEC-FT02-02; TPL-05 limitado por e-mail |
| RSK-FT02-03 | Pré-sequestro (atacante cadastra e-mail da vítima antes) | conta pendente não loga; login social verificado substitui a pendente (SEC-20) |
| RSK-FT02-04 | DoS via hash de senha | SEC-06 |
| RSK-FT02-05 | Vazamento do token em Referer/log | fragmento de URL, `Referrer-Policy: no-referrer`, redação de logs |
## 5. Auditoria: eventos de FT-02 em `event-catalog.md`.
## 6. Pontos a validar: nenhum.
## 7. Rastreabilidade
SEC-FT02-01 → CA-FT02-02, 03 · -02 → CA-FT02-07, 11 · -03 → CA-FT02-08…10 · -04 → CA-FT02-06, 14 · -05 → CA-FT02-05 · -06 → CA-FT02-12.
