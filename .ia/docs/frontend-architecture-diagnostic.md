# Diagnóstico de Arquitetura Frontend — Achernar Identity

**Data:** 2026-10-11  
**Versão Angular:** 22 (standalone components)  
**Estilo:** Tailwind CSS

---

## 1. Estado Atual

### Estrutura de Pastas Atual
```
frontend/src/app/
├── core/                    ✓ Serviços globais, guards, interceptors
│   ├── api.service.ts       ✗ Models de domínio misturados (Usuario, Cliente, Sessao, etc.)
│   ├── auth.interceptor.ts
│   ├── errors.ts
│   ├── guards.ts
│   ├── i18n.ts
│   ├── oidc.ts
│   ├── pkce.ts
│   ├── session.service.ts
│   ├── theme.service.ts
│   └── validators.ts
├── layout/                  ✓ Shell component
│   └── shell.component.ts
├── shared/                  ✓ Componentes reutilizáveis
│   ├── alert.component.ts   ✗ Template inline
│   └── field.component.ts   ✗ Template inline
├── features/                ✓ Organizado por domínio, mas sem subpastas
│   ├── admin/
│   │   ├── audit.component.ts
│   │   ├── client-detail.component.ts
│   │   ├── client-form.component.ts
│   │   ├── clients-list.component.ts
│   │   ├── dashboard.component.ts
│   │   ├── invitations.component.ts
│   │   ├── user-detail.component.ts
│   │   ├── user-form.component.ts
│   │   └── users-list.component.ts
│   ├── auth/
│   │   ├── invite-accept.component.ts
│   │   ├── login.component.ts
│   │   ├── recovery-request.component.ts
│   │   ├── recovery-reset.component.ts
│   │   ├── register.component.ts
│   │   ├── setup.component.ts
│   │   ├── verify-confirm.component.ts
│   │   └── verify-request.component.ts
│   ├── account/
│   │   ├── change-password.component.ts
│   │   └── sessions.component.ts
│   └── demo/
│       ├── sso-callback.component.ts
│       └── sso-demo.component.ts
├── app.component.ts         ✓ Mínimo (apenas router-outlet + init)
├── app.config.ts
└── app.routes.ts
```

---

## 2. Problemas Identificados

### 2.1 Models de Domínio no Local Errado
**Arquivo:** `core/api.service.ts` (linhas 8-58)
```typescript
// Models definidos no service de API — vazam domínio para infraestrutura
export interface Usuario { ... }
export interface Pagina<T> { ... }
export interface Sessao { ... }
export interface Cliente { ... }
export interface EventoAuditoria { ... }
```
**Problema:** Quebra separação de camadas. Domain models pertencem às features, não ao `core/`.

### 2.2 Templates Inline em Componentes Complexos
**Arquivos:** `shared/alert.component.ts`, `shared/field.component.ts`, `features/admin/users-list.component.ts`, etc.
```typescript
@Component({
  template: `
    <section>... 200+ linhas de HTML ...</section>
  `
})
```
**Problema:** Dificulta leitura, manutenção, revisão de código, ferramentas de lint/format, e colaboração com designers.

### 2.3 Features sem Subpastas por Responsabilidade
Atual:
```
features/admin/users-list.component.ts  // Tudo junto: template, lógica, chamadas HTTP, estado
```
Esperado:
```
features/admin/
├── pages/
│   └── users-list.page.ts
├── components/
│   ├── user-table.component.ts
│   ├── user-filters.component.ts
│   └── user-pagination.component.ts
├── services/
│   └── admin-users.service.ts
└── models/
    └── user.model.ts
```

### 2.4 Componentes "Smart" Fazendo Tudo
**Exemplo:** `users-list.component.ts`
- Template HTML (200+ linhas)
- Estado local (signals)
- Chamadas HTTP diretas (`this.api.listarUsuarios()`)
- Lógica de paginação/filtros
- Tratamento de erros
- TrackBy functions

**Problema:** Viola Single Responsibility Principle. Não é testável isoladamente. Difícil de reutilizar partes.

### 2.5 Ausência de Regras Arquiteturais Frontend
Não existe arquivo em `.ia/rules/` definindo:
- Feature-Based Architecture obrigatória
- Separação smart/presentational
- Onde colocar models, services, components
- Critérios de validação (tamanho máx de componente, templates separados, etc.)

### 2.6 Skill Frontend não Especifica Arquitetura
`skills/frontend/SKILL.md` diz "mapear componentes, padrões e fluxos existentes" mas não define **quais** padrões seguir.

---

## 3. Análise das Instruções Existentes (`.ia/`)

| Arquivo | Status | Problema |
|---------|--------|----------|
| `agents/frontend/AGENT.md` | ✓ Existe | Referencia `skills/frontend/SKILL.md` mas não define arquitetura |
| `skills/frontend/SKILL.md` | ✓ Existe | Processo genérico, sem padrões Angular específicos |
| `skills/ui-ux-design/SKILL.md` | ✓ Existe | Foco em design, não em arquitetura de código |
| `rules/quality/quality.md` | ✓ Existe | Princípios gerais (baixo acoplamento, alta coesão) mas sem regras frontend-specific |
| `rules/architecture/principles.md` | ✓ Existe | SOLID, DDD, MVC — genéricos, não específicos para Angular |
| `rules/architecture/layers.md` | ✓ Existe | Camadas backend (Controller/Service/Repository) — não se aplica a frontend |
| `rules/reuse/reuse.md` | ✓ Existe | Processo de reuse scan, não arquitetura |

**Gap crítico:** Não há **nenhuma regra** definindo a arquitetura Angular esperada (feature-based, core/shared/feature, separação de responsabilidades, templates separados, etc.).

---

## 4. Proposta de Alterações

### 4.1 Nova Rule: `rules/frontend/angular-architecture.md`

Criar regra obrigatória definindo:
- Estrutura de pastas obrigatória
- Separação smart/presentational components
- Models por feature (não em core)
- Templates em arquivos `.html` separados para componentes > 50 linhas
- Services por feature para lógica de negócio e chamadas HTTP
- Validação automática (ESLint rules, tamanho máx de arquivo)

### 4.2 Atualização: `skills/frontend/SKILL.md`

Adicionar seção "Arquitetura Obrigatória" referenciando a nova rule.

### 4.3 Atualização: `agents/frontend/AGENT.md`

Adicionar leitura obrigatória da nova rule em "Ler sempre".

### 4.4 Critérios de Validação (GATE-ANL / GATE-QLT)

Adicionar verificações:
- [ ] Nenhum model de domínio em `core/`
- [ ] Templates > 50 linhas em arquivo `.html` separado
- [ ] Features têm subpastas `pages/`, `components/`, `services/`, `models/` quando aplicável
- [ ] Componentes não injetam `ApiService` diretamente (usam feature services)
- [ ] ESLint rule `max-lines` configurado (ex.: 300 linhas/componente)
- [ ] Nenhum componente com template inline > 50 linhas

---

## 5. Arquivos a Modificar

| Arquivo | Ação |
|---------|------|
| `.ia/rules/frontend/angular-architecture.md` | **CRIAR** (nova rule) |
| `.ia/skills/frontend/SKILL.md` | **EDITAR** (adicionar referência à rule) |
| `.ia/agents/frontend/AGENT.md` | **EDITAR** (adicionar leitura obrigatória) |
| `frontend/eslint.config.js` (ou similar) | **EDITAR** (adicionar rules de validação) — *se existir* |

---

## 6. Próximos Passos

1. **Aprovação** deste diagnóstico e proposta
2. **Criação** da rule `angular-architecture.md`
3. **Atualização** do skill e agent frontend
4. **Configuração** de ESLint rules de validação (opcional, mas recomendado)
5. **Refatoração futura** do código existente (fora desta etapa)

---

## 7. Preservação da Criatividade Visual

> **Importante:** As regras arquiteturais acima são **apenas de engenharia de software** (organização de código, separação de responsabilidades, testabilidade). Elas **NÃO** restringem:
- Design visual, cores, tipografia, espaçamento
- Animações, transições, microinterações
- Componentes visuais diferenciados
- Dark/light mode, responsividade
- Experiências de usuário inovadoras

A IA continua **livre para propor interfaces modernas e criativas** — apenas deve organizar o código seguindo a arquitetura definida.