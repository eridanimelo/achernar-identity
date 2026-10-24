---
name: corporate-requirements-rules
type: rule
description: "Requirements Rules"
---

# Requirements Rules

## 1. Neutralidade tecnológica

`business.md` descreve comportamento de negócio.

MUST NOT citar:

- linguagem;
- framework;
- biblioteca;
- ORM;
- banco de dados específico;
- annotation;
- ferramenta de testes;
- componente técnico de frontend;
- classe, DTO, Repository, Service ou Controller da solução nova.

## 2. Não inventar requisitos

Toda regra deve ser suportada por:

- requisito fornecido;
- decisão humana;
- documento válido;
- regra corporativa aplicável;
- evidência do legado, quando `LEGACY`.

Quando houver interpretação provável sem confirmação, marcar como `INFERRED`.

Quando a decisão for necessária para prosseguir, marcar `REQUIRES_REVIEW`.

## 3. História de usuário

Quando apropriado, expressar a necessidade como:

- **Como** [ator/perfil]
- **Quero** [capacidade]
- **Para** [objetivo/valor]

MUST NOT inventar ator apenas para completar o formato.

Se a forma de história de usuário não for adequada ao caso, descrever o objetivo
funcional diretamente.

## 4. Critérios de aceite

Critérios devem ser:

- objetivos;
- verificáveis;
- relacionados ao comportamento;
- suficientes para determinar sucesso ou falha.

Preferir:

- **Dado**
- **Quando**
- **Então**

MUST NOT incluir detalhes de implementação.

## 5. Regras de negócio

Regras devem possuir identificador estável:

`RN001`, `RN002`, ...

Cada regra deve registrar:

- descrição;
- origem/referência;
- situação.

Situações:

- `CONFIRMED`
- `INFERRED`
- `REQUIRES_REVIEW`

## 6. Campos e dados funcionais

Quando a feature possuir entrada ou apresentação de dados, registrar:

- nome;
- significado;
- obrigatoriedade;
- formato lógico;
- restrições funcionais;
- regra relacionada.

MUST NOT transformar tipo funcional em tipo tecnológico.

Exemplo:

Correto:
`data de nascimento`

Incorreto:
`LocalDate`

## 7. Ações e fluxo

Quando houver interface ou processo, identificar:

- ações disponíveis;
- pré-condições;
- resultado esperado;
- estados funcionais relevantes;
- exceções conhecidas.

UI/UX detalhado pertence ao pacote `corporate/ui-ux`.

## 8. Rastreabilidade

Para `NEW`, referências podem ser:

- requisito;
- decisão;
- documento;
- Discovery;
- issue;
- regra corporativa.

Para `LEGACY`, evidências podem usar IDs:

`EVD001`, `EVD002`, ...

MUST NOT exigir EVD para feature `NEW`.

## 9. UNKNOWN e NOT_APPLICABLE

`UNKNOWN`:
a resposta deveria existir, mas ainda não foi determinada.

`NOT_APPLICABLE`:
o conceito não se aplica.

MUST NOT usar `UNKNOWN` apenas para preencher seção irrelevante.

## 10. Qualidade

Antes de concluir `business.md`, verificar:

- objetivo compreensível;
- atores conhecidos quando necessários;
- regras rastreáveis;
- critérios verificáveis;
- ambiguidades explicitadas;
- nenhuma tecnologia presente;
- nenhuma regra inventada;
- divergências registradas;
- pontos bloqueantes identificados.
