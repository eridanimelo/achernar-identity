---
name: corporate-testing-skill
type: skill
description: "Test Specification Skill"
---

# Test Specification Skill

## Objetivo

Criar ou revisar `tests.md`, especificando os cenários que deverão ser validados
posteriormente.

Esta Skill NÃO executa testes durante a criação da Spec.

## Entradas

Consultar os artefatos aplicáveis:

- `business.md`;
- `api.md`;
- `database.md`, quando relevante;
- `frontend.md`;
- `security.md`.

MUST NOT carregar `config/technology.yaml` para criar `tests.md`.

## Processo de Specification

1. mapear critérios de aceite;
2. mapear regras de negócio;
3. identificar fluxos positivos;
4. identificar fluxos alternativos;
5. identificar erros e limites;
6. considerar contratos de API;
7. considerar comportamento de UI/UX;
8. considerar controles de Security;
9. garantir rastreabilidade;
10. registrar pontos indefinidos;
11. gerar `tests.md` usando `templates/tests.md`.

## Saída

`specs/<ID>-<slug>/tests.md`

O arquivo representa:

> o que deverá ser testado

e NÃO:

> o resultado dos testes.

## Depois da implementação

Na fase de implementação/review, `tests.md` pode ser utilizado para:

1. implementar testes automatizados;
2. orientar testes manuais quando aplicáveis;
3. executar validações;
4. comparar resultado real com resultado esperado;
5. produzir evidências de execução.

Essas atividades não fazem parte desta Skill de Specification.

## Gate

Se o comportamento necessário estiver indefinido:

`REQUIRES_REVIEW`

MUST NOT inventar resultado esperado.
