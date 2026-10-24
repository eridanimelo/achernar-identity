---
name: responsive
type: skill-reference
description: "Padrões responsivos mobile-first da skill ui-ux-design"
---

# Responsividade

Mobile-first sempre. Desenhar primeiro para 360-390px, depois expandir.

## Breakpoints (referência, ajustar ao projeto)

- `<=600px`: coluna única, navegação inferior ou hamburger + overlay, tabelas viram cards;
- `601-1024px`: 2 colunas, drawer permitido com overlay;
- `>1024px`: 12 colunas, densidade maior, comparação tabular liberada.

Regras duras: `meta viewport` presente, sem scroll horizontal indesejado, container fluido (`minmax`, `%`, `fr`), nunca largura fixa em px para container.

## Tabelas CRUD

Padrão: desktop tabular, mobile card vertical com `label + valor` preservados e ações acessíveis sem hover.

- Dimensionar coluna pelo conteúdo; coluna Ação sempre por último, largura mínima;
- secundárias podem ir para detalhe expansível;
- scroll horizontal só como exceção motivada (matriz analítica, cronograma, comparativo onde comparar colunas é a tarefa).

## Formulários

Largura reflete conteúdo, não espaço disponível:

- linha inteira: endereço, descrição, observação;
- médios: nome, e-mail, telefone;
- curtos: CEP, UF, número, data, status.

Mesma linha mantém topo alinhado. Empilha progressivamente para 1 coluna. Label sempre visível (nunca só placeholder), erro junto ao campo, validação inline em tempo real.

## Navegação

Topo com até ~5-7 itens; além disso agrupar. Sidebar fixa só se o produto exigir densidade operacional — nunca imposta por padrão global. Voltar previsível. Bottom-nav mobile <= 5 itens.
