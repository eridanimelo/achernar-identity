# Database Spec — FT-02 Cadastro e verificação de e-mail

## 0. Identificação
FT-02 · `NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.1, 2.2, 2.8`.

## 1. Estruturas lógicas

### DATA-FT02-01 — Conta
| Dado | Significado | Obrig. | Restrição | Ref. |
|---|---|---|---|---|
| identificador | `sub` estável, opaco | sim | único, imutável | RN-ACC-03 |
| e-mail exibido / normalizado | endereço e forma canônica | sim | normalizado único | RN-ACC-02 |
| estado | `PENDING_VERIFICATION`, `ACTIVE`, `DISABLED` | sim | domínio fechado; `ACTIVE` exige e-mail verificado | RN-ACC-01 |
| perfil | `USER`/`ADMIN` | sim | padrão `USER` | RN-ADM-01 |
| e-mail verificado em | instante da verificação | condicional | obrigatório se `ACTIVE` | RN-ACC-10 |
| idioma | `pt-BR`/`en` | sim | padrão P-64 | RN-ACC-08 |
| desativação (em, motivo), anonimização (em) | ciclo de vida | não | ver FT-11 | RN-ACC-12/13 |
| criada/atualizada, versão | auditoria/otimista | sim | — | — |

### DATA-FT02-02 — Credencial de senha
Conta (1) → credencial (0..1): resumo seguro da senha (Argon2id), instante da última alteração, criação. Sem senha em claro (RNF-01).

### DATA-FT02-03 — Token de verificação (uso único)
Finalidade `EMAIL_VERIFICATION`; hash do token (SHA-256), conta, aplicação de origem, criação, expiração (24 h), consumo, invalidação. Estrutura compartilhada com recuperação/troca de e-mail (DATA-FT04-01, DATA-FT05-01) — única tabela `one_time_token`.

## 2. Integridade
| ID | Regra | Ref. |
|---|---|---|
| INT-FT02-01 | E-mail normalizado único; violação concorrente tratada como “duplicado” | DM-01, DM-12 |
| INT-FT02-02 | Conta `ACTIVE` sempre com e-mail verificado | DM-02 |
| INT-FT02-03 | No máximo um token ativo por (conta, finalidade); novo invalida o anterior | DM-05 |
| INT-FT02-04 | Consumo do token é atômico (condição de não consumido e não expirado) | DM-11 |
| INT-FT02-05 | Cadastro grava conta, credencial, token, outbox e evento na mesma transação | RN-AUD-01 |

## 3. Retenção: contas pendentes purgadas após P-50 (cascata); tokens após P-51 (RN-RET-04/05).
## 4. Histórico: eventos `ACCOUNT_REGISTERED`, `REGISTRATION_DUPLICATE_ATTEMPT`, `EMAIL_VERIFIED`, `EMAIL_VERIFICATION_RESENT`.
## 5. LEGACY: `NOT_APPLICABLE`. Pontos a validar: nenhum.
## 6. Rastreabilidade
| Estrutura | Critérios |
|---|---|
| DATA-FT02-01 | CA-FT02-01, 04, 08, 13 |
| DATA-FT02-02 | CA-FT02-01, 05 |
| DATA-FT02-03 | CA-FT02-08…11 |
