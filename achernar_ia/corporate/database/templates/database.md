---
name: corporate-database-template-database
type: template
description: "Database Spec — [ID] [Nome]"
---

# Database Spec — [ID] [Nome]

## 0. Identificação
- ID:
- Nome:
- Origem: `NEW | LEGACY`
- Status: `DRAFT | REVIEW | APPROVED`

## 1. Objetivo de persistência
[O que precisa ser mantido e por quê.]

## 2. Estruturas lógicas

### DATA001 — [Nome]

**Objetivo:**  
[Descrição.]

| Campo/Dado | Significado | Obrigatório | Restrição funcional | Referência |
|---|---|---|---|---|

## 3. Relacionamentos
| Origem | Relação | Destino | Cardinalidade | Regra/Justificativa |
|---|---|---|---|---|

## 4. Integridade
| ID | Regra de integridade | Referência |
|---|---|---|

## 5. Unicidade
| Dado/Conjunto | Regra | Referência |
|---|---|---|

## 6. Histórico e auditoria
[Quando aplicável.]

## 7. Dados derivados ou transitórios
[Quando aplicável.]

## 8. LEGACY — correspondências conhecidas
[Somente quando necessário. Não representa obrigação de copiar o modelo físico.]

## 9. Pontos a validar
| ID | Questão | Impacto |
|---|---|---|

## 10. Rastreabilidade
| Estrutura/Regra | Requisito/Regra/Critério |
|---|---|

> Este documento MUST NOT definir SGBD, ORM, tipos físicos ou ferramenta de migração.
