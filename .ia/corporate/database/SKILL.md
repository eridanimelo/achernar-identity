---
name: database
type: skill
description: "Database Skill"
---

# Database Skill

## Objetivo
Criar ou revisar `database.md` como modelo lógico de dados da feature.

## Entradas
- `business.md`;
- `api.md`, quando disponível;
- requisitos de auditoria/segurança aplicáveis;
- evidências do legado, somente para `LEGACY`;
- `rules/specification/spec-structure.md`.

MUST NOT carregar `config/technology.yaml` para criar `database.md`.

## Processo
1. identificar informações persistentes;
2. separar dado persistente de dado calculado/transitório;
3. identificar estruturas lógicas;
4. definir campos funcionais;
5. definir relacionamentos e cardinalidades;
6. definir integridade/unicidade;
7. avaliar histórico/auditoria quando requerido;
8. rastrear decisões;
9. registrar dúvidas;
10. gerar `database.md` usando `templates/database.md`.

## LEGACY
Comparar estrutura existente apenas quando necessário para entender comportamento ou migração.
MUST NOT reproduzir automaticamente o modelo físico legado.

## Gate
Decisão crítica de dados ausente → `REQUIRES_REVIEW`.
