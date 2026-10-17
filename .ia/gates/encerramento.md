---
name: gate-encerramento
type: gate
description: "GATE-CLS — Gate de Encerramento"
---

# GATE-CLS — Gate de Encerramento

## Objetivo

Garantir que a entrega atende ao objetivo, respeitou o escopo e não introduziu
alterações desnecessárias antes de ser considerada finalizada.

## Momento

Antes de declarar a unidade concluída.

## Executor

O orquestrador, com base na revisão (`agents/review`) quando aplicável.

## Critérios

- [ ] objetivo solicitado atendido;
- [ ] somente o escopo solicitado foi executado;
- [ ] contratos e Rules aplicáveis respeitados;
- [ ] nenhuma alteração desnecessária introduzida;
- [ ] nenhuma área não executada foi marcada como concluída;
- [ ] pendências reais registradas;
- [ ] estado atualizado (`status.yaml`, `state/current.yaml`/índice quando aplicável);
- [ ] revisão registrada em `reviews/` quando exigida;
- [ ] `GATE-QLT` satisfeito para o escopo;
- [ ] próxima etapa não iniciada automaticamente.

## Isolamento

Se a solicitação foi somente backend, frontend não é requisito para concluir o
backend salvo dependência explícita — e vice-versa.

## Evidência

- resultado de `GATE-QLT` para o escopo concluído;
- findings de `reviews/` quando a revisão foi exigida para o escopo;
- `status.yaml` e `state/current.yaml`/índice refletindo o que foi realmente feito;
- lista do que foi executado, comparada ao escopo originalmente solicitado.

## Resultado

- `PASS` — conclusão autorizada;
- `REQUIRES_REVIEW` — decisão humana ou pendência não resolvida;
- `FAIL` — objetivo não atendido ou desvio de escopo.

## Falha

1. registrar o desvio (escopo não atendido, alteração desnecessária introduzida
   ou pendência bloqueante aberta);
2. MUST NOT declarar a unidade concluída enquanto o desvio não for resolvido
   ou explicitamente aceito por decisão humana;
3. retornar `REQUIRES_REVIEW` (decisão humana pendente) ou `FAIL` (desvio
   objetivo de escopo/Spec/contrato).

## Referências

- `skills/review/SKILL.md`
- `gates/qualidade.md`
- `rules/context/session-lifecycle.md`
