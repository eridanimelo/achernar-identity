---
name: rule-frontend-angular-architecture
type: rule
description: "Arquitetura Angular - Feature-Based, Separação de Responsabilidades, Padrões Obrigatórios"
---

# Arquitetura Angular Obrigatória

Esta rule define a arquitetura **obrigatória** para todo código frontend Angular neste projeto. Não é sugestão — é exigência verificável nos gates `GATE-ANL` e `GATE-QLT`.

---

## 1. Estrutura de Pastas (Feature-Based Architecture)

```
src/app/
├── core/                      # Serviços globais, singleton, transversais
│   ├── auth/                  # Interceptors, guards, tokens
│   ├── config/                # Constantes, environment, feature flags
│   ├── i18n/                  # Internacionalização
│   ├── services/              # Serviços globais (Theme, Session, etc.)
│   ├── utils/                 # Utilitários puros (validators, formatters)
│   └── models/                # APENAS tipos transversais (ApiError, Page<T>, etc.)
├── layout/                    # Estrutura visual compartilhada
│   ├── shell/                 # ShellComponent + componentes de layout
│   └── navigation/            # Header, sidebar, breadcrumbs, footer
├── shared/                    # Realmente reutilizável entre features
│   ├── components/            # UI primitives (Button, Input, Card, Table, Alert, Modal, etc.)
│   ├── directives/            # Diretivas reutilizáveis
│   ├── pipes/                 # Pipes reutilizáveis
│   ├── validators/            # Validators reutilizáveis
│   └── utils/                 # Helpers de UI (date, currency, etc.)
├── features/                  # **Um diretório por domínio/feature**
│   ├── <feature-name>/
│   │   ├── pages/             # Componentes de rota (smart components)
│   │   ├── components/        # Componentes de apresentação (dumb/presentational)
│   │   ├── services/          # Services específicos da feature (lógica + HTTP)
│   │   ├── models/            # Interfaces/types da feature (domain models)
│   │   ├── guards/            # Guards específicos da feature
│   │   └── <feature>.routes.ts # Rotas lazy-loaded da feature
│   └── ...
└── app.routes.ts              # Rotas raiz + lazy loading de features
```

### Regras de Localização

| Tipo de Artefato | Onde Colocar |
|------------------|--------------|
| Domain models (Usuario, Cliente, Sessao) | `features/<feature>/models/` |
| Feature services (UserService, ClientService) | `features/<feature>/services/` |
| Page components (roteáveis) | `features/<feature>/pages/` |
| Presentational components | `features/<feature>/components/` ou `shared/components/` se realmente global |
| Global services (Theme, Session, Auth) | `core/services/` |
| HTTP interceptors, guards globais | `core/auth/` |
| API types genéricos (Page<T>, ApiError) | `core/models/` |
| UI primitives (Button, Input, Card) | `shared/components/` |

---

## 2. Separação Smart / Presentational Components

### Smart Components (Pages / Containers)
- **Localização:** `features/<feature>/pages/`
- **Responsabilidades:**
  - Injetam feature services
  - Gerenciam estado (signals, computed)
  - Orquestram dados e passam para presentational components
  - Lidam com navegação (Router)
  - Tratam erros globais da tela
- **NÃO fazem:** Lógica de apresentação pura, HTML complexo, estilos

### Presentational Components (Dumb / UI)
- **Localização:** `features/<feature>/components/` ou `shared/components/`
- **Responsabilidades:**
  - Recebem dados via `@Input()` / `@Input({required: true})` ou `input()`
  - Emitem eventos via `@Output()` / `output()`
  - Contêm template HTML, estilos, lógica de apresentação
  - São puros, testáveis, reutilizáveis
- **NÃO fazem:** Injeção de services, chamadas HTTP, navegação, gerenciamento de estado global

---

## 3. Models de Domínio

### Regra Obrigatória
**Models de domínio NUNCA ficam em `core/api.service.ts` ou `core/`.**

Cada feature define seus próprios models em `features/<feature>/models/`:

```typescript
// features/admin/models/user.model.ts
export interface Usuario {
  id: string;
  nome: string;
  email: string;
  perfil: 'ADMIN' | 'USER';
  estado: 'ATIVO' | 'BLOQUEADO';
  verificado: boolean;
  verificadoEm?: string | null;
}

export interface UsuarioFiltros {
  nome?: string;
  email?: string;
  perfil?: string;
  estado?: string;
  sort?: string;
  page?: number;
  size?: number;
}

export interface UsuarioPagina {
  items: Usuario[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}
```

### Tipos Genéricos (Permitidos em `core/models/`)
```typescript
// core/models/api.types.ts
export interface Page<T> { items: T[]; page: number; size: number; total: number; totalPages: number; }
export interface ApiError { codigo: string; mensagem: string; detalhes?: Record<string, unknown>; }
```

---

## 4. Feature Services

### Responsabilidades
- Encapsulam chamadas HTTP (via `HttpClient` ou `ApiService` genérico)
- Contêm lógica de negócio da feature
- Retornam tipagem forte (models da feature)
- São injetados **apenas** em smart components (pages)

### Exemplo
```typescript
// features/admin/services/admin-users.service.ts
@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  constructor(private http: HttpClient, private env: Environment) {}

  listar(filtros: UsuarioFiltros): Observable<UsuarioPagina> { ... }
  obter(id: string): Observable<Usuario> { ... }
  criar(body: CriarUsuarioDto): Observable<Usuario> { ... }
  atualizar(id: string, body: AtualizarUsuarioDto): Observable<Usuario> { ... }
  bloquear(id: string, motivo: string): Observable<Usuario> { ... }
  desbloquear(id: string): Observable<Usuario> { ... }
}
```

**Smart component usa:**
```typescript
// features/admin/pages/users-list.page.ts
@Component({ ... })
export class UsersListPage {
  private usersService = inject(AdminUsersService);
  // ... estado e lógica de apresentação
}
```

---

## 5. Templates em Arquivos Separados

### Regra Obrigatória
**Qualquer componente com template > 50 linhas DEVE ter arquivo `.html` separado.**

```typescript
// features/admin/pages/users-list.page.ts
@Component({
  selector: 'app-users-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, UserTableComponent, UserFiltersComponent, UserPaginationComponent, AlertComponent],
  templateUrl: './users-list.page.html',  // ← ARQUIVO SEPARADO
  styleUrl: './users-list.page.scss'
})
export class UsersListPage { ... }
```

### Benefícios
- Legibilidade e manutenção
- Code review eficiente (diff separado)
- Ferramentas de lint/format/prettier funcionam corretamente
- Colaboração com designers (arquivo HTML puro)
- Separação clara de responsabilidades

### Componentes Pequenos (≤ 50 linhas)
Podem usar `template: \`...\`` inline se forem:
- Presentational components simples (Button, Badge, Avatar, etc.)
- Componentes de layout simples
- Wrappers triviais

---

## 6. Standalone Components + Lazy Loading

### Obrigatório
- Todos os componentes são `standalone: true`
- Rotas usam lazy loading: `loadChildren` com `<feature>.routes.ts` (preferido para features com múltiplas rotas) ou `loadComponent` por rota (aceito como lazy; mínimo verificável). MUST NOT haver `import` estático de componente de feature em `app.routes.ts`.

```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
      {
        path: 'auth',
        loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
      },
      {
        path: 'conta',
        canActivate: [authGuard],
        loadChildren: () => import('./features/account/account.routes').then(m => m.ACCOUNT_ROUTES)
      },
      { path: 'demo', loadChildren: () => import('./features/demo/demo.routes').then(m => m.DEMO_ROUTES) }
    ]
  }
];
```

```typescript
// features/admin/admin.routes.ts
export const ADMIN_ROUTES: Routes = [
  { path: '', component: DashboardPage },
  { path: 'usuarios', component: UsersListPage },
  { path: 'usuarios/novo', component: UserFormPage },
  { path: 'usuarios/:id', component: UserDetailPage },
  { path: 'clientes', component: ClientsListPage },
  { path: 'clientes/novo', component: ClientFormPage },
  { path: 'clientes/:id', component: ClientDetailPage },
  { path: 'auditoria', component: AuditPage },
  { path: 'convites', component: InvitationsPage }
];
```

---

## 7. Signals e APIs Modernas

### Uso Recomendado
- `signal()` para estado local mutável
- `computed()` para estado derivado
- `effect()` apenas para side effects (logging, persistência local, etc.)
- `input()` / `output()` para componentes presentational (Angular 17+)
- `linkedSignal()` para estado sincronizado com input

### Exemplo Smart Component
```typescript
// features/admin/pages/users-list.page.ts
@Component({ ... })
export class UsersListPage {
  private usersService = inject(AdminUsersService);
  private destroyRef = inject(DestroyRef);

  // Estado
  readonly filtros = signal<UsuarioFiltros>({ page: 0, size: 20, sort: 'nome,asc' });
  readonly pagina = signal<UsuarioPagina | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);

  // Derivado
  readonly usuarios = computed(() => this.pagina()?.items ?? []);
  readonly totalPages = computed(() => this.pagina()?.totalPages ?? 0);
  readonly temResultados = computed(() => this.usuarios().length > 0);

  ngOnInit() {
    this.carregar();
  }

  async carregar() {
    this.carregando.set(true);
    this.erro.set(null);
    try {
      const resultado = await firstValueFrom(this.usersService.listar(this.filtros()));
      this.pagina.set(resultado);
    } catch (e) {
      this.erro.set(tratarErro(e));
    } finally {
      this.carregando.set(false);
    }
  }

  // ... handlers de UI que atualizam filtros e chamam carregar()
}
```

---

## 8. Validação Automatizada (ESLint)

Configurar no `eslint.config.js` (ou `.eslintrc.json`):

```javascript
module.exports = {
  rules: {
    // Tamanho máximo de arquivo (lines)
    'max-lines': ['error', { max: 300, skipBlankLines: true, skipComments: true }],

    // Tamanho máximo de função
    'max-lines-per-function': ['error', { max: 50, skipBlankLines: true, skipComments: true }],

    // Template inline máximo (apenas para componentes pequenos)
    '@angular-eslint/template-inline': ['error', { maxLines: 50 }],

    // Prefer standalone components
    '@angular-eslint/prefer-standalone': 'error',

    // Prefer inject() over constructor injection
    '@angular-eslint/prefer-inject': 'error',

    // Signals preferidos para estado
    // (requer plugin eslint-plugin-angular-signals ou similar)

    // Proibir injeção de ApiService genérico em components
    'no-restricted-imports': ['error', {
      patterns: [{
        group: ['../../core/api.service', '../../../core/api.service'],
        message: 'Use feature service (ex: AdminUsersService) em vez de ApiService diretamente. Crie service em features/<feature>/services/.'
      }]
    }]
  }
};
```

---

## 9. Critérios de Validação (GATE-ANL / GATE-QLT)

Antes de considerar uma feature frontend concluída, **TODOS** devem passar:

| Critério | Verificação |
|----------|-------------|
| ✅ Models de domínio em `features/<feature>/models/` | `grep -rn "export interface \(Usuario\|Cliente\|Sessao\|EventoAuditoria\|Pagina\)" core/ --include="*.ts" \| grep -v "core/models/api.types.ts"` retorna vazio (`Pagina<T>` genérico em `api.types.ts` e tipos transversais como `ErroApi`/`SessaoLocal` permanecem em `core/`) |
| ✅ Templates > 50 linhas em `.html` separado | `grep -r "template: \`" --include="*.ts" | while read line; do ... contar linhas ...` |
| ✅ Features têm subpastas conforme necessidade | `ls features/<feature>/` contém `pages/`, `services/`, `models/` quando houver conteúdo correspondente; `components/` somente quando houver presentational próprio; MUST NOT existir pasta vazia só para cumprir tabela |
| ✅ Nenhum componente injeta `HttpClient` diretamente | `grep -rn "inject(HttpClient)" features/ shared/ layout/ --include="*.ts"` retorna vazio |
| ✅ `app.component.ts` mínimo | `wc -l app.component.ts` ≤ 30 linhas; só shell/router-outlet + init global |
| ✅ Smart components não têm HTML complexo | Code review: pages/ só orquestram, components/ têm HTML |
| ✅ Feature services existem e são usados | `grep -r "inject.*Service" features/<feature>/pages/` — todos usam feature service |
| ✅ Nenhum componente injeta `ApiService` diretamente | `grep -rn "inject(ApiService)" features/ shared/ layout/ --include="*.ts" \| grep -v "/services/"` retorna vazio (services da feature MAY usar `ApiService` como transporte) |
| ✅ Lazy loading configurado | `app.routes.ts` usa `loadChildren` para features |
| ✅ Tamanho de arquivos ≤ 300 linhas | ESLint `max-lines` passa |
| ✅ Testes unitários para feature services | `features/<feature>/services/*.spec.ts` existe |

---

## 10. Exceções e Pragmatismo

| Situação | Permitido? | Condição |
|----------|------------|----------|
| Componente presentational trivial (Button, Badge) | Template inline | ≤ 50 linhas, sem lógica complexa |
| Feature com 1 única página simples | Sem subpastas `components/`/`services/` | Criar `pages/`, `models/`, `services/` mesmo assim; `components/` opcional |
| Utilitário puro (formatDate, cpfMask) | `shared/utils/` ou `core/utils/` | Sem dependências Angular, testável isoladamente |
| Guard global (authGuard) | `core/auth/guards.ts` | Usado por múltiplas features |

**Regra de ouro:** Sem pastas vazias e sem fragmentação excessiva. Criar `pages/`, `services/`, `models/` quando houver conteúdo correspondente; criar `components/` ou `guards/` somente quando houver presentational ou guard próprio da feature.Extrair componente/arquivo novo somente com responsabilidade clara e reuso ou separação comprovada.

---

## 11. Migração Gradual (Legado)

Para código existente que viola esta rule:
1. **Não bloquear** features novas por causa de legado
2. **Nova feature** = segue esta rule obrigatoriamente
3. **Alteração em código legado** = oportunidade de migrar o arquivo tocado
4. **Refatoração planejada** = criar issue/tarefa dedicada, não misturar com feature nova

---

## 12. Engenharia Obrigatória vs UI/UX Flexível

Obrigatório (verificável em GATE-ANL/GATE-QLT): estrutura `core/`/`shared/`/`features/`/`layout/`, models por feature, feature services, smart/presentational, `templateUrl` acima de 50 linhas, standalone + lazy, sem `ApiService`/`HttpClient` em componente, sem god-service em `core/`.

Flexível (criatividade preservada, `.agents/skills/ui-ux-design/SKILL.md`): design visual, cores, tipografia, animações, dark/light mode, responsividade, microinterações e composição de telas. Nenhuma exigência desta rule limita essas escolhas.

## 13. Referências

- `.agents/skills/frontend/SKILL.md` — Procedimento de implementação (deve referenciar esta rule)
- `subagents/frontend/AGENT.md` — Contrato do agente (deve exigir leitura desta rule)
- `rules/quality/quality.md` — Princípios gerais (baixo acoplamento, alta coesão)
- `rules/architecture/principles.md` — SOLID, separação de responsabilidades
- Angular Style Guide: https://angular.io/guide/styleguide
- Feature-Based Architecture: https://angular.io/guide/feature-modules