---
name: graph-view
type: skill
description: >-
  Gera e atualiza uma visualização interativa (HTML auto-contido, D3) do
  grafo de referências reais entre AGENTS.md, .ia/ (motor: agents/, skills/,
  corporate/, rules/, gates/, config/) e os nomes simbólicos `specs`,
  `state`, `discovery`, `reviews` e `requirements` (workspace do projeto,
  resolvidos por config/paths.yaml) — local, sem LLM, sem rede na extração
  (só o navegador precisa de internet pra carregar D3/fontes ao abrir o
  arquivo). Usar quando o pedido for visualizar, mapear, entender a
  estrutura de dependências do framework, ver o que referencia o quê,
  achar pacote órfão/desconectado, ou revisar arquitetura antes de estender
  o framework com novo Agent/Skill/Rule/Gate.
---

# Skill: Graph View

## Quando usar

Antes de estender o framework (novo Agent, Skill, Rule ou Gate), pra ver
rapidamente o que já existe e como se conecta. Ou sob pedido explícito de
visualizar/mapear a estrutura do projeto.

## Quando não usar

Não é fonte de verdade sobre comportamento — é um mapa de referências
textuais. Pra saber o que um Agent realmente faz, ler o próprio `AGENT.md`.

## Entradas

Nenhuma manual. O script lê `config/paths.yaml` (`workspace.*` e
`requirements.root`) pra descobrir onde `specs`, `state`, `discovery`,
`reviews` e os documentos-fonte de requisitos físicamente estão hoje —
esses nomes são símbolos em todo o resto do framework, nunca paths fixos.
Se `paths.yaml` não tiver essas chaves ainda, usa a convenção atual
(`requisitos/specs`, etc.) como fallback de partida.

A raiz do projeto é localizada a partir da própria posição do script
(`.ia/skills/graph-view/scripts/build_graph.py` → raiz do `.ia/` é três
níveis acima; raiz do projeto é mais um nível acima disso).

## Processo

1. Verificar se `.cache/graph.html` existe e está em dia:
   `python3 .ia/skills/graph-view/scripts/build_graph.py --check`
   (exit 0 = em dia; exit 1 = ausente ou desatualizado — inclusive se
   `config/paths.yaml` mudou depois da última geração);
2. Se ausente ou desatualizado, gerar/atualizar:
   `python3 .ia/skills/graph-view/scripts/build_graph.py`;
3. Abrir `.cache/graph.html` num navegador (arquivo local, `file://`
   funciona; D3 e a fonte carregam via CDN/Google Fonts — precisa de
   internet só nesse passo de visualização, não na geração).

## O que vira aresta

- path entre crases que resolve pra um arquivo real, tentando nesta ordem:
  relativo a `.ia/` (convenção da maioria das referências internas do
  motor), tradução de nome simbólico (`specs/`, `state/`, `discovery/`,
  `reviews/`) pro path físico lido de `config/paths.yaml`, relativo à raiz
  do projeto sem tradução, e relativo à pasta de quem referencia (assets
  locais de uma Skill);
- padrão de pacote inteiro `` `corporate/x/**` `` → aresta pra todo arquivo
  real sob esse prefixo;
- menção a script `.py` em qualquer lugar do texto;
- referência templated de Spec (`` `specs/<ID>-<slug>/business.md` ``)
  → fica pendente até existir uma Spec real (procurada dentro do path
  físico de `specs`) cujo arquivo final bata o nome (`business.md`,
  `api.md`, ...); quando existir, a aresta aparece sozinha na próxima
  geração, sem precisar tocar neste script;
- roteamento semântico do orquestrador: `.ia/AGENTS.md` → os 12
  `.ia/agents/<intenção>/AGENT.md`, lidos da própria lista de intenções em
  `AGENTS.md` (não hardcoded, exceto como fallback se o texto mudar de
  formato);
- **dependência entre Specs**: lida da tabela "Origem | Depende de | ..."
  em `discovery/dependencies.md` (preenchida pelo Discovery). Cada par
  vira uma aresta com seta, estilo visual próprio (laranja, mais grossa),
  do `business.md` da Spec de origem pro `business.md` da Spec da qual ela
  depende — distinta de uma referência comum, porque é dependência
  funcional entre Specs, não menção em texto.

## Limitações

- extração por regex, não parser real: referência solta em texto corrido
  (sem crase) não é capturada;
- o parser de `config/paths.yaml` é mínimo (chave: valor, duas camadas de
  indentação) — não suporta YAML completo (listas, strings multilinha);
  se `paths.yaml` crescer em complexidade, este parser pode precisar
  acompanhar;
- é um mapa de referência textual, não de execução — não mostra ordem
  nem condição de carregamento (isso está no `AGENT.md` de cada intenção,
  não no grafo);
- não varre `backend/`/`frontend/` — não é essa a finalidade (código do
  produto, não a estrutura do framework);
- não detecta staleness sozinho fora do `--check`: se você abrir
  `.cache/graph.html` direto sem rodar `--check` antes, pode estar olhando
  uma foto antiga.

## Saídas

- `.cache/graph.html` (auto-contido, não versionado — mesma política de
  `.cache/reuse-index.md`).

## Referências

- `scripts/build_graph.py`
- `assets/template.html`
