# Achernar Identity — Arquitetura

Documento técnico (D-49). Decisões: D-02, D-03, D-30, D-31, D-34, D-35, D-43. Princípio: **serviço independente, modular, sem abstrações sem necessidade**.

## 1. Contexto e componentes

```text
                                ┌────────────────────────────── Instalação do Identity (por projeto) ──────────────────────────────┐
 Navegador ──HTTPS──▶ Proxy ───▶│  achernar-identity-ui (Angular, estático)                                                         │
 (SPA do projeto, UI Identity)  │  achernar-identity-api (Spring Boot)  ──JDBC──▶ PostgreSQL 18                                      │
 APIs consumidoras ─JWKS/intro─▶│        │  └──SMTP──▶ servidor de e-mail (MailHog em dev)                                           │
                                │        └──HTTPS──▶ Google / Facebook (apenas login social)                                        │
                                └───────────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Componente | Responsabilidade | Tecnologia (D-03) |
|---|---|---|
| `achernar-identity-api` | contas, autenticação, sessões, tokens, login social, registro de aplicações/APIs, chaves, auditoria, e-mail (outbox), jobs | Java 25, Spring Boot 4.1, Spring Security 7 |
| `achernar-identity-ui` | telas de autenticação, conta, sessões, administração | Angular 22 standalone, Angular Material 22 |
| PostgreSQL | estado durável, restrições de integridade, contadores de taxa, filas (outbox) | PostgreSQL 18, Liquibase |
| Proxy reverso | TLS, HSTS, cabeçalhos encaminhados, roteamento (`/` UI, `/auth|/api|/setup|/.well-known` API) | Caddy (perfil `prod`) |
| SMTP | entrega de e-mail | MailHog (dev) / provedor SMTP (prod, pendente) |

Mesma origem pública (`https://id.exemplo.com.br`) serve UI e API ⇒ CORS só é necessário para as **aplicações consumidoras** (origens registradas).

## 2. Backend — estrutura modular

Pacote raiz `com.eridani.achernar.identity`. Cada módulo por funcionalidade com camadas internas **somente quando contribuem**:

```text
shared/      config (@ConfigurationProperties validadas), web (RequestIdFilter, OriginCheckFilter, ClientAuthResolver, security headers),
             error (ProblemDetailsAdvice, ErrorCode), crypto (TokenGenerator, Sha256, MasterKeyCipher), time (Clock), logging (RedactionFilter/Converter), i18n (MessageSource)
setup/       UC-001            web · application · (usa account + registry)
account/     UC-010,011,012,034,035,036   web · application · domain (Account, OneTimeToken, EmailChange) · infra (repos)
credential/  PasswordPolicy, PasswordHasher (Argon2id + semáforo SEC-06), PasswordCredential, UC-032,033,037,030,031
auth/        UC-020,021,022   web · application (LoginService, RefreshService, LogoutService)
session/     Session, RefreshToken, SessionService (criar/revogar/limitar), UC-040,041,042
token/       JwtIssuer, SigningKeyService, JwksController, KeyRotationJob, IntrospectionService (UC-070..073)
social/      UC-050..055: ProviderRegistry, SocialTransactionService, SocialAccountResolver, ExchangeService, adapters Google/Facebook
registry/    Application, ResourceApi, secrets, ClientAuthenticator, DynamicCorsConfigurationSource, UC-060..068
mail/        EmailOutbox, TemplateRenderer, OutboxWorker, SmtpMailSender (UC-095)
audit/       SecurityEvent, SecurityEventRecorder (TX / REQUIRES_NEW), SecurityEventQuery (UC-090)
ratelimit/   RateLimiter (contadores), LoginLockService
admin/       AccountAdminService (UC-080..084), controllers /api/v1/admin/**
retention/   RetentionJob, MaintenanceJobs (UC-091,092)
```

Regras estruturais (verificadas por testes de arquitetura):
1. `web` depende de `application`; `application` de `domain` e portas; `domain` não depende de `web`/Spring MVC.
2. Módulos conversam por **serviços `application` públicos** (interfaces ou classes públicas únicas por módulo); repositórios e entidades internas são `package-private`/não expostas.
3. Portas (interfaces) **somente** para: `MailSender`, `SocialProviderClient` (Google, Facebook), `Clock`. Demais colaboradores são classes concretas.
4. Entidades JPA são o modelo de domínio (sem DTO espelho no domínio). DTOs de `web` são `record`s com validação declarativa; mapeamento manual.
5. Chamadas externas (SMTP, provedores sociais) **nunca** dentro de transação de banco.
6. Propriedades de configuração são classes tipadas validadas na subida (PV-01…PV-06).

### 2.1 Mapeamento UC → módulo

| Módulo | UCs |
|---|---|
| setup | UC-001 |
| account | UC-010, 011, 012, 034, 035, 036 |
| credential | UC-030, 031, 032, 033, 037 |
| auth | UC-020, 021, 022 |
| session | UC-040, 041, 042 |
| token | UC-070, 071, 072, 073 |
| social | UC-050, 051, 052, 053, 054, 055 |
| registry | UC-060…068 |
| mail | UC-095 |
| audit / retention | UC-090 / UC-091, 092 |
| admin | UC-080…084 (+ delega a account/session) |

## 3. Segurança (Spring Security 7)

Cadeias de filtros (ordem):

| Ordem | `securityMatcher` | Autenticação | Detalhes |
|---|---|---|---|
| 1 | porta de gestão (`/actuator/**`) | rede privada | porta própria 9090; `health/liveness`, `health/readiness`, `prometheus` |
| 10 | `/auth/**`, `/setup/**`, `/.well-known/**` | pública | CORS dinâmico (D-41); CSRF desativado + `OriginCheckFilter` + exigência de `X-Achernar-Client` nas rotas com cookie; `ClientAuthResolver` (BROWSER por origem, CONFIDENTIAL por Basic) |
| 20 | `/api/v1/tokens/introspect` | HTTP Basic (credencial da API) | `ResourceApiAuthenticationProvider` (hash SHA-256 + tempo constante) |
| 30 | `/api/v1/**` | Resource Server JWT | `JwtDecoder` local (JWKSource do `SigningKeyService`, sem HTTP para si mesmo), validadores: `iss`, `aud` ∋ P-63, `exp/nbf` (skew P-06), `typ=at+jwt`, **sessão ativa** (`sid` no banco); `@PreAuthorize` com `AdminGuard` (consulta DB: perfil `ADMIN`, conta `ACTIVE`) |
| 99 | qualquer outra | `denyAll` | — |

- Cabeçalhos de segurança (SEC-35) em todas as cadeias; `Cache-Control: no-store` em respostas sensíveis.
- `AuthenticationEntryPoint` e `AccessDeniedHandler` produzem `application/problem+json`.
- Cookies: gerados por um único `RefreshCookieFactory` (nome, flags, `Path=/auth`, `Max-Age`).
- Login social: Spring Security OAuth2 Client com `OAuth2AuthorizationRequestResolver` customizado (PKCE S256, `nonce`), `AuthorizationRequestRepository` e transação persistidos em PostgreSQL, tratadores de sucesso/falha que produzem os redirecionamentos de `social-login.md` (não usar `oauth2Login()` com sessão HTTP).
- Sem sessão HTTP do servlet em nenhuma cadeia (`STATELESS`).

## 4. Transações, concorrência e erros

| Tema | Regra |
|---|---|
| Transação | uma por caso de uso (`@Transactional` em `application`); leitura `readOnly` |
| Eventos de auditoria | de sucesso: mesma transação; de falha: `REQUIRES_NEW` (D-26) |
| Outbox | `INSERT` na mesma transação da mudança (D-18) |
| Refresh | `SELECT … FOR UPDATE` do `refresh_token` (D-43) |
| Último admin / limite de sessões | `pg_advisory_xact_lock` (chaves: `hash('admin-set')`, `hash('account-sessions:'+id)`) |
| Consumo único | `UPDATE … WHERE consumed_at IS NULL AND expires_at > now()` ⇒ 1 linha |
| Unicidade | restrições do banco; `DataIntegrityViolationException` traduzida pelo caso de uso |
| Otimista | `@Version` em `account`, `application`, `resource_api` |
| Erros | `ProblemDetailsAdvice` mapeia exceções de domínio (`ErrorCode`) → HTTP + `error-catalog.md`; exceção inesperada ⇒ 500 genérico + `trace_id` |
| Validação | Bean Validation nos DTOs + validadores de domínio (política de senha, e-mail, URLs) |
| Isolamento | `READ COMMITTED` (padrão) é suficiente; invariantes críticas usam travas/restrições |

## 5. Jobs agendados (idempotentes; `pg_try_advisory_lock` evita execução duplicada em N instâncias)

| Job | Frequência | UC |
|---|---|---|
| Rotação de chaves | de hora em hora | UC-071 |
| Worker do outbox | a cada P-58 | UC-095 |
| Retenção/purga | diário 03:00 UTC | UC-091 |
| Manutenção (contas pendentes, tokens, contadores, sessões expiradas) | diário 03:30 UTC | UC-092 |

## 6. Observabilidade (D-35)

Logs JSON estruturados (formato nativo do Spring Boot), MDC com `trace_id`, `request_id`, `client_id`, `account_id` (UUID, nunca e-mail); filtro de redação (SEC-31).
Métricas Micrometer: `achernar_login_total{result}`, `achernar_refresh_total{result}`, `achernar_refresh_reuse_total{kind}`, `achernar_social_login_total{provider,result}`,
`achernar_hash_queue_wait_seconds`, `achernar_mail_outbox_pending`, `achernar_security_alert_total{type}`, `achernar_signing_key_age_seconds`.
Health: `readiness` = banco + existência de chave `ACTIVE`; `liveness` = processo.

## 7. Frontend — estrutura (Angular 22)

```text
src/
  app/
    app.config.ts · app.routes.ts · app.ts
    core/
      auth/        auth.service (estado em signals), token-store (memória), refresh-coordinator (Web Locks + BroadcastChannel), reauth.service
      http/        api-client (adaptador snake↔camel), problem.model, interceptors: bearer, refresh-on-401, error→notificação, accept-language
      guards/      auth.guard, admin.guard, guest.guard, setup.guard
      i18n/        transloco.config, locale.service
      theme/       theme.service (system|light|dark; persistência localStorage `achernar.theme`)
      config/      runtime-config.service (lê /assets/config.json: client_id, base URLs)
      a11y/        live-announcer, focus-manager
      layout/      shell, header, nav (SCR-22)
    shared/        page-header, empty-state, loading-state, error-state, confirm-dialog, secret-reveal (SCR-26), password-field (+medidor), field-error, copy-button, status-chip, paginator-bar
    features/
      setup/       pages/setup · services · models
      auth/        pages/{login,register,verify-email,resend-verification,forgot-password,reset-password,social-callback} · components/social-buttons · services
      account/     pages/{overview,security,sessions,confirm-email-change} · components/{reauth-dialog,password-form,email-change-form,social-links} · services · models
      admin/
        accounts/  pages/{list,detail} · components/{action-dialogs}
        applications/ · apis/ · keys/ · events/
  styles/          theme.scss (Material 3), tokens.scss (variáveis CSS), typography.scss
  assets/i18n/     pt-BR/*.json · en/*.json   ·   assets/config.json
```
Regras: componentes **standalone** e `OnPush`/signals; sem `NgModule`; rotas `loadComponent`/`loadChildren` por feature; serviços de feature não importam outra feature (compartilhar via `shared` ou `core`);
um componente por arquivo; formulários reativos tipados; nenhum componente concentra a aplicação.

### 7.1 Sessão no navegador (FT-14)
`token-store` mantém access e `expires_at` em memória; `refresh-coordinator` renova a 80 % do TTL ou em 401 (uma vez), com Web Locks e `BroadcastChannel('achernar:<client_id>')`;
na inicialização chama `/auth/refresh` (cookie) para reidratar; `auth.guard` aguarda a reidratação (estado `unknown → authenticated|anonymous`); logout limpa memória, chama `/auth/logout`, notifica abas.

## 8. Dependências (referência; **não instaladas nesta fase**)

Backend (Maven): `spring-boot-starter-web`, `-validation`, `-data-jpa`, `-security`, `-security-oauth2-resource-server`, `-security-oauth2-client`, `-mail`, `-thymeleaf`, `-actuator`, `liquibase-core`, `postgresql`, `bcprov-jdk18on` (Argon2), `micrometer-registry-prometheus`;
teste: `spring-boot-starter-test`, `spring-security-test`, Testcontainers (postgresql), GreenMail, WireMock (provedores simulados), ArchUnit.
Frontend (npm): `@angular/{core,common,router,forms,platform-browser}`, `@angular/material`, `@angular/cdk`, `@jsverse/transloco`, `rxjs`; dev: Angular CLI, Vitest, Playwright, `axe-core`/`@axe-core/playwright`, ESLint (angular-eslint), Prettier. Versões conforme D-03, fixadas na implementação.

## 9. Táticas para atributos de qualidade

| Atributo | Tática |
|---|---|
| Segurança | `security-policy.md` |
| Desempenho | Argon2id com semáforo; índices parciais; JWKS cacheável; introspecção por chave primária; paginação |
| Disponibilidade | stateless; N instâncias; jobs idempotentes; outbox tolera SMTP fora |
| Manutenibilidade | módulos por funcionalidade; testes de arquitetura; contratos em `api.md` |
| Portabilidade | Docker Compose; configuração por variáveis |
| Observabilidade | logs/métricas/health §6 |
