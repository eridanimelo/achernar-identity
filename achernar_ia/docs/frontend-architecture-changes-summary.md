# Resumo das Alterações — Arquitetura Frontend Angular

**Data:** 2026-10-11  
**Projeto:** Achernar Identity  
**Stack:** Angular 22 + Tailwind CSS (standalone components)

---

## Arquivos Criados

| Arquivo | Descrição |
|---------|-----------|
| `rules/frontend/angular-architecture.md` | **Nova rule obrigatória** definindo arquitetura Feature-Based, separação smart/presentational, models por feature, feature services, templates separados, lazy loading, Signals, validação ESLint, critérios de gate |
| `docs/frontend-architecture-diagnostic.md` | Documento de diagnóstico com análise do estado atual, problemas identificados, proposta de alterações |

---

## Arquivos Modificados

| Arquivo | Alteração |
|---------|-----------|
| `subagents/frontend/AGENT.md` | Adicionado `rules/frontend/angular-architecture.md` em "Ler sempre" |
| `.agents/skills/frontend/SKILL.md` | - Processo: passo 5 valida aderência à arquitetura<br>- Processo: passo 8 implementa "seguindo a arquitetura obrigatória"<br>- Processo: passo 13 valida GATE-QLT "incluindo critérios de arquitetura"<br>- Nova seção "Arquitetura Obrigatória" com 8 pontos não negociáveis<br>- Referências atualizadas |
| `gates/analise.md` | Critério adicionado: validação de aderência à `rules/frontend/angular-architecture.md` para Frontend |
| `gates/qualidade.md` | Critério adicionado: validação de arquitetura frontend (models, services, smart/presentational, templates, lazy loading, ESLint) |

---

## O que a Nova Rule Exige (Resumo)

### Estrutura de Pastas Obrigatória
```
features/<feature>/
├── pages/         # Smart components (roteáveis)
├── components/    # Presentational components (dumb)
├── services/      # Feature services (HTTP + lógica)
├── models/        # Domain models (interfaces/types)
└── <feature>.routes.ts
```

### Separação Smart / Presentational
- **Smart (pages):** Injetam feature services, gerenciam estado, orquestram, navegam
- **Presentational (components):** Recebem `@Input`/`input()`, emitem `@Output`/`output()`, HTML + estilos puros

### Models de Domínio
- **NUNCA** em `core/api.service.ts` ou `core/`
- **SEMPRE** em `features/<feature>/models/`

### Feature Services
- Encapsulam `HttpClient` + lógica de negócio
- Injetados **apenas** em smart components
- `ApiService` genérico **proibido** em components (ESLint rule)

### Templates
- > 50 linhas → **arquivo `.html` separado** (`templateUrl`)
- ≤ 50 linhas → inline permitido (presentational simples)

### Lazy Loading
- `app.routes.ts` usa `loadChildren` para cada feature
- Features têm seu `<feature>.routes.ts`

### Signals Modernos
- `signal()`, `computed()`, `input()`, `output()`, `linkedSignal()`, `effect()`

### Validação Automatizada (ESLint)
- `max-lines: 300`
- `max-lines-per-function: 50`
- `@angular-eslint/template-inline: maxLines: 50`
- `@angular-eslint/prefer-standalone: error`
- `@angular-eslint/prefer-inject: error`
- `no-restricted-imports` bloqueia `ApiService` em components

---

## Critérios de Validação nos Gates

### GATE-ANL (Análise) — ANTES de criar/alterar código
- [ ] Aderência a `rules/frontend/angular-architecture.md` validada

### GATE-QLT (Qualidade) — ANTES de declarar concluído
- [ ] Models em `features/<feature>/models/`
- [ ] Feature services existem e são usados
- [ ] Smart/presentational separados
- [ ] Templates >50 linhas em `.html` separado
- [ ] Lazy loading configurado
- [ ] ESLint rules passando

---

## O que NÃO Muda (Criatividade Visual Preservada)

As regras **são apenas de engenharia de software**. A IA continua **livre para**:
- Design visual, cores, tipografia, espaçamento
- Animações, transições, microinterações
- Componentes visuais diferenciados
- Dark/light mode, responsividade
- Experiências de usuário inovadoras
- UI/UX de alta qualidade

---

## Próximos Passos (Fora desta Etapa)

1. **Configurar ESLint** no frontend com as rules propostas
2. **Refatorar código existente** gradualmente (features novas já seguem a rule)
3. **Criar feature services** para admin, auth, account, demo
4. **Mover models** de `core/api.service.ts` para `features/*/models/`
5. **Separar templates** inline > 50 linhas para arquivos `.html`
6. **Criar subpastas** `pages/`, `components/`, `services/` nas features

---

## Aprovação Necessária

> **Aguardando sua aprovação** para considerar estas alterações como base obrigatória para futuras implementações frontend no Achernar Identity.

A refatoração do código existente **NÃO** faz parte desta etapa — apenas a definição das regras, skills, agents e gates.