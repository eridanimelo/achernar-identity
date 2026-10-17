---
name: requirements
type: skill
description: "Requirements Skill"
---

# Requirements Skill

## Objetivo

Criar ou revisar `business.md` a partir das fontes disponíveis, seguindo
`AGENT.md` e `RULES.md`.

## Entradas

Carregar somente quando necessário:

- contexto da solicitação;
- `discovery/use-cases.md`;
- `discovery/dependencies.md`;
- documentos/requisitos fornecidos;
- decisões humanas;
- para `LEGACY`, evidências relevantes do sistema existente;
- `rules/specification/spec-structure.md`.

MUST NOT carregar `config/technology.yaml` para escrever `business.md`.

## Processo

1. identificar a feature;
2. identificar origem `NEW` ou `LEGACY`;
3. compreender objetivo e valor;
4. identificar atores/perfis quando comprovados;
5. identificar fluxo funcional;
6. extrair regras de negócio;
7. identificar campos e ações funcionais;
8. definir critérios de aceite;
9. registrar rastreabilidade;
10. registrar inferências, divergências e pontos a validar;
11. para `LEGACY`, separar AS-IS e TO-BE;
12. gerar ou revisar `business.md`;
13. executar verificação de qualidade.

## Saída

Gerar:

`specs/<ID>-<slug>/business.md`

usando obrigatoriamente:

`templates/business.md`

O template define a estrutura da saída; `AGENT.md` e `RULES.md` definem como analisar e preencher o conteúdo.

## Gate

Retornar `REQUIRES_REVIEW` quando:

- regra crítica estiver indefinida;
- houver divergência que altere comportamento;
- ator/permissão necessária não puder ser determinada;
- critério de aceite essencial não puder ser definido;
- evidência de legado for insuficiente para uma decisão crítica.

MUST NOT aprovar `business.md` enquanto houver bloqueio funcional.
