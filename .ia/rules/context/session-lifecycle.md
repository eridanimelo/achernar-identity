---
name: rule-context-session-lifecycle
type: rule
description: "Rule — Lifecycle de Sessão e Checkpoint de Estado"
---

# Rule — Lifecycle de Sessão e Checkpoint de Estado

## Objetivo

Definir quando continuar na sessão atual, quando iniciar contexto isolado e como persistir o estado necessário para continuidade.

Aplicada pelo orquestrador (`AGENTS.md`) e por Agents especializados ao concluir uma unidade de trabalho.

Relacionada: `rules/context/context-economy.md`.

---

## 1. Resolução de caminhos

Caminhos de requisitos, Specs, estado, Discovery, Reviews, Backend e Frontend MUST ser resolvidos por `config/paths.yaml`.

MUST NOT assumir caminhos físicos ou relativos fora dessa configuração.

Quando esta Rule mencionar:

* `current.yaml`;
* `spec-index.yaml`;
* `history.yaml`;

eles representam arquivos localizados sob `workspace.state`, conforme definido em `config/paths.yaml`.

---

## 2. Compatibilidade da sessão

Antes de iniciar uma nova unidade de trabalho, consultar `current.yaml` em `workspace.state` e comparar seu estado com a solicitação atual.

A sessão atual MAY continuar quando:

* a intenção principal permanecer a mesma;
* o escopo permanecer na mesma Spec ou em lote pequeno relacionado;
* não houver mudança de fase do workflow.

Considerar mudança de contexto quando houver:

* mudança de fase;
* mudança da intenção principal;
* troca para lote grande ou não relacionado;
* nova unidade que exija especialidades substancialmente diferentes.

Intenções principais incluem:

`DISCOVERY`, `REQUIREMENTS`, `SPECIFICATION`, `ARCHITECTURE`, `BACKEND`,
`FRONTEND`, `DATABASE`, `SECURITY`, `TESTING`, `MIGRATION`, `JIRA` e `REVIEW`.

---

## 3. Mudança de contexto

Quando houver mudança de contexto:

1. consolidar o resultado da unidade atual;
2. atualizar `current.yaml`;
3. atualizar `spec-index.yaml` quando aplicável;
4. registrar conclusão em `history.yaml`;
5. MUST NOT depender do histórico bruto da conversa para continuidade.

Todos os arquivos de estado MUST ser resolvidos a partir de `workspace.state`.

Se o ambiente permitir criação de nova sessão isolada, SHOULD utilizá-la.

Caso contrário:

* retornar `NEW_SESSION_REQUIRED`;
* informar `next_intent`;
* informar `next_scope`;
* ENCERRAR a execução imediatamente.

Após `NEW_SESSION_REQUIRED`:

* MUST NOT carregar arquivos da próxima intenção;
* MUST NOT carregar Agents, Skills, Rules ou templates da próxima intenção;
* MUST NOT executar ferramentas para a próxima etapa.

MUST NOT usar fork como mecanismo de isolamento quando ele herdar o histórico anterior.

---

## 4. Estado persistido

Sob `workspace.state`:

```text
current.yaml
→ estado corrente + próximo passo

spec-index.yaml
→ índice rápido de status e pendências das Specs

history.yaml
→ histórico append-only de unidades concluídas
```

`history.yaml` MUST permanecer fora do contexto inicial e ser consultado somente quando a tarefa exigir histórico, auditoria, revisão ou retomada.

Ao atualizar estado:

* `current.yaml` MUST conter somente o estado corrente e o próximo passo;
* conclusões MUST ser registradas em `history.yaml`;
* mudanças de estado de Specs MUST atualizar `spec-index.yaml`;
* somente estado consolidado MUST ser persistido.

MUST NOT persistir:

* logs completos;
* tool calls;
* transcrições;
* conteúdo bruto de arquivos;
* histórico acumulado em `current.yaml`.

Preferir estado estruturado a reconstruir contexto pelo histórico da conversa.

---

## 5. Checkpoint e decisão de continuidade

Ao concluir uma etapa ou lote relevante:

1. persistir somente o necessário para continuidade;
2. atualizar os arquivos de estado aplicáveis;
3. decidir entre continuar, compactar ou iniciar nova sessão;
4. MUST NOT iniciar automaticamente uma etapa incompatível.

Política:

```text
mesma intenção + mesmo escopo pequeno
→ continuar

mesma intenção + novo lote grande
→ checkpoint + nova sessão preferencial

mudança de intenção
→ checkpoint + nova sessão

mudança de fase
→ checkpoint + nova sessão

mesma tarefa longa
→ compactação quando suportada
```

Compactação MUST NOT ser considerada equivalente a isolamento de contexto.

---

## 6. Princípio

```text
conteúdo bruto
    ↓
extrair decisão / estado
    ↓
persistir em workspace.state
    ↓
descartar conteúdo bruto desnecessário
    ↓
continuar ou iniciar nova sessão
```

Correção, segurança e continuidade confiável têm prioridade sobre economia de contexto.
