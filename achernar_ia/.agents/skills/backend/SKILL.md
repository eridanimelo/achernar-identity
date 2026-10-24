---
name: backend
type: skill
description: >-
  Procedimento para implementar backend conforme Spec aprovada: localizar o
  backend, executar Reuse Scan, respeitar camadas, aplicar contratos de API,
  persistência, segurança e testes. Usar em solicitações de implementação backend,
  criação de endpoint, service, repository, entidade, DTO, migration ou teste de
  backend.
---

# Skill: Backend

## Quando usar

Quando a solicitação for implementar, ajustar ou corrigir backend de uma Spec aprovada.

## Quando não usar

Para frontend, Discovery, requisito ou Spec funcional.

Esta Skill não substitui `subagents/backend/AGENT.md`: o Agent coordena e declara o
contexto; esta Skill define o procedimento.

## Entradas obrigatórias

- `config/technology.yaml`;
- caminho físico do backend (`config/paths.yaml`, chave `backend.root`);
- Spec alvo aprovada;
- Rules técnicas aplicáveis;
- políticas corporativas de segurança e testes aplicáveis.

Da Spec, usar como fonte primária: `business.md`, `api.md` (quando houver API),
`database.md` (quando houver persistência), `security.md`, `tests.md` e `status.yaml`.

Para `LEGACY`, consultar `migration.md` e `evidence.md` somente quando necessário.

## Processo

1. validar `GATE-AUT` (`gates/autorizacao.md`);
2. confirmar escopo e dependências da Spec;
3. localizar o backend real;
4. executar `GATE-ANL` delegando o Reuse Scan ao especialista `subagents/reuse/backend/AGENT.md` conforme `rules/reuse/reuse.md`;
5. planejar somente os artefatos necessários respeitando `REUSE | COMPOSE | EXTEND | CREATE` retornado pelo scan;
6. implementar regras e contratos da Spec;
7. respeitar camadas e dependências (`rules/architecture/layers.md`);
8. aplicar segurança desde a implementação, não como etapa posterior;
9. implementar testes derivados de `tests.md`;
10. executar verificações disponíveis e validar `GATE-QLT` (`gates/qualidade.md`);
11. revisar qualidade, segurança e aderência à Spec;
12. atualizar `status.yaml` e estado somente com evidência real.

## Dependências de domínio

Um Service acessa diretamente apenas o Repository do próprio domínio/agregado.
Para outro domínio, usar o Service responsável por aquele domínio
(`rules/architecture/layers.md`).

## Persistência

- seguir `database.md`;
- manter integridade;
- evitar alterações de schema não especificadas;
- aplicar a ferramenta configurada quando houver mudança de schema.

## API

- seguir `api.md`;
- validar entradas;
- não expor detalhes internos desnecessários;
- aplicar autenticação/autorização definidas;
- MUST NOT inventar contrato ausente.

## Segurança

- seguir `security.md` da Spec;
- aplicar `corporate/security/RULES.md` quando o Agent determinar que é aplicável ao escopo (ver `subagents/backend/AGENT.md`);
- tratar autenticação, autorização, validação, exposição de dados, secrets e logging conforme aplicável;
- MUST NOT adiar segurança para uma revisão opcional ao final.

## Testes

- `tests.md` é entrada obrigatória da implementação;
- aplicar `corporate/testing/RULES.md` quando o Agent determinar que é aplicável ao escopo (ver `subagents/backend/AGENT.md`);
- implementar cenários backend aplicáveis antes de concluir;
- registrar cenários não executáveis e o motivo.

## Assets de implementação

A tecnologia desta camada MUST ser descoberta em `config/technology.yaml`
(chave `backend`); esta Skill MUST NOT presumir tecnologia.

Quando o backend for Java/Spring, aplicar a fonte principal
`rules/backend/java-backend.md` e usar `assets/java-backend.md` como
referência de forma (estrutura Entity/DTO/Service/ServiceImpl/Repository,
conversão manual, `@Query` explícita, `NOT_SUPPORTED` em consultas sem
transação, exceptions específicas, Liquibase + `validate`).

Assets são opcionais:

- se `assets/` existir e contiver arquivos, seguir seu conteúdo como referência
  de estrutura, adaptando à Spec e ao padrão vigente do projeto;
- se não houver assets, seguir o padrão vigente do projeto.

Assets são referência de forma; a Spec é a fonte de comportamento.
MUST NOT criar artefato que a Spec não exige.

## Restrições

MUST NOT:

- inventar endpoint, persistência ou regra de negócio;
- ignorar `security.md`, `tests.md` ou Corporate Security aplicável;
- implementar frontend;
- iniciar outra UC automaticamente após concluir a atual.

Se faltar decisão crítica ou Gate válido: `REQUIRES_REVIEW`.

## Referências

- `rules/api/api.md`, `rules/database/database.md`
- `rules/backend/java-backend.md` + `assets/java-backend.md` (quando backend Java/Spring)
- `rules/quality/quality.md`, `rules/reuse/reuse.md`
- `rules/context/context-economy.md`
