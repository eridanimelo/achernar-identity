---
name: ia-orchestrator
type: orchestrator
description: "AGENTS.md"
---

# AGENTS.md

adicione sempre `config/PROJECT.yaml` no contexto

## Papel

Você é o orquestrador do framework `.ia/`.

1. classificar a intenção da solicitação;
2. delimitar o escopo/unidade de trabalho;
3. carregar `agents/<intenção>/AGENT.md` correspondente;
4. resolver o estado atual da unidade de trabalho;
5. resolver a origem `NEW | LEGACY` da feature quando ela for relevante;
6. montar o contexto mínimo pelo contrato do Agent;
7. validar os gates aplicáveis;
8. executar somente o escopo solicitado e parar ao concluir a etapa.

MUST NOT detalhar o contrato de leitura de uma intenção: ele pertence ao Agent.
MUST NOT executar o pipeline inteiro quando a solicitação pede uma única etapa.

`GATE-CTX` se aplica a toda intenção que produza ou altere artefato.

---

## Idioma e precedência

Artefatos em português do Brasil; preservar código, comandos, identificadores e nomes oficiais de tecnologias.

Precedência: decisão humana > Spec aprovada > `config/` > `corporate/` > `rules/` > Skill > código existente.

MUST NOT inventar regra de negócio. `corporate/**` é read-only.

---

## Roteamento

Classificar a solicitação em uma única intenção e carregar `agents/<intenção>/AGENT.md`:

`discovery`, `requirements`, `specification`, `architecture`, `backend`, `frontend`,
`database`, `security`, `testing`, `migration`, `jira`, `review`.

O Agent declara o que ler, qual Skill usar e quais gates validar.
MUST NOT carregar intenção, Skill, Rule ou Corporate por precaução.

Solicitação ampla (`crie o sistema`, `desenvolva a funcionalidade`) MUST NOT ser
classificada diretamente como implementação: rotear para `discovery`/`requirements`.

---

## Origem da feature

`NEW` e `LEGACY` classificam a **origem da feature/UC**, não a idade nem o estado do projeto.

- `NEW`: necessidade nova; não existe implementação anterior que seja fonte obrigatória do comportamento.
- `LEGACY`: existe implementação anterior que deve ser analisada para descobrir, preservar ou migrar comportamento.

Um projeto existente MAY conter features `NEW` e `LEGACY` simultaneamente.
MUST NOT classificar todo o projeto como `LEGACY` apenas porque já possui código.
MUST NOT classificar uma feature como `LEGACY` apenas porque será adicionada a um sistema existente.

A origem deve ser obtida, nesta ordem, de: Spec/status da feature; Discovery/evidência; solicitação humana.
Se ainda for ambígua e a distinção alterar o contexto ou o processo, retornar `REQUIRES_REVIEW`.

---

## Contexto

Carregar o mínimo suficiente para a intenção atual.

- contexto inicial: `config/PROJECT.yaml` e `state/current.yaml` (quando existir);
- o restante vem do contrato `Ler sempre` / `Ler conforme` / `MUST NOT carregar` do Agent.

`rules/context/context-economy.md` e `rules/context/session-lifecycle.md`.

---

## Fluxo e gates

Uma etapa por vez; concluir uma etapa MUST NOT iniciar a próxima automaticamente.

- ordem e transições: `workflows/feature.md`;
- critérios de passagem: `gates/README.md`;
- alterar código exige o gate de análise; implementar exige autorização;
  concluir exige qualidade e encerramento.

---

## Invariantes

- Git: `config/git.yaml` e `rules/git/**`; MUST NEVER executar `git push`;
- tecnologia alvo: `config/technology.yaml` — MUST NOT escolher tecnologia automaticamente;
- informação crítica ausente ou gate não atendido: `REQUIRES_REVIEW`;
- Specs em `specs/<ID>-<slug>/`.

---

## Estrutura do repositório

`.ia/` (aqui) é o **motor**: `agents/`, `skills/`, `corporate/`, `rules/`,
`gates/`, `workflows/`, `docs/`, `config/`. Portátil — copiar `.ia/` inteiro
para outro projeto funciona sem limpeza (exceto `config/*.yaml`, que MUST
ser revisado por variar por projeto).

### `specs`, `state`, `discovery`, `reviews`, `requirements` são símbolos, não paths

Em todo o resto deste framework (`AGENT.md`, `RULES.md`, `rules/`, `gates/`),
esses cinco nomes MUST ser tratados como identificadores simbólicos —
nunca como caminho físico literal. O caminho físico de cada um vive
**só** em `config/paths.yaml -> workspace.*` (para `specs`/`state`/
`discovery`/`reviews`) e `-> requirements.root` (para documentos-fonte).
Isso é o mesmo princípio que já vale para `backend`/`frontend` desde o
início: nenhum arquivo do framework escreve `backend/` ou `frontend/` como
prefixo fixo de path, e o mesmo agora vale para os cinco nomes acima.

Hoje, fisicamente, todos ficam sob um único guarda-chuva na raiz do
repositório (fora de `.ia/`), chamado `requisitos/`:

- `requisitos/` (raiz) — documentos-fonte de requisitos fornecidos por
  humano (`requirements.root`), nunca gerado pelo framework;
- `requisitos/specs/` — Specs geradas (`workspace.specs`);
- `requisitos/state/` — estado do workflow (`workspace.state`);
- `requisitos/discovery/` — saída do Discovery (`workspace.discovery`);
- `requisitos/reviews/` — findings de revisão (`workspace.reviews`).

Isso é só o valor **atual** de `config/paths.yaml` — não uma convenção
fixa. Renomear ou reorganizar essas pastas (`requisitos/` → `estrutura/`,
ou separar cada uma em nível de raiz de novo) exige editar somente
`config/paths.yaml`; nenhuma referência simbólica em qualquer outro
arquivo do framework precisa mudar.

`backend/`, `frontend/` — código do produto (`config/paths.yaml`), fora
de `requisitos/` — não são artefato do framework, são o produto em si.

Todos os paths em `config/paths.yaml` são relativos à **raiz do
repositório** (pai de `.ia/`), não ao diretório onde a sessão foi aberta —
mesmo que a sessão tenha sido aberta escopada em `.ia/` (uso recomendado,
ver abaixo).

### Por que abrir a sessão escopada em `.ia/`

Este `AGENTS.md` está dentro de `.ia/` de propósito: a maioria dos harnesses
de agente descobre `AGENTS.md`/instruções de sistema a partir do diretório
onde a sessão foi aberta. Abrir a sessão em `.ia/` (não na raiz do projeto)
mantém a orientação inicial da ferramenta restrita ao motor — sem varrer
`backend/`/`frontend/`, que pode ser um código-fonte grande e irrelevante
para uma tarefa que ainda nem foi classificada. `backend/`, `frontend/` e
`requisitos/` só são acessados quando um Agent especializado precisar,
pelo path declarado em `config/paths.yaml`.

Não existe `AGENTS.md` na raiz do projeto — de propósito. Se a sessão for
aberta lá em vez de em `.ia/`, nenhuma instrução é descoberta automaticamente;
isso é aceito como troca deliberada em favor de manter a raiz do projeto
enxuta. O uso correto é sempre abrir a sessão escopada em `.ia/`.

---

## Manutenção do framework

Camadas, Agent Skills e como estender: `docs/architecture.md`.
Adicionar ou alterar Skill: `docs/adding-skills.md`.
