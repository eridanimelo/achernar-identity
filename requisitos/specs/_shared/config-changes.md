# Achernar Identity — Mudanças Futuras de Configuração e Ambiente (não executadas)

Esta execução limitou-se a documentação (§15). Itens abaixo devem ser aplicados **na etapa de implementação**, sob `GATE-AUT`, em branch própria a partir de `develop`
(DEV-07: `develop` ainda não existe — criar a partir de `master`).

## 1. `.ia/config/technology.yaml` (motor do framework)

| Chave atual | Valor atual | Novo valor | Motivo |
|---|---|---|---|
| `technology.backend` | `java-system-spring-boot-4` | manter | — |
| `details.java_version` | `"27"` | `"25"` | D-03/DEV-02 |
| `details.spring_boot` | `"4"` | `"4.1"` | D-03 |
| `details.spring_authorization_server` | `true` | **remover/false** | D-02/DEV-01 |
| `details.authorization_server_jdbc_components` | `true` | **remover/false** | D-02 |
| `details.angular` | `"22"` | `"22"` (manter; Node 24 LTS) | D-03 |
| `details.frontend_styling` | `angular-material` | manter | D-03/DEV-04 |
| `details.mailhog_local` | `true` | manter (ou `mailpit_local`) | opcional |
| (novos) | — | `postgresql_version: "18"`, `node_version: "24"`, `i18n_library: transloco`, `argon2: true`, `social_login: [google, facebook]` | D-03, D-15 |
| `details.production_email_provider` | `pending` | manter `pending` (parâmetro de implantação) | A-02 |

## 2. `.ia/config/PROJECT.yaml`

`project.description` hoje diz “autenticação centralizada e **SSO** entre aplicações, baseado em OAuth2 e OpenID Connect” ⇒ substituir por: “Serviço de identidade reutilizável por projeto
(contas, autenticação e sessões) com JWT assimétrico, JWKS e login social; sem SSO entre aplicações independentes.” (DEV-03).

## 3. Outros arquivos de configuração do framework

| Arquivo | Observação |
|---|---|
| `.ia/config/jira.yaml` | `project.key: TS1506CS` (TSE-CSADM-CERES-SOFTWARE) não corresponde a este produto: definir o projeto Jira correto antes de criar issues (DEV-06). Nenhuma issue foi criada. |
| `.ia/config/git.yaml` | `baseBranch: develop` inexistente (DEV-07). |
| `.ia/config/paths.yaml` | sem mudanças (`requisitos/`, `backend`, `frontend`, `docker`). |

## 4. Repositório

| Item | Mudança futura |
|---|---|
| `README.md` | (atualizado nesta execução apenas na descrição e na seção de estado; ver abaixo) |
| `docker/docker-compose.yml` | itens de `deployment-operations.md §3.2` |
| `docker/docker-compose.prod.yml`, `docker/caddy/Caddyfile`, `docker/initdb/01-roles.sql`, `docker/secrets/` | criar (§3.1) |
| `docker/.env.example` | substituir pelo conteúdo de `deployment-operations.md §4` |
| `.gitignore` | adicionar `/docker/secrets/*` (exceto `.gitkeep`), `*.pem`, `*.p12` |
| `backend/` | projeto Maven (`pom.xml`, Dockerfile, `src/main/resources/db/changelog/**`, templates de e-mail, `common-passwords.txt`) |
| `frontend/` | workspace Angular 22 (`angular.json`, `package.json`, Dockerfile, `assets/config.json`, `assets/i18n/**`) |
| CI (a definir) | build + testes + varredura de dependências + imagens |

## 5. Ambiente do desenvolvedor

JDK 25, Maven 3.9+, Node 24 LTS (≥ 24.15), Docker Compose v2, `openssl` (geração de segredos). Variáveis de `.env`/`application-dev` conforme §4 de `deployment-operations.md`.
