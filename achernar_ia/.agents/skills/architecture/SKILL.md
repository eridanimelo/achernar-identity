---
name: architecture
type: skill
description: >-
  Tomar decisões arquiteturais somente quando necessárias, preferindo consistência
  com a solução existente, e registrar ADR quando a decisão for relevante. Usar
  em solicitações de arquitetura, definição de camadas, integração estrutural,
  decisão de padrão ou alteração significativa de persistência.
---

# Skill: Architecture

## Quando usar

Quando houver decisão arquitetural real a tomar para a feature ou projeto.

## Quando não usar

Para decisão trivial de implementação ou para implementar código.

## Entradas

- Spec aplicável;
- `config/technology.yaml`;
- `rules/architecture/*`;
- código existente do sistema novo, quando necessário.

## Processo

1. identificar a decisão necessária e seu motivo;
2. verificar se já existe padrão arquitetural no sistema;
3. preferir consistência com a solução atual;
4. avaliar alternativas apenas quando houver decisão real;
5. validar `GATE-ANL` (`gates/analise.md`) quando a decisão alterar estrutura existente;
6. aplicar a decisão;
7. registrar ADR quando aplicável (`rules/architecture/adr.md`);
8. retornar `REQUIRES_REVIEW` quando a decisão exigir aprovação humana.

## ADR

Registrar quando houver decisão relevante e duradoura; MUST NOT registrar ADR
para decisão trivial (`rules/architecture/adr.md`).

## Regras

- MUST NOT criar arquitetura nova por preferência;
- MUST NOT copiar arquitetura do legado automaticamente;
- MUST respeitar separação de responsabilidades;
- MUST evitar acoplamento indevido entre domínios;
- MUST evitar abstrações especulativas.

## Saída

- decisão aplicada;
- ADR quando necessário;
- `REQUIRES_REVIEW` quando houver decisão humana bloqueante.

## Referências

- `rules/architecture/principles.md`, `layers.md`, `adr.md`
- `rules/reuse/reuse.md`
