---
name: gate-qualidade
type: gate
description: "GATE-QLT — Gate de Qualidade e Segurança"
---

# GATE-QLT — Gate de Qualidade e Segurança

## Objetivo

Garantir que a unidade implementada foi validada por execução real antes de ser
considerada concluída.

## Momento

`IMPLEMENTATION → DONE`, antes de `GATE-CLS`.

Escopo: somente o que foi implementado.

## Executor

O Agent que implementou, com apoio de `subagents/testing` e `subagents/security` quando aplicável.

## Critérios

- [ ] compilação/build do escopo executado, quando aplicável;
- [ ] lint/análise estática aplicáveis executados, quando existirem no projeto;
- [ ] testes aplicáveis executados;
- [ ] cenários de `tests.md` referentes ao escopo cobertos;
- [ ] requisitos de `security.md` do escopo verificados;
- [ ] nenhuma pendência crítica ocultada;
- [ ] cenários não executáveis registrados com motivo;
- [ ] **para Frontend: validação de arquitetura (`rules/frontend/angular-architecture.md`) — models em feature, feature services, smart/presentational, templates >50 linhas separados, lazy loading (`loadChildren` preferido, `loadComponent` aceito), sem `ApiService`/`HttpClient` em componente, `app.component.ts` mínimo, sem pastas vazias, ESLint rules passando quando configurado.**

## Evidência

- resumo dos comandos executados e do resultado (não o log completo);
- falhas relevantes, quando houver.

## Resultado

- `PASS` — validações aplicáveis executadas e sem pendência crítica;
- `REQUIRES_REVIEW` — impossibilidade de executar validação essencial;
- `FAIL` — falha objetiva de teste, build ou segurança.

## Falha

Corrigir e reavaliar. MUST NOT declarar concluído o que não foi validado.

## Referências

- `rules/quality/quality.md`
- `rules/context/context-economy.md` (saída de terminal/build)
- `corporate/testing/**`, `corporate/security/**` quando aplicável
