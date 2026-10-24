---
name: jira-generic
type: skill
description: "Skill: Jira Projects"
---

# Skill: Jira Projects

## Objetivo

Consultar e operar Jira de forma genérica usando MCP, sempre com base na configuração ativa do projeto.

Suporta:
- consulta de projetos não arquivados;
- resolução do projeto configurado;
- consulta de componentes;
- criação de Epic, Story, Task, Subtask e Bug;
- associação de Jira Issue com Spec;
- implementação iniciada por Jira ou por Spec.

## Fonte oficial

Use sempre MCP Jira para dados dinâmicos.

Nunca inventar:
- project key;
- project name;
- component;
- issue type;
- issue key;
- status;
- prioridade.

## Projeto ativo

O projeto ativo MUST vir de:

`config/jira.yaml`

Exemplo:

```yaml
project:
  key: PROJECT_KEY
  name: MY-um projeto específico-SOFTWARE
```

O mesmo framework pode ser usado em qualquer outro projeto alterando apenas a configuração.

Quando `validateProjectViaMcp=true`:
- validar o `project.key`;
- validar o nome;
- usar os valores retornados pelo MCP como fonte de verdade.

## Projetos acessíveis

Quando solicitado listar projetos:
- consultar apenas projetos não arquivados, salvo pedido contrário;
- retornar `KEY — NOME`;
- não limitar a consulta ao projeto configurado.

## Componentes

Quando `components.resolveViaMcp=true`:
- consultar componentes do projeto ativo;
- não inventar componente;
- marcar componente arquivado quando a API/MCP informar isso.

## Criação de issues

Tipos permitidos vêm de `config/jira.yaml`.

### Epic

Título: [ÉPICO] <Nome>

Contexto:
<problema de negócio>

Objetivo:
<resultado esperado>

Descrição:
<escopo macro>

Critérios de aceite:
- [ ] Critério 1
- [ ] Critério 2

Tempo de entrega:
- Início:
- Entrega alvo:
- Estimativa:

Dependências:
<times/sistemas>

### Story / Task

Título: [HISTÓRIA] <Nome da funcionalidade>

Contexto:
<necessidade>

Descrição:
Como <perfil>,
quero <ação>,
para <benefício>.

Critérios de aceite:
- [ ] Critério 1
- [ ] Critério 2

Tempo de entrega:
- Estimativa:
- Entrega alvo:

Spec:
<caminho da Spec associada>

### Subtask

Título: [SUBTASK] <Ação objetiva>

Parent:
<issue key>

Descrição:
<escopo técnico>

Critérios de aceite:
- [ ] Item 1
- [ ] Item 2

Tempo de entrega:
- Estimativa:
- Entrega alvo:

Spec:
<caminho da Spec associada>

### Bug

Título: [BUG] <Resumo>

Contexto:
<onde ocorre e impacto>

Descrição do problema:
<comportamento atual>

Passos para reproduzir:
1.
2.
3.

Resultado esperado:
<comportamento correto>

Resultado atual:
<erro observado>

Critérios de aceite:
- [ ] Bug corrigido
- [ ] Sem regressão

Spec:
<caminho da Spec associada>

## Integração Jira ↔ Spec

A Spec permanece a fonte de verdade funcional.

Jira representa planejamento e acompanhamento.

Quando criar uma issue:
- MUST associar o caminho da Spec;
- SHOULD atualizar `status.yaml` com issue key e projeto;
- MUST NOT duplicar ou substituir a Spec inteira na descrição da issue.

Quando implementação começar por Jira:
1. consultar issue no MCP;
2. resolver Spec associada;
3. validar Spec;
4. seguir workflow normal.

Quando implementação começar por Spec:
- Jira MAY ser consultado se habilitado;
- ausência de issue não substitui a Spec;
- criar issue somente se o workflow configurado exigir.

## Regras

- MUST usar o projeto ativo da configuração.
- MUST NOT hardcodar um projeto específico ou qualquer outro projeto.
- MUST NOT assumir que o usuário participa de apenas um projeto.
- MUST validar dados críticos no MCP quando configurado.
