---
name: rule-jira-integrity
type: rule
description: "Integridade Jira"
---

# Integridade Jira

Jira não é a fonte principal de regras funcionais quando existir Spec aprovada.

MUST NOT sobrescrever conteúdo funcional da Spec com informações vindas da issue Jira.

## Chaves

MUST NOT inventar chave Jira.

MUST NOT registrar link para issue antes da criação real da issue.

Após a criação:
- capturar chave real;
- capturar link real;
- atualizar somente os metadados de integração permitidos.

## Idempotência

Antes de criar uma issue, verificar se já existe vínculo correspondente.

MUST NOT criar issues duplicadas para a mesma operação.
