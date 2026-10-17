---
name: rule-context-context-economy
type: rule
description: "Rule — Economia de Contexto"
---

# Rule — Economia de Contexto

## Objetivo

Usar o contexto mínimo suficiente para executar corretamente a tarefa, sem comprometer Spec, segurança, qualidade ou rastreabilidade.

---

## 1. Escopo da execução

Cada execução SHOULD possuir uma intenção principal e escopo limitado.

MUST NOT carregar contexto de outras etapas, especialidades ou UCs sem necessidade concreta.

Ao concluir uma unidade de trabalho:

1. atualizar o estado aplicável;
2. atualizar índices aplicáveis;
3. persistir decisões necessárias;
4. descartar contexto bruto desnecessário;
5. aplicar a política definida em `rules/context/session-lifecycle.md`.

Caminhos de estado, Specs, Discovery, requisitos, Reviews, Backend e Frontend MUST ser resolvidos por `config/paths.yaml`.

MUST NOT assumir caminhos físicos fora dessa configuração.

---

## 2. Leitura sob demanda

Antes de qualquer leitura ou tool call, avaliar:

> Esta informação é necessária para executar ou validar a tarefa atual?

Se não for necessária, MUST NOT carregá-la.

Preferir:

`localizar -> selecionar -> ler trecho necessário`

em vez de:

`ler tudo -> procurar no contexto`.

Quando suportado, SHOULD utilizar:

* busca por símbolo;
* busca por texto;
* heading ou seção;
* intervalo de linhas;
* referências previamente identificadas.

MUST parar de coletar contexto assim que houver informação suficiente.

Leitura integral continua permitida quando necessária para compreender ou validar corretamente o artefato.

---

## 3. Arquivos auxiliares

Arquivos como:

* `README*`;
* `CHANGELOG*`;
* `LICENSE*`;
* documentação geral;
* logs;
* artefatos de build;
* arquivos gerados;

MUST NOT ser carregados automaticamente durante investigação ou implementação.

MAY ser carregados quando forem explicitamente necessários para a tarefa atual ou declarados obrigatórios pelo contrato de contexto do Agent/Skill.

---

## 4. Código-fonte

Código-fonte MUST ser localizado antes de ser lido.

As raízes do código MUST ser obtidas de `config/paths.yaml`, como `backend.root` e `frontend.root`, quando aplicáveis.

Quando possível, SHOULD carregar somente símbolos, classes, funções, métodos, componentes ou trechos necessários à tarefa.

Antes de incorporar código-fonte ao contexto, MAY aplicar sanitização local e determinística para remover:

* comentários sem relevância funcional;
* blocos de documentação não necessários;
* linhas vazias excessivas.

MUST preservar conteúdo semanticamente relevante, incluindo quando aplicável:

* strings e literais;
* anotações e decorators;
* assinaturas;
* tipos;
* imports necessários à compreensão;
* comentários com informação funcional relevante.

A sanitização MUST ocorrer localmente e MUST NOT exigir chamada a LLM.

MUST NOT sanitizar quando comentários ou documentação forem objeto da análise.

---

## 5. Outputs extensos

Comandos, builds, testes, logs, browser e processos potencialmente verbosos MUST evitar incorporar output completo quando um recorte for suficiente.

SHOULD preferir:

* redirecionamento para arquivo temporário;
* `tail`;
* `grep`;
* filtros;
* busca específica;
* resumo de erros;
* asserts e evidências relevantes.

Processos longos MAY ser executados em background quando apropriado.

O contexto SHOULD receber somente a evidência necessária para executar ou validar a tarefa atual.

---

## 6. Estado e Specs

Para consultas de status, pendências ou próxima etapa, consultar primeiro:

1. `current.yaml` sob `workspace.state`;
2. `spec-index.yaml` sob `workspace.state`, somente se necessário.

Se a informação encontrada for suficiente, MUST encerrar a coleta.

MUST NOT abrir Specs, Discovery, Reviews ou código-fonte apenas para reconfirmar informação já consolidada nesses índices.

Para implementação, os artefatos obrigatórios definidos pelo contrato do Agent/Skill continuam obrigatórios.

Specs MUST ser resolvidas a partir de `workspace.specs`.

Discovery MUST ser resolvido a partir de `workspace.discovery`.

Reviews MUST ser resolvidas a partir de `workspace.reviews`.

Memória, resumo ou histórico de execução anterior MUST NOT substituir fonte de verdade obrigatória.

---

## 7. Investigações

Investigações exploratórias SHOULD utilizar contexto isolado ou subagente quando o ambiente oferecer essa capacidade e houver benefício real.

Exemplos:

* localizar implementação;
* mapear arquivos relacionados;
* identificar componentes reutilizáveis;
* investigar dependências;
* executar Reuse Scan;
* localizar testes existentes.

O subagente SHOULD receber escopo pequeno e retornar somente resultado estruturado necessário à execução principal.

MUST NOT usar subagente como justificativa para ignorar fontes obrigatórias.

---

## 8. Lifecycle

Decisões sobre:

* continuar na sessão;
* compactar contexto;
* criar nova sessão;
* checkpoint;
* persistência de estado entre unidades;

MUST seguir `rules/context/session-lifecycle.md`.

Esta Rule MUST NOT duplicar a política de Lifecycle.

---

## 9. Prioridade

Correção, segurança, cumprimento da Spec e fontes de verdade obrigatórias têm prioridade sobre economia de tokens.
