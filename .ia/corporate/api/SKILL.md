---
name: api
type: skill
description: "API Skill"
---

# API Skill

## Objetivo
Criar ou revisar `api.md` a partir da Spec funcional, sem definir tecnologia de implementação.

## Entradas
- `business.md`;
- `security.md`, quando aplicável;
- `frontend.md`, quando necessário para compreender interações;
- integrações/documentação aprovada;
- `rules/specification/spec-structure.md`.

MUST NOT carregar `config/technology.yaml` para criar `api.md`.

## Processo
1. identificar capacidades funcionais;
2. mapear operações necessárias;
3. definir contrato de cada operação;
4. definir entradas e saídas;
5. definir erros;
6. aplicar requisitos de segurança;
7. definir filtros/paginação quando necessários;
8. rastrear operações para regras/critérios;
9. registrar dúvidas;
10. gerar `api.md` usando `templates/api.md`.

## Gate
MUST NOT inventar contrato para requisito indefinido.
Bloqueio funcional → `REQUIRES_REVIEW`.
