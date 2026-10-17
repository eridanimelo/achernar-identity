---
name: rule-migration-migration
type: rule
description: "Migração"
---

# Migração

Código legado é fonte de evidência funcional.

MUST NOT utilizar automaticamente arquitetura do legado como modelo para a nova solução.

## Descoberta

Antes de migrar uma funcionalidade:
1. identificar comportamento existente;
2. identificar regras de negócio;
3. identificar entradas e saídas;
4. identificar integrações;
5. identificar persistência;
6. identificar dependências;
7. registrar incertezas.

## Evidências

Regras descobertas no legado devem possuir evidência rastreável quando possível.

MUST NOT inventar regra de negócio ausente.

Quando não houver evidência suficiente:

`REQUIRES_REVIEW`

## Specs

Specs aprovadas passam a ser a fonte funcional principal para a nova implementação.
