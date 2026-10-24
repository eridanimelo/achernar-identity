---
name: gate-analise
type: gate
description: "GATE-ANL — Gate de Análise"
---

# GATE-ANL — Gate de Análise

## Objetivo

Garantir que o código existente — inclusive legado, quando aplicável — foi
analisado antes de propor ou criar alterações, e que o reuso foi avaliado.

## Momento

Antes de criar ou alterar código, schema ou estrutura compartilhada.

Aplica-se às intenções `BACKEND`, `FRONTEND`, `DATABASE`, `ARCHITECTURE` e `MIGRATION`.

## Executor

O Agent da intenção que vai alterar.

## Critérios

- [ ] código existente no escopo localizado e analisado;
- [ ] padrões e convenções vigentes identificados;
- [ ] Reuse Scan executado conforme `rules/reuse/reuse.md`;
- [ ] para Backend/Frontend, especialista `subagents/reuse/<camada>/AGENT.md` executado (preferencialmente como subagente isolado quando suportado);
- [ ] resultado do Reuse Scan registrado: `REUSE | COMPOSE | EXTEND | CREATE` (ou `MIXED`);
- [ ] **para Frontend: aderência a `rules/frontend/angular-architecture.md` validada (feature-based, smart/presentational, models por feature, feature services, templates separados, lazy loading — sem `ApiService`/`HttpClient` em componente, sem pastas vazias);**
- [ ] estrutura nova justificada quando não houver equivalente;
- [ ] impacto em consumidores avaliado quando o artefato for compartilhado;
- [ ] para `LEGACY`, evidência AS-IS registrada quando o requisito vier do legado.

## Evidência

- artefatos existentes analisados;
- decisão de reuso e justificativa para criação nova.

## Resultado

- `PASS` — análise concluída e decisão registrada;
- `REQUIRES_REVIEW` — evidência insuficiente para alterar com segurança.

## Falha

MUST NOT criar estrutura nova nem alterar artefato compartilhado sem este Gate.

## Referências

- `rules/reuse/reuse.md`
- `rules/architecture/layers.md`
- `rules/migration/migration.md`
