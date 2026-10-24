---
name: rule-jira-workflow
type: rule
description: "Jira Workflow"
---

# Jira Workflow

Quando Jira estiver habilitado em `config/jira.yaml`, as operações devem seguir o workflow configurado.

## Criação

Quando o fluxo utilizar Specs:
1. Spec deve estar concluída;
2. Spec deve estar aprovada quando exigido;
3. priorização deve ocorrer;
4. somente então a issue pode ser criada.

## Associação

Toda issue criada a partir de uma Spec deve manter referência àquela Spec.

A implementação pode iniciar:
- pela Spec;
- pela issue Jira vinculada.

Quando iniciar pelo Jira, localizar a Spec associada antes da implementação.

## Hierarquia

Tipos de issue e relacionamentos devem seguir `config/jira.yaml`.

MUST NOT assumir projeto Jira fixo.
