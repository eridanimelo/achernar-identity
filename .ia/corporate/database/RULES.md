---
name: corporate-database-rules
type: rule
description: "Database Rules"
---

# Database Rules

## Neutralidade tecnológica
`database.md` descreve o modelo lógico dos dados.

MUST NOT citar SGBD, ORM, annotation, framework ou ferramenta de migração.

## Persistência
Persistir somente dados necessários ao comportamento aprovado.

## Integridade
Quando aplicável, explicitar:
- obrigatoriedade;
- unicidade;
- relacionamento;
- cardinalidade;
- domínio/valores permitidos;
- consistência entre estruturas;
- retenção ou histórico quando houver requisito.

## Identificadores
Descrever necessidade funcional de identificação sem impor mecanismo tecnológico.

## Relacionamentos
Todo relacionamento deve possuir justificativa funcional.

## Legado
Estrutura existente é evidência.
MUST NOT copiar tabela, coluna, trigger, sequence ou procedure apenas porque existe no legado.

## Dúvidas
Informação necessária não determinada → `UNKNOWN` ou `REQUIRES_REVIEW`.
Conceito não aplicável → `NOT_APPLICABLE`.
