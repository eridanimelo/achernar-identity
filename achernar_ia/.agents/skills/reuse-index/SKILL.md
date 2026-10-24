---
name: reuse-index
type: skill
description: >-
  Gera e consulta um índice local, leve e determinístico (regex, sem LLM,
  sem rede) dos símbolos declarados no código-fonte de backend/frontend,
  para localizar candidatos a reuso sem precisar ler o código inteiro. Usar
  antes ou durante o Reuse Scan, ou sempre que precisar localizar rapidamente
  onde uma classe, service, component ou DTO equivalente pode existir.
---

# Skill: Reuse Index

## Quando usar

Como primeira camada do Reuse Scan (`GATE-ANL`) dos especialistas `subagents/reuse/backend` e `subagents/reuse/frontend`, ou sempre que for necessário
localizar rapidamente onde algo equivalente já existe no código.

## Quando não usar

Como substituto de abrir o arquivo real antes de decidir reutilizar. O
índice é um atalho de localização, não uma fonte de verdade sobre
comportamento, contrato ou dependências entre símbolos.

## Entradas

- `config/paths.yaml` → raízes de backend/frontend; só as raízes com stack
  declarada em `config/technology.yaml` são indexadas (docker, legacy e
  requirements ficam de fora — não são código-fonte);
- `.cache/reuse-index.md` → índice já existente, quando houver.

## Processo

1. Verificar se `.cache/reuse-index.md` existe e está em dia:
   `python3 .agents/skills/reuse-index/scripts/build_index.py --check`
   (exit 0 = em dia; exit 1 = ausente ou desatualizado);
2. Se ausente ou desatualizado, gerar/atualizar:
   `python3 .agents/skills/reuse-index/scripts/build_index.py`;
3. Buscar no índice por nome/palavra-chave do que está sendo procurado
   (classe, service, DTO, component);
4. Abrir e ler o arquivo real do(s) candidato(s) encontrados para confirmar
   assinatura e comportamento antes de decidir reutilizar — o índice é
   heurístico (regex, não parser real) e pode errar em casos ambíguos;
5. Buscar também por conceitos/sinônimos relevantes, não apenas pelo nome exato;
6. Se a busca no índice não trouxer nada plausível, complementar com
   busca por símbolo, grep/leitura seletiva do código-fonte antes de concluir que não existe
   equivalente.

## Saídas

- `.cache/reuse-index.md` atualizado;
- localização informada para a decisão do Reuse Scan
  (`rules/reuse/reuse.md`).

## Limitações

- extração por regex, não AST: pode perder assinaturas com formatação
  incomum, ou casar algo incorretamente em código muito atípico;
- não resolve relação entre símbolos (quem depende de quem) — para blast
  radius/impacto em consumidores, ainda é necessário grep/leitura manual;
- cobertura dirigida pela stack (`STACK_KEYWORDS` em `scripts/build_index.py`):
  `.java`/`.cs`/`.py`/`.ts`/`.tsx` via `LANG_RULES`, `.html` via extrator
  dedicado de fragmentos Thymeleaf (`SPECIAL_INDEXERS`); stacks sem regra
  (ex.: `h2`) geram aviso e são puladas — o banco não é indexado;
- novas tecnologias exigem mapear a stack em `STACK_KEYWORDS` e, se preciso,
  um bloco em `LANG_RULES`;
- não detecta staleness sozinho: rodar `--check` antes de confiar no índice.

## Custo

Sem LLM, sem rede, sem dependência externa (só Python 3 stdlib). O custo é
de tempo de execução local (milissegundos a poucos segundos, dependendo do
tamanho do repo), não de tokens.

## Referências

- `scripts/build_index.py`
