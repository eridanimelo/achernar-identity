---
name: review
type: skill
description: >-
  Revisar uma feature ou artefato contra a Spec e as regras aplicáveis, registrando
  findings com evidência e classificando o resultado em PASS, REQUIRES_REVIEW ou
  FAIL. Usar em solicitações de revisão, auditoria de implementação, verificação
  de aderência à Spec ou validação final antes de concluir.
---

# Skill: Review

## Quando usar

Quando for necessário revisar um artefato, uma Spec ou uma implementação.

## Quando não usar

Para implementar correções, criar Spec ou executar a implementação.

## Entradas

- escopo da revisão (artefato, Spec ou implementação);
- `business.md`, `status.yaml` e os artefatos do escopo revisado;
- arquivos de código efetivamente alterados, quando houver implementação;
- evidências de teste disponíveis.

## Processo

1. identificar o escopo da revisão;
2. carregar somente os arquivos relacionados;
3. verificar aderência funcional à Spec;
4. verificar arquitetura e reuso quando aplicável;
5. quando houver frontend Angular, verificar `rules/frontend/angular-architecture.md`: models em `features/<feature>/models/`, feature services usados nas pages, sem `ApiService`/`HttpClient` injetado em componente, templates > 50 linhas em `.html` separado, lazy loading (`loadChildren` preferido, `loadComponent` aceito), sem pastas vazias;
5. quando houver backend Java/Spring, verificar `rules/backend/java-backend.md`: DTOs sem entidade no contrato, LAZY explícito sem bidirecionalidade, `@Query` declarada e parametrizada, `NOT_SUPPORTED` em consultas sem transação (sem `readOnly = true` como substituto), conversão manual, exceptions específicas com tratamento global, Liquibase + `validate`;
6. acionar os pacotes corporativos aplicáveis;
7. verificar testes e evidências disponíveis;
8. registrar findings com evidência;
9. classificar o resultado;
10. validar `GATE-CLS` (`gates/encerramento.md`).

## Resultado

- `PASS` — atende aos critérios aplicáveis;
- `REQUIRES_REVIEW` — existe dúvida ou decisão humana necessária;
- `FAIL` — violação objetiva de Spec, Rule, contrato ou Gate.

## Regras

MUST NOT:

- inventar evidência;
- alterar a Spec para fazer a implementação parecer correta;
- marcar algo como validado sem verificar;
- aprovar com pendência bloqueante aberta;
- implementar correções sem nova intenção explícita;
- duplicar as regras de Requirements, UI/UX, Security ou Testing — a revisão
  coordena resultados especializados.

A revisão é do escopo solicitado: backend revisado não exige frontend concluído
salvo dependência explícita.

## Saída

- findings e evidências em `reviews/`.

## Referências

- `rules/quality/quality.md`
- `rules/backend/java-backend.md` (quando a revisão cobrir backend Java/Spring)
- `gates/qualidade.md`
