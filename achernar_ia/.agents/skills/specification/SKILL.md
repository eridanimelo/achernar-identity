---
name: specification
type: skill
description: >-
  Criar ou revisar artefatos de Specification sem implementar: business.md, api.md,
  database.md, frontend.md, security.md, tests.md, status.yaml e manifesto de
  contexto. Usar em solicitações de requisito, especificação funcional, Spec de
  feature, PRD para Spec ou revisão de artefato de Spec.
---

# Skill: Specification

## Quando usar

Quando a solicitação for criar ou revisar um requisito ou artefato de Spec.

## Quando não usar

Para Discovery, arquitetura, implementação, testes de execução ou revisão final.

## Regra fundamental

Specs descrevem **o que o sistema deve fazer** e MUST NOT definir tecnologia de implementação.

MUST NOT carregar `config/technology.yaml` durante criação/revisão funcional da Spec.

### 📌 Origem NEW/LEGACY

- **NEW**: Feature sem implementação anterior obrigatória. A Spec pode definir do zero o comportamento esperado, sem necessidade de preservar funcionalidades anteriores. Quando a origem for `NEW`, a skill deve permitir que a Spec descreva fluxos novos sem restrições de compatibilidade com versões passadas.

- **LEGACY**: Feature que depende de comportamento de sistema anterior. A Spec deve registrar quais partes do legado devem ser mantidas, substituídas ou estendidas. Quando a origem for `LEGACY`, a skill deve orientar o registo de evidências do sistema anterior e garantir que a Spec não quebre comportamentos existentes sem justificativa explícita.

- **Transição NEW → LEGACY**: Se uma feature iniciar como `NEW` e, no futuro, precisar preservar comportamento, a origem deve ser atualizada para `LEGACY` e o `status.yaml` da Spec deve refletir essa mudança.

MUST NOT carregar `config/technology.yaml` durante criação/revisão funcional da Spec.

## Contrato

Seguir `rules/specification/spec-structure.md`.

## Manifesto de contexto

Criar ou atualizar `specs/<ID>-<slug>/context-manifest.yaml` usando
`assets/context-manifest.yaml` quando a solicitação atuar efetivamente sobre uma Spec, por exemplo:

- criar/revisar `business.md`;
- criar/revisar a Spec completa;
- criar/revisar um artefato específico;
- investigar inconsistência daquela Spec.

MUST NOT criar manifesto apenas para listar Specs, responder status global,
consultar priorização ou identificar pendências pelo índice.

O manifesto MUST registrar, para cada arquivo realmente necessário: caminho,
motivo da leitura, artefato dependente e condição de aplicabilidade.

Uma Spec existente só deve ser consultada quando a execução atuar nela, houver
dúvida de estrutura/consistência ou a solicitação for de revisão.

## Modos de execução

### `REQUIREMENTS`

Usar quando a solicitação tratar somente de requisito, regra de negócio ou `business.md`.

Carregar:

- `config/PROJECT.yaml`;
- `.agents/skills/specification/SKILL.md`;
- `rules/specification/spec-structure.md`;
- `corporate/requirements/AGENT.md`, `RULES.md`, `SKILL.md` e `templates/business.md`;
- `business.md` existente somente quando a intenção for revisão.

MUST NOT carregar backend, frontend, UI/UX, API, database ou `config/technology.yaml`.

Ao encerrar:

1. criar ou revisar `business.md`;
2. atualizar `context-manifest.yaml`;
3. atualizar `status.yaml` com o conteúdo realmente concluído;
4. interromper o fluxo;
5. retornar `REQUIRES_REVIEW`.

MUST NOT gerar `api.md`, `database.md`, `frontend.md`, `security.md` ou
`tests.md` nesse modo, mesmo que a feature possa precisar deles depois.

### `FULL_SPEC`

Usar somente quando a solicitação pedir explicitamente criação/revisão da Spec completa.
Cada artefato chama sua especialidade responsável apenas quando aplicável.

### `ARTIFACT`

Usar quando a solicitação pedir somente um artefato específico.
Carregar somente a especialidade responsável e as dependências mínimas.

## Mapeamento de responsabilidade

| Artefato | Responsável | Recurso |
|---|---|---|
| `business.md` | `corporate/requirements` | `corporate/requirements/templates/business.md` |
| `api.md` | `corporate/api` | `corporate/api/templates/api.md` |
| `database.md` | `corporate/database` | `corporate/database/templates/database.md` |
| `frontend.md` | `corporate/ui-ux` | `corporate/ui-ux/templates/frontend.md` |
| `security.md` | `corporate/security` | `corporate/security/templates/security.md` |
| `tests.md` | `corporate/testing` | `corporate/testing/templates/tests.md` |
| `status.yaml` | Specification | `assets/status.yaml` |
| `evidence.md` | Migration | `.agents/skills/migration/assets/evidence.md` |
| `migration.md` | Migration | `.agents/skills/migration/assets/migration.md` |

## Processo para FULL_SPEC

1. criar o manifesto antes da primeira leitura específica;
2. localizar a feature no Discovery, quando existir;
3. resolver a origem `NEW | LEGACY` da feature pelo Discovery/Spec/solicitação humana;
4. criar/localizar `specs/<ID>-<slug>/`;
5. se `LEGACY`, coletar somente as evidências necessárias;
6. gerar/revisar `business.md`;
7. gerar somente artefatos aplicáveis, carregando cada especialidade sob demanda;
8. gerar `tests.md` com cenários, sem executar testes;
9. gerar `evidence.md`/`migration.md` para `LEGACY` quando aplicáveis;
10. atualizar `status.yaml` após cada artefato, refletindo apenas o que foi concluído;
11. verificar consistência cruzada;
12. retornar `REQUIRES_REVIEW` se houver decisão crítica.

## Encerramento obrigatório

Ao terminar Specification:

- MUST parar;
- MUST NOT iniciar backend ou frontend;
- MUST NOT escolher tecnologia;
- MUST NOT executar implementação automaticamente.

A implementação exige nova intenção e `GATE-AUT` (`gates/autorizacao.md`).

## Referências

- `rules/specification/spec-structure.md`
- `rules/context/context-economy.md`
