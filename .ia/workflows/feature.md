---
name: workflow-feature
type: workflow
description: "Pipeline de Feature"
---

# Workflow — Pipeline de Feature

Orquestração do fluxo. Este arquivo diz **em que ordem**, **com quem** e **com
qual Gate** cada etapa acontece.

- critérios de Gate: `gates/`
- procedimento de cada etapa: `skills/`
- contrato de contexto de cada `agents/<intenção>/AGENT.md`
- regras estáveis: `rules/`

MUST NOT repetir aqui o conteúdo dessas camadas.

---

## Visão geral

```text
SOLICITAÇÃO → INTENÇÃO → ESCOPO → ESTADO → ORIGEM → CONTEXTO → ETAPA SOLICITADA → VALIDAÇÃO → ESTADO → STOP
                                             │
                                      NEW ────┴──── LEGACY
                                       │              │
                              requisito/necessidade   evidência do sistema anterior
                                       └──────┬───────┘
                                              ↓
                                    REQUIREMENTS / DISCOVERY
                                              ↓
                                        SPECIFICATION
                                              ↓
                                        IMPLEMENTATION
                                              ↓
                                     VALIDATION / REVIEW
```

---

## Etapas

| # | Etapa | Intenção | Agent | Procedimento | Gate de saída | Saída |
|---|---|---|---|---|---|---|
| 0 | Contexto | — | Agent da intenção | `rules/context/*` | `GATE-CTX` | escopo + contexto delimitado |
| 1 | Requisitos | `REQUIREMENTS` | `agents/requirements` | `skills/specification` (modo `REQUIREMENTS`) | gate de `corporate/requirements` | `business.md` |
| 2 | Discovery | `DISCOVERY` | `agents/discovery` | `skills/discovery` | `GATE-PLN` | `discovery/*.md` |
| 3 | Especificação | `SPECIFICATION` | `agents/specification` | `skills/specification` | `GATE-AUT` | artefatos da Spec |
| 4 | Implementação | `BACKEND` / `FRONTEND` / `DATABASE` / `MIGRATION` | Agents correspondentes | Skills/corporate correspondentes; Backend/Frontend delegam Reuse Scan aos especialistas `agents/reuse/*` | `GATE-ANL` (pré) + `GATE-QLT` (pós) | código/artefato |
| 5 | Validação | `TESTING` / `SECURITY` | `agents/testing` / `agents/security` | corporate correspondente | `GATE-QLT` | evidência de execução |
| 6 | Revisão | `REVIEW` | `agents/review` | `skills/review` | `GATE-CLS` | findings em `reviews/` |
| 7 | Finalização | — | orquestrador | — | `GATE-CLS` | estado atualizado |

`ARCHITECTURE` e `JIRA` são acionados conforme o escopo, sem etapa própria.

---

## Origem da feature

A origem é resolvida por feature/UC e não por projeto:

- `NEW`: requisito/necessidade nova, mesmo dentro de um sistema já existente;
- `LEGACY`: comportamento anterior é fonte necessária para descoberta, preservação ou migração.

Os dois modos convergem em Requirements/Discovery e seguem o mesmo pipeline após a Spec.
Somente `LEGACY` exige leitura de evidências/código anterior quando aplicável.

---

## Entrada

Uma feature pode ser iniciada:

- pela Spec;
- por issue Jira vinculada;
- pelo roadmap/priorização do Discovery;
- por solicitação humana explícita.

---

## Regra de parada

Ao concluir uma etapa:

- MUST apresentar o resultado da etapa;
- MUST registrar o resultado do Gate aplicável;
- MUST registrar pendências;
- MUST NOT iniciar a etapa seguinte sem intenção explícita.

Conclusão de Spec, Discovery ou Review **não** autoriza a próxima etapa.

---

## Paralelismo

Backend e frontend MAY executar em paralelo somente quando explicitamente
solicitados e quando `GATE-AUT` permitir.

---

## Isolamento de contexto

- descobrir o pacote corporativo apenas para a intenção atual;
- MUST NOT carregar todos os pacotes corporativos por precaução;
- mudança de fase é mudança de contexto: aplicar `rules/context/session-lifecycle.md`.
