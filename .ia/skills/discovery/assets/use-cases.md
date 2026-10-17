---
name: use-cases
type: skill-asset
description: "Discovery — Inventário Funcional"
---

# Discovery — Inventário Funcional

> MELHORIA: separa Feature de Caso de Uso para evitar Specs agregadoras do tipo `Manter <Entidade>` quando operações puderem ser independentes.

## Features identificadas

| ID | Feature | Objetivo | Módulo/Domínio | Origem/Evidência | Status |
|---|---|---|---|---|---|

### Status de Feature

`DISCOVERED | REVIEW | APPROVED | BLOCKED`

---

## Casos de uso

| ID | Feature | Caso de uso | Ator | Telas/Entradas | Dependências | Complexidade | Risco | Valor | Integrações | Reuso potencial | Evidência/Origem | Status |
|---|---|---|---|---|---|---|---|---|---|---|---|---|

## Convenções

### ID
Temporário durante Discovery, salvo padrão já definido pelo projeto.

### Complexidade
`LOW | MEDIUM | HIGH`

### Risco
`LOW | MEDIUM | HIGH`

### Valor
`LOW | MEDIUM | HIGH`

### Status
`DISCOVERED | REVIEW | APPROVED | BLOCKED | REQUIRES_REVIEW`

## Regras de decomposição

- Feature representa capacidade/agrupamento funcional.
- Caso de uso representa objetivo funcional independente e verificável.
- MUST NOT transformar automaticamente uma Feature inteira em um único UC `Manter <Entidade>`.
- SHOULD separar operações com fluxo, regra, permissão, critério de aceite, dependência, prioridade, implementação ou teste próprios.
- MUST NOT inventar CRUD completo.
- Operação não suportada por requisito/evidência MUST NOT ser marcada como confirmada.
- Operação plausível, mas não especificada, MAY ser registrada como `REQUIRES_REVIEW`.
- MUST NOT ordenar apenas pelo ID.
- MUST NOT inventar dependência sem evidência.
- Em `LEGACY`, registrar evidência do código/documentação quando aplicável.
- Em `NEW`, registrar requisito/decisão que originou o caso de uso.

## Exemplo

```text
FEATURE-001 — Gestão de Alunos
├── UC-010 — Cadastrar aluno
├── UC-011 — Listar alunos
├── UC-012 — Consultar aluno
└── UC-013 — Remover aluno
```

Se `Editar aluno` não estiver no requisito, não criar como confirmado. Registrar como `REQUIRES_REVIEW` somente se houver motivo para levantar a necessidade.
