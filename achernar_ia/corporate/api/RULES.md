---
name: corporate-api-rules
type: rule
description: "API Rules"
---

# API Rules

## Neutralidade tecnológica
`api.md` é contrato funcional/técnico de interface, não desenho de implementação.

MUST NOT citar framework, linguagem, annotation, classe ou biblioteca.

## Contratos
Cada operação aplicável deve deixar explícitos:
- objetivo;
- método semântico da operação;
- recurso/caminho quando houver API HTTP;
- entradas;
- obrigatoriedade e restrições funcionais;
- saída;
- erros esperados;
- autenticação/autorização conforme `security.md`;
- paginação/filtros/ordenação quando necessários;
- rastreabilidade.

## Consistência
MUST:
- derivar operações do `business.md`;
- respeitar `security.md`;
- evitar retornar dados não necessários;
- manter nomenclatura consistente;
- diferenciar ausência, validação e conflito quando funcionalmente relevantes.

## Persistência
MUST NOT copiar automaticamente o modelo de `database.md` para o contrato externo.

## Dúvidas
Decisão bloqueante ausente → `REQUIRES_REVIEW`.
Item que não se aplica → `NOT_APPLICABLE`.
