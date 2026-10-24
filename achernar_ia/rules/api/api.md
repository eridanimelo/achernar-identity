---
name: rule-api-api
type: rule
description: "API"
---

# API

Contratos de API devem ser claros, previsíveis e documentáveis.

## Contratos

Requests e responses devem possuir contratos explícitos.

MUST NOT expor estruturas internas de persistência diretamente como contrato externo.

## Endpoints

Endpoints devem:
- representar claramente sua intenção;
- utilizar semântica consistente;
- validar entradas;
- retornar erros padronizados;
- evitar exposição desnecessária de dados.

## Compatibilidade

Alterações incompatíveis em contratos existentes devem ser identificadas antes da implementação.

## Segurança

Quando a feature exigir segurança:
- endpoints devem respeitar autenticação aplicável;
- autorização deve ocorrer antes da execução da operação protegida;
- permissões devem considerar recurso e operação;
- dados não autorizados MUST NOT ser retornados.

Quando houver pacote corporativo de segurança aplicável, ele é a referência normativa principal para segurança.
