# Achernar Identity — Plano de Implementação (futuro; **não autorizado nesta execução**)

Fonte: Waves de `discovery/prioritization.md`. Cada etapa exige `GATE-AUT` (intenção explícita do humano) e `GATE-ANL` antes de alterar código, e `GATE-QLT` ao concluir.
Branches: `feature/<slug>` a partir de `develop` (DEV-07: criar `develop`). Commits `{{jiraKey}} - mensagem` quando houver chave (jira.yaml a corrigir, DEV-06). Nunca `git push` automático.
Estimativas são ordens de grandeza (dev-dias de 1 dev experiente) para planejamento, não compromisso.

## 1. Visão das etapas

| Etapa | Wave | Entrega | Specs/UCs | Depende de | Paralelo com | Est. |
|---|---|---|---|---|---|---:|
| E0 | 0 | Preparação do ambiente (Docker/PG18, esqueletos `backend/` e `frontend/`, CI, `technology.yaml`) | `config-changes.md` | GATE-AUT | — | 3 |
| E1 | 0 | **Fundação do backend**: parâmetros validados, erros (RFC 9457), logs com redação, migração base, `Clock`, utilitários criptográficos, eventos de segurança (FND-02), limitação de taxa (FND-03) | FND-01/02/03 | E0 | E3 | 8 |
| E2 | 0 | **Chaves e JWT** (UC-070, 071) + **e-mail/outbox** (UC-095) | FT-10 (parte), FT-13 | E1 | E3 | 7 |
| E3 | 0 | **Shell Angular**: `core`, tema M3, i18n, interceptors, guards, layout, estados globais (UC-100) | FT-14 | E0 | E1, E2 | 7 |
| E4 | 1 | **Registro e setup**: UC-060 (+061..063 mínimo), UC-064, UC-068, UC-001 (backend + SCR-01/24/25/27/28) | FT-09 (parte), FT-01 | E1, E2 | E3 | 9 |
| E5 | 1 | **Cadastro e login**: UC-010, 011, 012, 020, 021, 022, 034 (backend + SCR-02..06, 09) | FT-02, FT-03, FT-05 (parte) | E4 | — | 14 |
| E6 | 2 | **Senhas, reautenticação, e-mail, sessões**: UC-030..033, 037(prep), 035, 036, 040..042 (+ SCR-07, 08, 10..14) | FT-04, FT-05, FT-06 | E5 | E7 | 14 |
| E7 | 2 | **Controle de tokens e administração**: UC-072, 073, 061..067 completos, 080..083, 090 (+ SCR-18..21, 24..29) | FT-09, FT-10, FT-11 (parte), FT-12 (parte) | E5 | E6 | 15 |
| E8 | 3 | **Login social e vínculos**: UC-050..055, 037 (+ SCR-15..17) | FT-07, FT-08 | E5, E6 | — | 15 |
| E9 | 4 | **Fechamento**: UC-084, 091, 092; hardening (SEC), e2e completos, performance, acessibilidade, guia de integração validado, backups testados, documentação operacional | FT-11, FT-12, todos | E6, E7, E8 | — | 12 |

Caminho crítico: **E0 → E1 → E2 → E4 → E5 → E6/E7 → E8 → E9** (~74 dev-dias em série; ~55 com frontend em paralelo e duas frentes em E6/E7).

## 2. Detalhe por etapa

### E0 — Preparação do ambiente
- Aplicar `config-changes.md` (somente o necessário ao desenvolvimento: Compose base com PG18, `.env.example`, `.gitignore`); criar `backend/` (Maven wrapper, `pom.xml` com BOM Boot 4.1.x) e `frontend/` (Angular 22).
- Criar `develop`; configurar CI (build, testes, varredura de dependências).
- Atualizar `technology.yaml`/`PROJECT.yaml` (decisão registrada).
- **Saída:** `docker compose up` sobe PG e MailHog; `mvn verify` e `ng build` vazios passam; CI verde.

### E1 — Fundação do backend (FND-01/02/03)
- Tarefas: `ConfigurationProperties` + PV-01..06; `ProblemDetailsAdvice` + `ErrorCode` (catálogo); filtro de `request_id`; logging JSON + redação (CT-RNF-06); `Clock`; `TokenGenerator`/`Sha256`/`MasterKeyCipher`; Liquibase `0006`, `0008`; `SecurityEventRecorder` (TX/REQUIRES_NEW); `RateLimiter`/`LoginLockService`; testes de arquitetura (ArchUnit).
- **Saída (DoD):** CT-RNF-06, CT-SEC-37, CT-OPS-01 (parcial), unit de rate limit/lock; estrutura modular pronta.

### E2 — Chaves, JWT e e-mail
- Tarefas: `signing_key` (0005), `SigningKeyService` (estados, geração RSA 3072, cifra AES-GCM), `JwtIssuer`, `JwksController`, metadados, `KeyRotationJob`; `email_outbox` (0007), `TemplateRenderer` (pt-BR/en, TPL-01..11 como arquivos), `OutboxWorker`, `SmtpMailSender`.
- **Saída:** FT-10 (UC-070, 071) e FT-13 com CT definidos passando; CT-CON-10, CT-CON-12.

### E3 — Shell Angular (paralelo)
- Tarefas: workspace, tema M3 + tokens, `ThemeService`, Transloco, `runtime-config`, `api-client` (snake↔camel, problem), interceptors (bearer, refresh-on-401, erro), guards, layout/shell, componentes `shared`, `refresh-coordinator` (Web Locks/BroadcastChannel) contra **mock** da API.
- **Saída:** FT-14 CT (tema, i18n, a11y base, sessão em memória); Storybook/galeria opcional.

### E4 — Registro de aplicações/APIs e setup
- Tarefas: tabelas 0002; entidades/serviços de registro; `ClientAuthResolver`/`DynamicCorsConfigurationSource`; UC-060 (serviço + endpoint), UC-064, UC-068; semeadura da aplicação da UI; `SetupController` (UC-001) com bootstrap; telas SCR-01, 24, 25, 26, 27, 28 (versão funcional mínima).
- **Saída:** CT-FT01-*, CT-FT09 (parte), CT-SEC-26, CT-E2E-01 (parcial: sem login ainda → usa token de teste).

### E5 — Cadastro e login (caminho crítico)
- Tarefas: tabelas 0001, 0003; `RegistrationService`, verificação/reenvio, `LoginService`, `RefreshService` (D-43), `LogoutService`, `SessionService` (criar/revogar/limitar), `PasswordHasher` + semáforo, `PasswordPolicy`; chain Resource Server do Identity (`/api/v1/**` + validador `sid`); UC-034; telas SCR-02..06, 09; integração do `refresh-coordinator` real.
- **Saída:** FT-02, FT-03, parte de FT-05 com CT passando; CT-CON-01..05,08; CT-SEC-02,06,07,14; CT-E2E-02..05; CT-INT-01..03.

### E6 — Autosserviço
- Tarefas: recuperação/redefinição, alteração de senha, reautenticação, troca de e-mail, listagem/revogação/encerramento de sessões; TPL-06/07/11; telas SCR-07, 08, 10..14.
- **Saída:** FT-04 (sem UC-037 social), FT-05, FT-06; CT-E2E-06..08; CT-CON-05,11,13.

### E7 — Controle de tokens e administração
- Tarefas: introspecção (UC-073) + credenciais de API (UC-067); chaves manuais/emergencial (UC-072); gestão completa de aplicações/APIs (UC-061..067); administração de contas (UC-080..083) com trava do último admin; consulta de eventos (UC-090); telas SCR-18..21, 24..29.
- **Saída:** FT-09, FT-10, FT-11 (sem UC-084), FT-12 (UC-090); CT-CON-07,09; CT-INT-04..07; CT-E2E-11,12.

### E8 — Login social
- Tarefas: tabela 0004 (social); `SocialProviderClient` Google/Facebook; `SocialTransactionService`; `ExchangeService`; resolução de conta; `LINK`/`REAUTH`; UC-037; telas SCR-15..17; simulador de provedor para testes.
- **Saída:** FT-07, FT-08 e UC-037 com CT passando; CT-CON-06; CT-E2E-09,10; roteiro manual com contas reais.

### E9 — Fechamento
- Tarefas: UC-084 (anonimização), jobs de retenção/manutenção, hardening e revisão de SEC-01..40, performance (CT-PERF-*), a11y completa, e2e em 3 navegadores, validação do guia de integração (API de referência), ensaio de backup/restore (CT-OPS-*), documentação final e `deployment-operations` no Compose `prod`.
- **Saída:** todos os CT de todos os FT + transversais com PASS; checklists de §7 de `deployment-operations.md` atendidos; `GATE-CLS`.

## 3. Estratégia de entrega

1. Cada etapa = uma ou mais branches `feature/...` com PR; revisão por `agents/review` e verificação de `GATE-QLT` (testes + findings) antes de seguir.
2. Backend e frontend por etapa seguem em paralelo **após** o contrato (`api.md`) estar fixo; frontend usa mock até o backend existir.
3. Definition of Done geral: critérios de aceite (`CA-*`) da etapa verificados por testes automatizados; rastreabilidade atualizada; sem segredos; logs sem dados sensíveis; migrações versionadas; documentação de API (OpenAPI gerado, validado contra `api.md`).
4. Riscos de sequenciamento: ver `prioritization.md`. Mitigações: simulador de provedor social desde E2; serviço de revogação de sessões entregue em E5.
5. Primeira candidata a implementação (se autorizada): **E0 → E1**.
