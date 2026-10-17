# Achernar Identity — Decisões de Implementação (IMP-xx)

Registradas durante a implementação E0→E9 (execução única autorizada por GATE-AUT em 2026-10-17). Complementam
`decisions.md` (D-01…D-51); nenhuma regra de negócio foi alterada — apenas escolhas técnicas dentro do que as specs permitem.

## 1. Backend

| ID | Decisão | Motivo / efeito |
|---|---|---|
| IMP-01 | Persistência com **Spring JDBC (`JdbcClient`) + Liquibase**, sem JPA/Hibernate | SQL explícito para locks/`FOR UPDATE SKIP LOCKED`, rate limit e outbox no PostgreSQL; menos magia no caminho de segurança |
| IMP-02 | **Filtro Bearer próprio** (Nimbus JOSE) em vez do `JwtDecoder` do Resource Server | validação de `iss`, `aud` do Identity, `exp/nbf` com skew, `kid` entre chaves publicadas e `sid` ativo no mesmo ponto |
| IMP-03 | Lista de senhas comuns **gerada** (`tools/gen_common_passwords.py` → `common-passwords.txt`) | RN de política sem dependência externa em runtime |
| IMP-04 | Clientes OAuth do Google/Facebook **escritos à mão** com hosts fixos no código (sem URL configurável) | elimina SSRF por configuração (SEC); só `client_id/secret` são parâmetros |
| IMP-05 | **Intenção social sem estado no servidor**: `state` assinado/transação em PostgreSQL, cookie de vínculo (binding) por transação; troca por **código próprio de uso único + PKCE** | `social-login.md` §3; fragmentos nunca carregam token de sessão |
| IMP-06 | Links dos e-mails (TPL-05 etc.) apontam para a **UI do Identity** quando a aplicação não define URL de ação | UI semeada (`achernar-identity-ui`) cobre verify/reset/email-change |
| IMP-07 | Regra `DEFAULT_REQUIRED`: aplicação ativa precisa de ao menos uma API padrão ativa, salvo acesso exclusivo à API do Identity | RN-APP-17 |
| IMP-08 | Resposta de reautenticação `200` com corpo conforme a spec (FT-06) | consistência com `api.md` |
| IMP-09 | Perfil `dev` **versionado** (`application-dev.yml`) — removida a linha correspondente do `.gitignore` | o perfil padrão é `dev`; o arquivo não contém segredos (apenas `cookie.secure=false` e log) |
| IMP-10 | Aplicação da UI semeada com redirect `…/auth/callback` | rota única de retorno do login social/vínculo/reautenticação |
| IMP-11 | Testes: unitários `*Test` (Surefire), arquitetura (ArchUnit) e integração `*IT` (Failsafe + Testcontainers PostgreSQL + GreenMail) | `mvn verify` roda todos |

## 2. Frontend

| ID | Decisão | Motivo / efeito |
|---|---|---|
| IMP-20 | Angular 22 **standalone + signals**, zero NgModules; estrutura `core / layout / shared / features/<f>/{pages,components,services,models}` | `.ia/rules/frontend/angular-architecture.md` |
| IMP-21 | Material 22 tema **M3 (`mat.theme`)** com tokens `--ach-*` mapeados de `--mat-sys-*`; claro/escuro/sistema | visual-proposal.md |
| IMP-22 | **Material Icons (outlined)** via fontsource, em vez de Material Symbols | pacote estável e leve, auto-hospedado (CSP `font-src 'self'`) |
| IMP-23 | i18n com **Transloco**, arquivos JSON **planos** (`chave.com.pontos`) em `public/i18n/{pt-BR,en}.json` | evita colisão chave-folha × prefixo; paridade verificada por teste e `npm run i18n:check` |
| IMP-24 | Access token **somente em memória**; refresh por cookie HttpOnly; `RefreshCoordinator` single-flight + Web Locks; `BroadcastChannel` para login/logout entre abas | SEC-12 |
| IMP-25 | `apiInterceptor` funcional: base URL em tempo de execução (`config.json`), `Accept-Language`, `X-Achernar-Client` em `/auth/*`, Bearer em `/api/*`, 401 ⇒ 1 refresh + 1 repetição, `REAUTH_REQUIRED` ⇒ diálogo + 1 repetição | ui-map §7 |
| IMP-26 | Login social: `state` e `code_verifier` PKCE em `sessionStorage` (≤ 10 min); retorno em `/auth/callback`; tokens em fragmento removidos com `history.replaceState` | social-login.md |
| IMP-27 | ESLint: `max-lines` 300/arquivo, 50/função, templates > 50 linhas em `.html`, `prefer-inject`, `prefer-standalone`, OnPush obrigatório, regras de acessibilidade de template | angular-architecture.md |
| IMP-28 | Testes: Vitest (`@angular/build:unit-test`) com jsdom e **axe-core**; sem animações legadas (`@angular/animations` não existe no Angular 22) | |
| IMP-29 | **Node 24 LTS** obrigatório (Angular 22 recusa Node 22) | config-changes.md §5 |

## 3. Operação (E0)

| ID | Decisão |
|---|---|
| IMP-40 | Compose de desenvolvimento: **PostgreSQL 18 + Mailpit** (substitui MailHog, sem manutenção); volume `achernar_identity_pgdata18` (PG18 mudou o diretório de dados) |
| IMP-41 | Produção de referência: **Caddy** (TLS) serve UI e API na **mesma origem**; Actuator (9090) não publicado; imagens rodam como usuário não-root |
| IMP-42 | CI (GitHub Actions): backend `mvn verify`; frontend `i18n:check`, `ng lint`, `ng test`, `ng build` |

## 4. Limitações de verificação nesta execução

O ambiente de execução **não conseguiu baixar dependências Maven nem executar Docker**. Consequências, declaradas para revisão:

- O **backend não foi compilado com Maven** nem testado contra PostgreSQL. Foi verificado por `javac` sobre o código-fonte (com stubs mínimos das bibliotecas) e por execução dos testes **unitários puros** (92/92) com um executor JUnit mínimo. Os testes `*IT` e `ArchitectureTest` estão escritos, mas **não foram executados**.
- APIs do Spring Boot 4.1 / Security 7 / Jackson 3 que merecem atenção na primeira compilação: `HttpSecurity.securityMatcher(String...)`, `UsernamePasswordAuthenticationToken.authenticated`, bean `TransactionTemplate`, pacote das anotações Jackson, `ResponseCookie` e as classes de teste do Boot 4.
- O **frontend foi compilado e testado de fato** (`ng build`, `ng lint`, 62 testes Vitest); não houve teste ponta a ponta contra o backend real.

## 5. Revalidação real do backend — 17/10/2026

As limitações Maven/Docker da execução original (§4) foram resolvidas na retomada. `mvn -B verify` executou com sucesso no host e `mvn -B clean verify` na imagem oficial Maven com Java 25: **96 testes unitários/arquitetura + 19 testes de integração**, zero falhas/erros/ignorados. Integração usa PostgreSQL 18 real via Testcontainers e GreenMail. Expectativas de três cenários foram alinhadas aos contratos aprovados e contadores de taxa isolados entre cenários. Evidência consolidada: `state/backend-validation.md` (símbolo resolvido por `config/paths.yaml`).

Esse resultado comprova build e suíte existente; não substitui a cobertura completa dos cenários das Specs, revisão humana, login social com credenciais reais ou GATE-CLS global.
