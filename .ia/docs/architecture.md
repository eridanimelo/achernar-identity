---
name: docs-architecture
type: docs
description: "Arquitetura do `.ia/`"
---

# Arquitetura do `.ia/`

Documento de **manutenção**. Não é carregado em execução; o roteador é
`AGENTS.md`, o primeiro arquivo deste diretório. Não existe cópia nem stub
na raiz do repositório de propósito — a sessão do agente MUST ser aberta
escopada em `.ia/`, nunca na raiz do projeto (ver `AGENTS.md`, seção "Por
que abrir a sessão escopada em `.ia/`").

Define o papel de cada camada e como elas se relacionam.

---

## Camadas e responsabilidades

| Camada | Responsabilidade | Não faz |
|---|---|---|
| `AGENTS.md` | orquestração: classificar intenção, rotear, precedência, parada | não contém procedimento, contrato de leitura nem critério de gate |
| `agents/<intenção>/AGENT.md` | identidade da intenção + **contrato de contexto** (`Ler sempre`, `Ler conforme`, `MUST NOT carregar`) + gates | não contém o procedimento |
| `agents/reuse/<camada>/AGENT.md` | especialista de Reuse Scan delegado por Backend/Frontend; investiga em contexto isolado e retorna decisão estruturada | não implementa a feature |
| `skills/<capacidade>/SKILL.md` | **procedimento** reutilizável, no formato Agent Skills, com `assets/` e `references/` | não define o contrato de contexto |
| `rules/<área>/*.md` | regras **obrigatórias e invariantes** (o que deve ser verdade) | não descreve passo a passo |
| `gates/<gate>.md` | **critérios objetivos** de passagem entre etapas | não cria regra nem procedimento |
| `workflows/<fluxo>.md` | **orquestração**: ordem das etapas, responsável e Gate de cada transição | não repete o procedimento da Skill |
| `corporate/<especialidade>/` | políticas organizacionais **read-only (exceção só por decisão humana explícita)** | não é editado localmente |
| `config/*.yaml` | decisões que **variam por projeto** — inclusive os caminhos do workspace (`paths.yaml -> workspace`) | não contém regra de negócio |

`.ia/` termina aqui. O que segue vive na **raiz do projeto**, fora de `.ia/` —
não é motor, é o resultado de rodar o motor neste projeto específico.
`specs`, `state`, `discovery`, `reviews` e `requirements` (documentos-fonte)
são **nomes simbólicos**, resolvidos só por `config/paths.yaml` — igual já
vale para `backend`/`frontend`. Nenhum arquivo do framework fora deste e de
`AGENTS.md` escreve o caminho físico deles:

| Camada (fora de `.ia/`) | Responsabilidade | Não faz |
|---|---|---|
| `requirements` (documentos-fonte) | documentos-fonte de requisitos fornecidos por humano | nunca é gerado pelo framework |
| `specs` | Specs geradas pelo fluxo (`<ID>-<slug>/*.md`) | não é reusável entre projetos |
| `state` | estado consolidado do workflow | não acumula histórico bruto |
| `discovery` | saída do Discovery | — |
| `reviews` | findings de revisão | — |
| `backend`, `frontend` | código do produto (`config/paths.yaml`) | não pertence ao motor |
| `.cache` | artefatos locais regeneráveis (ex.: `reuse-index.md`, `graph.html`) | não é versionado nem é fonte de verdade |

Hoje, fisicamente, `config/paths.yaml` aponta os cinco primeiros para dentro
de um único diretório na raiz do repositório, `requisitos/`
(`requisitos/specs/`, `requisitos/state/`, etc.) — mas isso é só o valor
atual da configuração, não uma convenção fixa do framework.

**Por que fora de `.ia/`:** `.ia/` é o motor — copiável para outro projeto
sem limpeza (exceto `config/*.yaml`, que é decisão por projeto e precisa
revisão manual). O que vive em `specs`/`state`/`discovery`/`reviews` cresce
a cada feature rodada *neste* projeto; se vivesse dentro de `.ia/`, copiar
o motor para um projeto novo exigiria apagar esse histórico manualmente
toda vez.

Regra de ouro: **uma informação vive em um único lugar**.

- Regra de negócio/técnica → `rules/`
- Como executar → `skills/`
- O que comprovar para avançar → `gates/`
- Quem faz o quê, em que ordem → `workflows/` + `agents/`
- O que ler antes de agir → `agents/`

---

## Contexto dinâmico

O custo de contexto é por iteração, não por arquivo.

- `AGENTS.md` permanece mínimo: entra em toda chamada;
- `agents/<intenção>/AGENT.md` entra só para a intenção ativa;
- `skills/`, `rules/`, `gates/`, `corporate/` e Specs entram sob demanda;
- arquivos grandes usam leitura seletiva e subagente isolado.

Regras: `rules/context/context-economy.md` e `rules/context/session-lifecycle.md`.

Reuse Scan especializado: `docs/reuse-scan.md`.

---

## Agent Skills

As Skills em `skills/` seguem a especificação oficial
(https://agentskills.io/specification):

```text
skills/<nome>/
├── SKILL.md            # obrigatório: frontmatter + instruções
├── assets/             # opcional: templates e recursos estáticos
├── references/         # opcional: documentação carregada sob demanda
└── scripts/            # opcional: código executável
```

```markdown
---
name: <igual ao nome do diretório>
description: <o que faz + quando usar, com palavras-chave de disparo>
---
```

Convenções obrigatórias:

- `name` em minúsculas, com hífens, sem hífen no início/fim e **igual ao diretório**;
- `description` descreve **o que** a Skill faz e **quando** usar;
- `SKILL.md` curto (referência: menos de 500 linhas / ~5.000 tokens);
- detalhe em `references/`; templates em `assets/`;
- referências relativas à raiz da Skill, uma camada de profundidade.

`corporate/**` é read-only por padrão (exceção só por decisão humana explícita).
Para `DATABASE`, `SECURITY`, `TESTING` e `JIRA`, o procedimento normativo está no pacote
corporativo e o Agent da intenção é o ponto de entrada. A conformidade com Agent
Skills é exigida apenas para `skills/`; o campo `type` descrito abaixo é convenção
local do motor e não faz parte da especificação oficial.

---

## Frontmatter do motor

Todo `.md` dentro de `.ia/` possui frontmatter com `name`, `type` e `description`
curta em português do Brasil (decisão humana — padronização do fluxo):

```markdown
---
name: <slug em minúsculas com hífens>
type: <orchestrator|agent|subagent|skill|skill-asset|skill-reference|rule|gate|workflow|template|docs>
description: "<curta>"
---
```

| `type` | Onde usa | Exemplo de `name` |
|---|---|---|
| `orchestrator` | `AGENTS.md` | `ia-orchestrator` |
| `agent` | `agents/<intenção>/AGENT.md` e `corporate/*/AGENT.md` | `agent-architecture`, `corporate-api-agent` |
| `subagent` | `agents/reuse/*/AGENT.md` | `subagent-backend` |
| `skill` | `skills/*/SKILL.md` e `corporate/*/SKILL.md` | `architecture` (igual ao diretório, pela spec), `corporate-api-skill` |
| `skill-asset` | `skills/*/assets/*.md` | `use-cases` |
| `skill-reference` | `skills/*/references/*.md` | `cli` |
| `rule` | `rules/**/*.md` e `corporate/*/RULES.md` | `rule-architecture-adr`, `corporate-api-rules` |
| `gate` | `gates/*.md` | `gate-contexto` |
| `workflow` | `workflows/*.md` | `workflow-feature` |
| `template` | `corporate/*/templates/*.md` | `corporate-api-template-api` |
| `docs` | `docs/*.md` e `corporate/**/README.md` | `docs-architecture`, `corporate-geral-readme` |

Regras:

- em `skills/*/SKILL.md`, `name` segue a spec oficial (igual ao diretório) e `type: skill` é apenas adição local;
- nos demais arquivos, `name` é derivado do path e `description` é o título `H1` existente;
- o tipo da camada continua codificado pelo path; o frontmatter só espelha para leitura rápida.

---

## Como estender

| Necessidade | Onde criar |
|---|---|
| Nova intenção no fluxo | `agents/<intenção>/AGENT.md` + rota em `AGENTS.md` |
| Novo procedimento reutilizável | `skills/<nome>/SKILL.md` |
| Nova restrição estável | `rules/<área>/<regra>.md` |
| Novo critério de passagem | `gates/<nome>.md` + catálogo em `gates/README.md` |
| Nova sequência de etapas | `workflows/<fluxo>.md` |
| Novo parâmetro por projeto | `config/<arquivo>.yaml` |

Ao adicionar uma Skill, siga `docs/adding-skills.md`.
