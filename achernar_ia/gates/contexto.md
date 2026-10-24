---
name: gate-contexto
type: gate
description: "GATE-CTX — Gate de Contexto"
---

# GATE-CTX — Gate de Contexto

## Objetivo

Garantir que o agente possui contexto suficiente e delimitado antes de iniciar a
execução, sem carregar especialidades desnecessárias.

## Momento

No início de qualquer unidade de execução que produza ou altere artefato.

`NOT_APPLICABLE` para consulta de status, navegação e resposta informativa.

## Executor

O Agent da intenção. O orquestrador confirma quando controla a transição.

## Critérios

- [ ] intenção classificada em uma única intenção principal;
- [ ] escopo delimitado (Spec/UC ou alvo explícito);
- [ ] arquivos do contrato **Ler sempre** do Agent carregados;
- [ ] arquivos de **Ler conforme** carregados apenas quando aplicáveis ao escopo;
- [ ] `config/technology.yaml` carregado somente em intenção técnica;
- [ ] artefatos obrigatórios da Spec carregados quando a execução atua sobre ela;
- [ ] `context-manifest.yaml` criado/atualizado quando a execução atua sobre uma Spec;
- [ ] nenhuma Skill, Rule, Corporate ou Spec carregada por precaução;
- [ ] decisões bloqueantes conhecidas foram registradas.

## Evidência

- escopo declarado;
- relação dos arquivos efetivamente carregados e o motivo (manifesto, quando aplicável).

## Resultado

- `PASS` — contexto suficiente e delimitado;
- `NOT_APPLICABLE` — consulta sem alteração de artefato;
- `REQUIRES_REVIEW` — informação obrigatória inexistente ou inobtenível.

## Falha

1. carregar somente o que falta;
2. se o contexto obrigatório não puder ser obtido, parar e retornar `REQUIRES_REVIEW`;
3. MUST NOT iniciar a execução "no escuro" nem ampliar o escopo para compensar.

## Referências

- `rules/context/context-economy.md`
- `rules/context/session-lifecycle.md`
