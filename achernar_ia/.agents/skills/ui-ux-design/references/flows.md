---
name: flows
type: skill-reference
description: "Mapeamento de dependência entre tarefas e escolha do padrão de fluxo"
---

# Fluxos e dependência entre tarefas

## Mapear antes de desenhar

1. Listar tarefas e marcar `independente | pré-requisito | bloqueante`;
2. marcar estados de cada etapa: inicial, carregando, vazio, erro, sucesso, desabilitado;
3. identificar frequência: caminho feliz diário vs. exceção rara;
4. só então escolher o contêiner.

## Regra de escolha

| Dependência | Padrão | Evitar |
|---|---|---|
| Grupos relacionados sem ordem | Tabs | Stepper |
| Etapas ordenadas com pré-requisito, progresso ou validação antes de avançar | Stepper / Wizard | Tabs ou seção longa única |
| Muitas opções com busca | Autocomplete | Select longo |
| Poucas opções conhecidas | Select / Radio / Segmented | Autocomplete |
| Ação curta, confirmação, edição pequena | Dialog | Página nova |
| Detalhe mantendo contexto da lista | Drawer / Side panel | Dialog para fluxo longo |
| Conteúdo secundário expansível | Accordion | Esconder informação essencial |
| Comparação tabular relevante | Table | Cards quando comparação não importa |
| Entidade agrupada ou fallback mobile de tabela | Card | Tabela sem comportamento mobile |

Regras: preferir o mais simples que resolve; nunca aninhar modal em modal; confirmação destrutiva com fricção proporcional à irreversibilidade; funcionalidade complementar subordinada (seção secundária, aba dedicada ou ação secundária -> modal), nunca com mesmo peso da ação principal.
