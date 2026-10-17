---
name: rule-architecture-principles
type: rule
description: "Princípios de Arquitetura"
---

# Princípios de Arquitetura

A arquitetura deve favorecer:
- baixo acoplamento;
- alta coesão;
- separação de responsabilidades;
- testabilidade;
- manutenibilidade;
- legibilidade;
- evolução incremental.

## SOLID

Os princípios SOLID devem ser aplicados quando contribuírem para clareza e manutenção.

MUST NOT criar abstrações apenas para demonstrar aplicação de SOLID.

## DDD

Quando existir domínio relevante:
- regras de negócio devem refletir a linguagem do domínio;
- nomes devem utilizar o vocabulário definido pelas Specs;
- conceitos de domínio não devem ser espalhados arbitrariamente pelo sistema;
- infraestrutura não deve definir regras de negócio.

DDD deve ser aplicado proporcionalmente à complexidade do domínio.

MUST NOT introduzir complexidade arquitetural desnecessária.

## MVC

Quando a arquitetura configurada utilizar MVC ou estrutura equivalente, deve existir separação clara entre entrada, aplicação, domínio e persistência.

As responsabilidades detalhadas estão em `rules/architecture/layers.md`.

## Simplicidade

Preferir a solução mais simples que:
- respeite as regras de negócio;
- mantenha separação de responsabilidades;
- permita testes;
- permita evolução futura.

MUST NOT criar abstrações especulativas.
