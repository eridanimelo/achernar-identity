# Achernar Identity — Índice da documentação

Mapa de leitura do conjunto de discovery e especificações. Nenhum código de produto existe; tudo aqui é documentação.
Estado de retomada: `requisitos/state/current.yaml`. Revisão de completude: `requisitos/reviews/completeness-check.md`.

## 1. Ordem de leitura sugerida
1. `discovery/system.md` → visão, escopo, riscos, premissas (A-xx).
2. `specs/_shared/decisions.md` (D-01…D-51) e `parameters.md` (P-xx).
3. `specs/_shared/requirements.md` e `business-rules.md` (RF/RNF/RN).
4. Documentos técnicos transversais (tabela 3).
5. Specs por feature (tabela 2).
6. `specs/_shared/traceability.md` (matriz gerada) e `implementation-plan.md`.

## 2. Specs por feature (8 arquivos cada)
Arquivos: `business.md`, `api.md`, `database.md`, `frontend.md`, `security.md`, `tests.md`, `status.yaml`, `context-manifest.yaml`.

| Spec | Título | UCs | CA | CT |
|---|---|---|---|---|
| [FT-01](../FT-01-setup-primeiro-administrador/business.md) | FT-01 Setup do primeiro administrador | UC-001 | 12 | 15 |
| [FT-02](../FT-02-cadastro-e-verificacao-de-email/business.md) | FT-02 Cadastro e verificação de e-mail | UC-010, UC-011, UC-012 | 15 | 18 |
| [FT-03](../FT-03-autenticacao-por-senha-e-tokens/business.md) | FT-03 Autenticação por senha e tokens | UC-020, UC-021, UC-022 | 24 | 25 |
| [FT-04](../FT-04-recuperacao-e-gestao-de-senha/business.md) | FT-04 Recuperação e gestão de senha | UC-030, UC-031, UC-032, UC-033, UC-037, UC-055 | 19 | 20 |
| [FT-05](../FT-05-conta-e-alteracao-de-email/business.md) | FT-05 Conta e alteração de e-mail | UC-034, UC-035, UC-036 | 14 | 15 |
| [FT-06](../FT-06-sessoes-do-usuario/business.md) | FT-06 Sessões do usuário | UC-040, UC-041, UC-042 | 11 | 12 |
| [FT-07](../FT-07-login-social/business.md) | FT-07 Login social | UC-050, UC-051, UC-052 | 22 | 23 |
| [FT-08](../FT-08-vinculos-sociais/business.md) | FT-08 Vínculos sociais | UC-053, UC-054, UC-055 | 15 | 15 |
| [FT-09](../FT-09-registro-de-aplicacoes-e-apis/business.md) | FT-09 Registro de aplicações e APIs | UC-060, UC-061, UC-062, UC-063, UC-064, UC-067, UC-068 | 25 | 26 |
| [FT-10](../FT-10-chaves-jwks-e-introspeccao/business.md) | FT-10 Chaves, JWKS e introspecção | UC-070, UC-071, UC-072, UC-073 | 22 | 22 |
| [FT-11](../FT-11-administracao-de-contas/business.md) | FT-11 Administração de contas | UC-080, UC-081, UC-082, UC-083, UC-084 | 19 | 20 |
| [FT-12](../FT-12-eventos-de-seguranca-e-retencao/business.md) | FT-12 Eventos de segurança e retenção | UC-090, UC-091, UC-092 | 22 | 23 |
| [FT-13](../FT-13-emails-transacionais/business.md) | FT-13 E-mails transacionais | UC-095 | 16 | 15 |
| [FT-14](../FT-14-interface-base-tema-e-sessao/business.md) | FT-14 Interface base, tema e sessão no navegador | UC-100 | 20 | 20 |
| **Total** | | | **256** | **269** |

## 3. Documentos transversais (`specs/_shared/`)
| Documento | Conteúdo |
|---|---|
| `decisions.md` | decisões D-01…D-51 (decisão, justificativa, premissas, consequências) |
| `parameters.md` | parâmetros de política P-01…P-80 (valor padrão, faixa, variável) |
| `requirements.md` | RF-01…34 e RNF-01…20 |
| `business-rules.md` | regras RN-* por área |
| `error-catalog.md` · `event-catalog.md` | erros estáveis (RFC 9457) e eventos de segurança |
| `api-conventions.md` | convenções e índice dos 40 endpoints |
| `data-model.md` | modelo físico de dados (referência técnica) |
| `token-lifecycle.md` · `security-policy.md` · `social-login.md` · `emails.md` | especificações normativas transversais |
| `consumer-integration.md` | guia de integração de APIs/aplicações consumidoras (Spring Security 7) |
| `architecture.md` · `ui-map.md` · `visual-proposal.md` | arquitetura, mapa de telas e proposta visual |
| `deployment-operations.md` · `migrations-backup.md` · `config-changes.md` | implantação, operação, migrações/backup e mudanças futuras de configuração (não executadas) |
| `test-plan.md` · `implementation-plan.md` | estratégia de testes e plano de implementação E0–E9 |
| `traceability.md` | matriz UC→RF→CA→CT (gerada por `tools/verify_specs.py`) |

## 4. Convenção de IDs
RF/RNF · RN-<ÁREA>-nn · D-nn · P-nn · SEC-nn · DM-nn · UC-nnn · FT-nn · SCR-nn · TPL-nn · A-nn · DEV-nn · RISK-nn · CA-FTnn-nn (aceite) · CT-FTnn-nn (teste) · API-/DATA-/INT-/SEC-/RSK-FTnn-nn (locais da Spec) · CT-SEC/RNF/CON/PERF/A11Y/UI/INT/OPS/E2E (transversais).
