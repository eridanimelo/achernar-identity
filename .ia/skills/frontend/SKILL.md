---
name: frontend
type: skill
description: >-
  Procedimento para implementar frontend conforme Spec aprovada: mapear padrões
  existentes, executar Reuse Scan, implementar telas, componentes e fluxos,
  consumir contratos de API e aplicar UI/UX, acessibilidade e testes. Usar em
  solicitações de implementação frontend, tela, componente, formulário, listagem,
  serviço de frontend ou teste de interface.
---

# Skill: Frontend

## Quando usar

Quando a solicitação for implementar, ajustar ou corrigir frontend de uma Spec aprovada.

## Quando não usar

Para backend, Discovery, requisito ou Spec funcional.

Esta Skill não substitui `agents/frontend/AGENT.md`: o Agent coordena e declara o
contexto; esta Skill define o procedimento.

## Entradas obrigatórias

- `config/technology.yaml`;
- caminho físico do frontend (`config/paths.yaml`, chave `frontend.root`);
- Spec alvo aprovada;
- Rules técnicas aplicáveis;
- políticas de UI/UX, segurança e testes aplicáveis.

Da Spec, usar como fonte primária: `business.md`, `frontend.md`, `api.md`,
`security.md`, `tests.md` e `status.yaml`.

MUST NOT usar `database.md` como dependência padrão do frontend.

## Processo

1. validar `GATE-AUT` (`gates/autorizacao.md`);
2. confirmar escopo e dependências da Spec;
3. localizar o frontend real;
4. mapear componentes, padrões e fluxos existentes;
5. **validar aderência à `rules/frontend/angular-architecture.md` (feature-based, separação smart/presentational, models por feature, templates separados, lazy loading, feature services);**
6. executar `GATE-ANL` delegando o Reuse Scan ao especialista `agents/reuse/frontend/AGENT.md` conforme `rules/reuse/reuse.md`;
7. planejar somente telas/componentes/serviços necessários respeitando `REUSE | COMPOSE | EXTEND | CREATE` retornado pelo scan;
8. implementar o fluxo definido em `frontend.md` **seguindo a arquitetura obrigatória**;
9. consumir somente contratos definidos em `api.md`;
10. aplicar UI/UX, acessibilidade e segurança durante a implementação;
11. implementar testes derivados de `tests.md`;
12. validar responsividade e estados relevantes;
13. executar verificações disponíveis e validar `GATE-QLT` (`gates/qualidade.md`) **incluindo critérios de arquitetura**;
14. revisar qualidade, segurança e aderência à Spec;
15. atualizar `status.yaml` e estado somente com evidência real.

## UI/UX

- aplicar `skills/ui-ux-design/SKILL.md` (com suas references sob demanda), conforme contrato do Agent (`agents/frontend/AGENT.md`);
- reutilizar design system e componentes existentes;
- preservar responsividade e acessibilidade;
- MUST NOT criar novo design system por preferência individual quando existir padrão aplicável.

## Arquitetura Obrigatória

**Toda implementação frontend DEVE seguir `rules/frontend/angular-architecture.md`.** Pontos não negociáveis:

- Feature-Based Architecture com pastas `pages/`, `components/`, `services/`, `models/` por feature
- Separação clara Smart (pages) / Presentational (components) components
- Models de domínio em `features/<feature>/models/` — **NUNCA** em `core/`
- Feature services em `features/<feature>/services/` — encapsulam HTTP e lógica de negócio
- Smart components injetam **apenas** feature services, **NUNCA** `ApiService` genérico
- Templates > 50 linhas em arquivo `.html` separado (`templateUrl`)
- Standalone components + lazy loading nas rotas (`loadChildren`)
- Signals (`signal`, `computed`, `input`, `output`) para estado e comunicação
- Validação automatizada via ESLint (`max-lines`, `template-inline`, `no-restricted-imports`)

MUST NOT violar estes princípios por preferência individual ou pressa. Código que não aderir não passa `GATE-ANL` nem `GATE-QLT`.

## Segurança

- seguir `security.md` da Spec;
- aplicar `corporate/security/RULES.md` quando o Agent determinar que é aplicável ao escopo (ver `agents/frontend/AGENT.md`);
- frontend MUST NOT substituir autorização de backend;
- tratar exposição de dados, armazenamento local, navegação e permissões conforme aplicável.

## Testes

- `tests.md` é entrada obrigatória da implementação;
- aplicar `corporate/testing/RULES.md` quando o Agent determinar que é aplicável ao escopo (ver `agents/frontend/AGENT.md`);
- implementar cenários frontend aplicáveis antes de concluir;
- registrar cenários não executáveis e o motivo.

## Assets de implementação

A tecnologia desta camada MUST ser descoberta em `config/technology.yaml`
(chave `frontend`); esta Skill MUST NOT presumir tecnologia.

Assets são opcionais:

- se `assets/` existir e contiver arquivos, seguir seu conteúdo como referência
  de estrutura, adaptando à Spec e ao padrão vigente do projeto;
- se não houver assets, seguir o padrão vigente do projeto.

Assets são referência de forma; a Spec é a fonte de comportamento.
MUST NOT criar artefato que a Spec não exige.

## Restrições

MUST NOT:

- inventar comportamento ou contrato de API;
- duplicar componente existente sem justificativa;
- implementar backend;
- iniciar outra UC automaticamente após concluir a atual.

Se faltar decisão crítica ou Gate válido: `REQUIRES_REVIEW`.

## Referências

- `rules/quality/quality.md`, `rules/reuse/reuse.md`
- `rules/context/context-economy.md`
- `rules/frontend/angular-architecture.md` — arquitetura obrigatória
