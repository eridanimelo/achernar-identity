---
name: discovery
type: skill
description: >-
  Descobrir e planejar o produto antes de criar Specs: inventário de módulos,
  casos de uso, mapa de dependências, priorização e Waves. Usar em solicitações
  de discovery, levantamento de escopo, inventário funcional, backlog inicial,
  análise de dependências ou planejamento de Waves, para features de origem NEW ou LEGACY.
---

# Skill: Discovery

## Quando usar

Quando for necessário compreender o produto e produzir o planejamento inicial
antes das Specs. Pode conter features `NEW` e `LEGACY` no mesmo projeto.

## Quando não usar

Para criar Spec, implementar ou revisar feature já especificada.

## Entradas

MUST consultar:

- `config/PROJECT.yaml`;
- `config/paths.yaml` quando necessário;
- requisitos/documentos/contexto fornecido.

Para `LEGACY`, consultar somente os caminhos `legacy.*` necessários.

## Origem da feature e fonte de requisitos

1. identificar a origem por feature/UC, não pelo projeto;
2. `NEW` quando não houver implementação anterior como fonte obrigatória;
3. `LEGACY` quando código, telas, banco ou sistema anterior forem fonte para descobrir/preservar comportamento;
4. um mesmo projeto MAY conter features `NEW` e `LEGACY`;
5. registrar a origem no Discovery e propagá-la para `status.yaml` da Spec;
6. se a origem estiver ambígua e mudar o processo, retornar `REQUIRES_REVIEW`.

`requirements.source`:

- `BUSINESS` → requisito/`business.md` existente; não exigir PRD;
- `PRD` → PRD existente;
- `INTERACTIVE` → consolidar requisito antes de concluir o Discovery.

## Processo

1. compreender objetivo e escopo;
2. identificar módulos/domínios relevantes;
3. identificar Features/Capacidades funcionais;
4. decompor cada Feature em casos de uso independentes e verificáveis;
5. registrar evidência/origem de cada caso de uso;
6. mapear dependências entre casos de uso;
7. identificar integrações e persistência relevantes;
8. avaliar complexidade, risco, valor e fundações;
9. calcular ordem mínima válida;
10. agrupar casos independentes em Waves;
11. definir prioridade de criação de Specs;
12. definir prioridade de implementação;
13. registrar dúvidas críticas;
14. validar `GATE-PLN`.

## Decomposição funcional

Hierarquia: `FEATURE -> USE CASE -> comportamento funcional verificável`.

Feature MUST NOT ser tratada automaticamente como um único caso de uso.

Operações SHOULD ser separadas quando puderem possuir fluxo, regra, permissão,
critério de aceite, dependência, prioridade, implementação ou teste próprios.

MUST NOT agrupar automaticamente operações independentes em `Manter <Entidade>`.

MUST NOT assumir CRUD completo apenas porque uma entidade foi identificada.
Somente criar casos de uso suportados por requisito ou evidência.
Operação plausível e não especificada MAY ser registrada como `REQUIRES_REVIEW`,
mas MUST NOT ser tratada como requisito confirmado.

## Dependências

Toda dependência deve possuir motivo e evidência suficiente.

Classificar quando possível: `FUNCTIONAL`, `DATA`, `TECHNICAL`, `INTEGRATION`.

Registrar também: `Bloqueante: YES | NO`, evidência e impacto na ordem/Wave.

MUST NOT criar dependência apenas por conveniência de implementação.

## Planejamento

Considerar: pré-requisitos, valor de negócio, fundações, risco, complexidade,
integração externa, prontidão e paralelismo.

Prioridade de criação de Specs e prioridade de implementação são independentes.

MUST NOT ordenar apenas por ID.

## LEGACY

Código legado é fonte de evidência funcional/técnica.
MUST NOT copiar automaticamente sua arquitetura nem inferir tecnologia alvo a partir dele.

### 📌 Considerações NEW/LEGACY

- **NEW**: Discovery do zero. Sem código anterior como fonte obrigatória. Foco em compreender produto, casos de uso, dependências do zero. Quando origin `NEW`, a skill deve priorizar inventário funcional e mapa de dependências sem restrições de comportamento legado.

- **LEGACY**: Preservação de comportamento existente. Código, telas, banco ou sistema anterior são fonte necessária para descobrir/validar comportamento. Quando origin `LEGACY`, a skill deve consultar evidências do legado (via `config/paths.yaml legacy.root`) antes de registrar novos casos de uso, garantindo que não se perca comportamento anterior intencionalmente.

- **Mesmo projeto**: Um projeto MAY conter features `NEW` e `LEGACY` simultaneamente. Nesse caso, o Discovery deve registrar a origem de cada feature separadamente e propagá-la para `status.yaml` da Spec (ver item 5 do processo).

## Saídas

Gerar/atualizar, usando os assets:

- `discovery/system.md` — asset `assets/system.md`;
- `discovery/modules.md` — asset `assets/modules.md`;
- `discovery/use-cases.md` — asset `assets/use-cases.md`;
- `discovery/dependencies.md` — asset `assets/dependency-map.md`;
- `discovery/prioritization.md` — asset `assets/prioritization.md`.

## Encerramento

Validar `gates/planejamento.md` (`GATE-PLN`) e parar.
Ao concluir, atualizar `state/current.yaml` e `state/spec-index.yaml` quando
existirem Specs afetadas (`rules/context/session-lifecycle.md`).

Decisão funcional crítica ausente: `REQUIRES_REVIEW`.

## Referências

- `rules/migration/migration.md`
- `rules/context/context-economy.md`
