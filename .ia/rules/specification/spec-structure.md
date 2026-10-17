---
name: rule-specification-spec-structure
type: rule
description: "Estrutura Padrão de Spec"
---

# Estrutura Padrão de Spec

## Objetivo

Garantir uma estrutura previsível para todas as features, sem acoplar a Spec à tecnologia de implementação.

## Local

Toda Spec deve ser criada em:

`specs/<ID>-<slug>/`

Exemplo:

`specs/UC-010-manter-cargo/`

## Origem

Toda feature deve possuir origem:

- `NEW`
- `LEGACY`

## Estrutura padrão

```text
business.md
api.md
database.md
frontend.md
security.md
tests.md
status.yaml
context-manifest.yaml
```

Para `LEGACY`, quando aplicável:

```text
evidence.md
migration.md
```

## Arquivo único proibido

MUST NOT criar um único arquivo `spec.md` (ou qualquer outro arquivo
único) contendo vários artefatos da Spec.

Cada artefato é um arquivo próprio em `specs/<ID>-<slug>/`, mesmo
quando gerados na mesma execução. Modo `REQUIREMENTS` gera somente
`business.md`; modo `ARTIFACT` gera somente o artefato pedido; modo
`FULL_SPEC` gera um arquivo por artefato aplicável.

Ao concluir o modo pedido: atualizar `status.yaml` e
`context-manifest.yaml`, avaliar o gate aplicável e parar. MUST NOT
iniciar a etapa seguinte sem nova solicitação explícita.

## Responsabilidades

| Arquivo | Responsabilidade |
|---|---|
| `business.md` | Requirements |
| `api.md` | Contrato funcional de API |
| `database.md` | Modelo lógico e regras de persistência |
| `frontend.md` | UI/UX e comportamento da interface |
| `security.md` | Security |
| `tests.md` | Testing |
| `status.yaml` | Orquestração/status |
| `context-manifest.yaml` | Manifesto dos arquivos lidos e do motivo da leitura |
| `evidence.md` | Evidência de legado |
| `migration.md` | Migração |

## Tecnologia

Specs MUST NOT definir tecnologia de implementação.

MUST NOT incluir, por exemplo:

- linguagem;
- framework;
- biblioteca;
- annotation;
- ORM;
- banco específico;
- ferramenta de migração;
- ferramenta de testes;
- componente de UI específico;
- comando de build.

Exemplos:

Errado:

```text
Criar <componente de framework específico>.
Usar <banco de dados específico>.
Usar <biblioteca de UI específica>.
Testar com <ferramenta de testes específica>.
```

Correto:

```text
Expor operação para cadastro de cliente.
Persistir cliente com e-mail único.
Tela deve permitir salvar e cancelar.
Cenário deve validar cadastro com dados obrigatórios.
```

Tecnologia pertence a:

`config/technology.yaml`

e somente deve ser aplicada durante implementação.

## Corporate

Quando existir pacote corporativo para uma especialidade, ele define como produzir o arquivo correspondente.

Exemplo:

```text
corporate/requirements → business.md
corporate/ui-ux        → frontend.md
corporate/security     → security.md
corporate/testing      → tests.md
```

A estrutura da Spec permanece estável mesmo que o conteúdo interno seja definido por pacotes diferentes.

## NEW

MUST NOT exigir:

- evidência de legado;
- AS-IS;
- paridade;
- `migration.md`.

## LEGACY

Quando aplicável:

- `evidence.md` registra fatos comprovados;
- `migration.md` separa AS-IS, TO-BE e GAP.

Código legado é evidência funcional e não modelo técnico.

## Estados

Usar:

- `DEFINED`
- `NOT_APPLICABLE`
- `REQUIRES_REVIEW`
- `UNKNOWN`

`UNKNOWN` = deveria existir uma resposta, mas ainda não foi determinada.

`NOT_APPLICABLE` = o conceito não se aplica.

## Gate

Uma Spec pode ser aprovada somente quando:

- regras críticas estiverem definidas;
- critérios de aceite forem verificáveis;
- decisões bloqueantes não estiverem pendentes;
- arquivos aplicáveis forem coerentes entre si.

A aprovação da Spec **não** autoriza implementação. A autorização é
`GATE-AUT` (`gates/autorizacao.md`).

O resultado dos gates da Spec deve ser registrado em `status.yaml -> gates`.

## Donos dos artefatos

A estrutura é estável, mas cada artefato possui um responsável:

- `business.md` → `corporate/requirements`
- `api.md` → `corporate/api`
- `database.md` → `corporate/database`
- `frontend.md` → `corporate/ui-ux`
- `security.md` → `corporate/security`
- `tests.md` → `corporate/testing`
- `status.yaml` → `skills/specification`
- `evidence.md` / `migration.md` → `skills/migration`

O template deve ficar junto do responsável pelo artefato.
