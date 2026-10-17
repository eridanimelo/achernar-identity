# Achernar Identity — Verificação de completude (discovery e especificações)

Data: 2026-10-17 · Escopo: discovery + especificações completas (sem implementação) · Executor: agente de especificação (autoverificação automatizada + revisão)

## 1. Resultado resumido
| Verificação | Resultado |
|---|---|
| Cobertura das 18 áreas pedidas | **Completa** (tabela 2) |
| Specs por feature: 14 × 8 arquivos (112 arquivos) | **Completo** |
| IDs referenciados e não definidos | **0** (script `requisitos/tools/verify_specs.py`) |
| RF (34) sem critério de aceite (CA) | **0** |
| RNF (20) sem verificação | **0** (`test-plan.md §9` + CA/CT) |
| UC (47) sem CA | **0** |
| CA (256) sem cenário de teste (CT) | **0** |
| CT (269) sem CA | **0** |
| Critérios de aceite verificáveis (dado/quando/então ou condição mensurável) | **Sim** (revisão amostral + todas as Specs seguem o formato) |
| Valores de política concretos ou configuráveis | **Sim** (`parameters.md` P-01…P-80) |
| Decisões pendentes que a IA poderia resolver | **0** |
| Código de produto, Docker, dependências, migrações, deploy alterados | **Nenhum** |
| Credenciais reais no repositório | **Nenhuma** (varredura textual; placeholders apenas) |

Limite da verificação: o script comprova integridade de IDs e cobertura, **não** a correção semântica. Revisão humana (GATE-AUT) continua necessária e não foi simulada. Não foi acionado revisor independente (subagente) nesta execução.

## 2. Cobertura das 18 áreas do pedido
| # | Área | Onde |
|---|---|---|
| 1 | Visão e escopo | `discovery/system.md`, `decisions.md` D-01, D-50 |
| 2 | RF/RNF | `requirements.md` |
| 3 | Regras de negócio | `business-rules.md` (RN-*) |
| 4 | Fluxos | `business.md` (§3) de cada FT; `token-lifecycle.md`, `social-login.md` |
| 5 | Arquitetura | `architecture.md` |
| 6 | Modelo de dados | `data-model.md` + `database.md` por FT |
| 7 | Contratos de API | `api-conventions.md` + `api.md` por FT (40 operações) |
| 8 | Guia de integração | `consumer-integration.md` |
| 9 | Política de segurança | `security-policy.md` (SEC-01…40) + `security.md` por FT |
| 10 | Ciclo de vida dos tokens | `token-lifecycle.md` |
| 11 | Login social e e-mails | `social-login.md`, `emails.md` |
| 12 | Mapa de telas | `ui-map.md` + `frontend.md` por FT |
| 13 | Proposta visual | `visual-proposal.md` |
| 14 | Critérios de aceite verificáveis | CA-FTnn-nn (256) |
| 15 | Cenários de teste (falhas e concorrência) | CT-FTnn-nn (269), `test-plan.md` (CT-CON, CT-PERF, CT-INT, CT-OPS, CT-E2E) |
| 16 | Configuração, implantação, operação | `deployment-operations.md`, `config-changes.md`, `parameters.md` |
| 17 | Migrações, backup, restauração | `migrations-backup.md` |
| 18 | Plano de implementação | `implementation-plan.md` (E0–E9) |

## 3. Gates do framework
| Gate | Situação | Evidência |
|---|---|---|
| GATE-CTX (contexto) | PASS | `context-manifest.yaml` por Spec |
| GATE-PLN (planejamento) | PASS | `discovery/*` (47 UCs, dependências, riscos, Waves) |
| Aprovação documental das Specs | `approved` sob a delegação do prompt de 2026-10-16 | `status.yaml` de cada FT |
| GATE-AUT (autorização de implementação) | **NÃO concedido** — exige intenção explícita do humano | o prompt proíbe implementar nesta execução |
| GATE-QLT / GATE-CLS | não aplicáveis (sem implementação) | — |

## 4. Inconsistências encontradas e corrigidas durante a execução
1. Estados da conta: `RN-ACC-02` (pedido de troca não reserva e-mail) alinhado a `DM-01`.
2. `session_id` em `social_exchange_code` para revogar a sessão no reuso do código (RFC 6749 §4.1.2).
3. Códigos de erro de redirecionamento do login social unificados em `error-catalog.md`.
4. Árvore de decisão de conta social alinhada às regras RN-SOC-04…06 (sem vinculação por e-mail).
5. Corpo de erro padronizado em `snake_case` (`trace_id`, `retry_after_seconds`), coerente com D-51; `traceId` permanece apenas como campo de log.
6. Esclarecimentos registrados como regras (rastreáveis): **RN-SET-06/07** (setup: conflito de e-mail, sem sessão), **RN-ACC-15** (troca de e-mail revoga todas as sessões, pois a confirmação é por link sem sessão — esclarece D-47), **RN-ADM-09** (ações administrativas destrutivas exigem reautenticação recente — extensão de D-22).
7. `SAME_AS_CURRENT` incluído como código de campo em `VALIDATION_ERROR`.
8. Divergências com `README`/`technology.yaml`/`PROJECT.yaml` (Java 27, Spring Authorization Server, SSO, Tailwind, "30 Specs"): DEV-01…07 em `decisions.md` D-40 e `config-changes.md`; arquivos de `.ia/config` **não** foram alterados; o README foi atualizado.

## 5. Itens que dependem do operador (não resolvíveis pela IA; nenhum bloqueia a especificação)
| ID | Item | Onde tratado |
|---|---|---|
| A-01 | Aplicações `BROWSER` e Identity no mesmo site (senão usar BFF `CONFIDENTIAL`) | `consumer-integration.md` |
| A-02 | Provedor SMTP de produção | P-42/P-43; qualquer SMTP |
| A-03 | Domínios e certificados | P-60…P-62; proxy com TLS |
| A-04 | Apps OAuth Google/Facebook (credenciais) | P-70…P-72; checklist em `social-login.md` |
| A-05 | Logotipo oficial | proposta visual provisória (D-38) |
| A-06 | Chave-mestra e segredo de bootstrap gerados no deploy | P-65/P-66 |
| A-07 | Janela de manutenção para migrações em produção | `migrations-backup.md` |

## 6. Riscos aceitos e pontos de atenção para a implementação
- Bloqueio de login por e-mail permite negar acesso à vítima por tentativas deliberadas (RSK-FT03-01): mitigado por escalonamento, aviso (TPL-08) e recuperação de senha; aceito.
- Access token revogado vale até `exp` (≤ 10 min) para APIs sem introspecção (RN-TOK-09): decisão consciente, documentada para as equipes consumidoras.
- Facebook: e-mail tratado como não verificado e a Graph API muda de versão; versão é parâmetro (P-72).
- Metas de desempenho (RNF-02) são hipóteses de dimensionamento; confirmar em teste de carga na E9.
- Versões de tecnologia (Java 25, Spring Boot 4.1.x, Angular 22, PostgreSQL 18) devem ser revalidadas na E0 contra as versões estáveis do dia da implementação.
- `requisitos/tools/verify_specs.py` é ferramenta de documentação (não é código de produto); pode ser descartada sem impacto.

## 7. Conclusão
Discovery e especificações estão **completos e consistentes** para o escopo pedido. Nada foi implementado. O próximo passo é revisão humana e, se aprovada, intenção explícita de implementar (GATE-AUT) começando pela etapa E0 de `implementation-plan.md`.
