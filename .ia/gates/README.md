---
name: gate-readme
type: gate
description: "Gates"
---

# Gates

Gates são **critérios objetivos de passagem entre etapas** do fluxo.

Um Gate não descreve procedimento e não cria regra: ele verifica um estado e
decide se a etapa pode avançar.

- regra de negócio/técnica estável → `rules/`
- procedimento especializado → `skills/`
- critério de passagem → `gates/`
- ordem das etapas e quando cada Gate é chamado → `workflows/`

---

## Contrato de um Gate

Cada arquivo de Gate define:

1. **ID** — identificador estável (`GATE-CTX`, `GATE-PLN`, ...);
2. **Momento** — quando é avaliado;
3. **Executor** — quem avalia;
4. **Critérios** — itens verificáveis;
5. **Evidência** — o que comprova cada item;
6. **Resultado** — `PASS`, `REQUIRES_REVIEW`, `FAIL` ou `NOT_APPLICABLE`;
7. **Falha** — o que acontece quando o critério não é atendido.

---

## Regras gerais

MUST NOT marcar um critério como atendido apenas porque o arquivo existe.

Arquivo vazio, placeholder, `UNKNOWN` crítico ou contradição MUST ser tratado
como pendência.

Um Gate de saída MUST ser avaliado antes de autorizar a próxima etapa.
Um Gate de entrada (pré-condição) MUST ser avaliado antes de iniciar a execução.

Item bloqueante não atendido:

`REQUIRES_REVIEW`

Gates de leitura/consulta pura (status, navegação, resposta informativa) são
`NOT_APPLICABLE`.

---

## Catálogo

| Gate | Arquivo | Momento | Etapa coberta |
|---|---|---|---|
| `GATE-CTX` | `contexto.md` | antes de produzir/alterar artefato | Contexto |
| `GATE-PLN` | `planejamento.md` | Discovery → Specification | Planejamento |
| `GATE-AUT` | `autorizacao.md` | Specification → Implementation | Autorização |
| `GATE-ANL` | `analise.md` | antes de criar/alterar código ou schema | Análise |
| `GATE-QLT` | `qualidade.md` | Implementation → Done | Validação |
| `GATE-CLS` | `encerramento.md` | antes de declarar concluído | Revisão/Finalização |

Gates não criados (decisão consciente, para evitar redundância):

- **Requisitos** — a completude de `business.md` é gate normativo de
  `corporate/requirements` e é revalidada em `GATE-AUT` (`business.md suficiente`).
- **Arquitetura** — decisões arquiteturais bloqueantes são verificadas em
  `GATE-PLN` e `GATE-AUT`; quando exigem registro, aplica-se
  `rules/architecture/adr.md`.
- **Revisão** — é uma intenção própria (`skills/review`) com resultado
  `PASS/REQUIRES_REVIEW/FAIL`; o fechamento é verificado por `GATE-CLS`.

---

## Registro do resultado

| Escopo | Onde registrar |
|---|---|
| Gate de uma Spec | `gates:` em `specs/<ID>-<slug>/status.yaml` |
| Gate de workflow/projeto | `gates:` em `state/current.yaml` |

Nunca registrar log completo ou conteúdo bruto: apenas o resultado e a evidência
mínima que o sustenta.

---

## Falha bloqueante

Quando um Gate não pode ser satisfeito:

1. registrar o item não atendido;
2. não avançar de etapa;
3. retornar `REQUIRES_REVIEW` quando depender de decisão humana;
4. retornar `FAIL` quando houver violação objetiva de Rule, Spec ou contrato.
