---
name: gate-planejamento
type: gate
description: "GATE-PLN — Gate de Planejamento (Discovery → Specification)"
---

# GATE-PLN — Gate de Planejamento (Discovery → Specification)

## Objetivo

Garantir que o Discovery produziu planejamento suficiente antes de criar Specs.

## Momento

Transição `DISCOVERY → SPECIFICATION`.

## Executor

`agents/discovery/AGENT.md`, com confirmação do orquestrador.

## Critérios

- [ ] fonte de requisitos identificada;
- [ ] origem `NEW | LEGACY` definida por caso de uso/feature quando relevante;
- [ ] módulos/domínios relevantes identificados quando aplicável;
- [ ] casos de uso identificados;
- [ ] casos de uso possuem origem/evidência suficiente;
- [ ] `dependencies.md` atualizado;
- [ ] dependências bloqueantes identificadas;
- [ ] riscos avaliados;
- [ ] complexidade avaliada;
- [ ] ordem de criação das Specs definida;
- [ ] ordem de implementação definida;
- [ ] Waves definidas;
- [ ] `UNKNOWN` crítico resolvido ou explicitamente aceito;
- [ ] revisão humana registrada quando exigida.

## Evidência

- `discovery/system.md`, `modules.md`, `use-cases.md`, `dependencies.md`, `prioritization.md`
  preenchidos com conteúdo real, sem placeholders.

## Resultado

- `PASS` — itens bloqueantes atendidos;
- `REQUIRES_REVIEW` — `UNKNOWN` crítico ou dependência bloqueante em aberto.

## Falha

MUST NOT avançar para Specification com item bloqueante pendente.

## Referências

- `skills/discovery/SKILL.md`
- `workflows/feature.md`
