# Achernar Identity

Serviço de **autenticação reutilizável, instalado por projeto**, para aplicações próprias:
cadastro e login (e-mail/senha, Google e Facebook), tokens JWT validáveis por JWKS,
refresh tokens rotativos, sessões, recuperação de senha, troca de e-mail,
administração de contas, registro de aplicações e APIs consumidoras, eventos de
segurança, e-mails transacionais e interface própria (Angular).

Organização: Eridani · Versão do projeto: 1.0.0

> **Estado atual:** implementação **concluída na branch `feature/implementacao-achernar-identity`**
> (backend, frontend e testes). O frontend foi compilado, analisado (ESLint) e testado (62 testes). O
> **backend ainda precisa ser compilado e verificado com Maven/Docker** (`mvn verify`): o ambiente em que foi
> escrito não tinha acesso a Maven nem a Docker — ver `requisitos/specs/_shared/implementation-decisions.md` §4.

Fora do escopo: SSO entre aplicações, alternativa ao Keycloak/OIDC completo,
multi-tenancy, federação de identidades, Helm/Kubernetes.

---

## 1. Visão técnica (alvo)

Fonte de decisões: `requisitos/specs/_shared/decisions.md` (D-01…D-51). Resumo:

| Camada | Alvo |
|---|---|
| Backend | Java 25 LTS · Spring Boot 4.1.x · Spring Security 7 · monólito modular · Liquibase |
| Frontend | Angular 22 (standalone) · Angular Material 3 · i18n pt-BR/en |
| Banco | PostgreSQL 18 (estado, limites, eventos e fila de e-mail no próprio banco; sem Redis) |
| Tokens | JWT RS256 + JWKS; refresh opaco rotativo; cookie HttpOnly para navegadores |
| Execução | Docker Compose (dev/prod), proxy com TLS; e-mail local por servidor de captura |

As versões são **alvo de planejamento** e serão revalidadas na primeira etapa de
implementação. Divergências entre este alvo e `.ia/config/*.yaml` estão registradas
(DEV-01…07 em `decisions.md` e `config-changes.md`); os arquivos de `.ia/config`
ainda **não** foram atualizados.

## 2. Estrutura do repositório

```text
achernar-identity/
├── README.md
├── .ia/             ← motor do framework de trabalho (abrir a sessão do agente escopada aqui)
├── requisitos/
│   ├── discovery/   ← sistema, módulos, casos de uso (47), dependências, priorização/Waves
│   ├── specs/
│   │   ├── _shared/ ← decisões, parâmetros, regras, catálogos, modelo de dados, segurança,
│   │   │              tokens, login social, e-mails, telas, operação, testes, plano, rastreabilidade
│   │   └── FT-01 … FT-14/ ← 8 arquivos por feature (business, api, database, frontend,
│   │                         security, tests, status.yaml, context-manifest.yaml)
│   ├── reviews/     ← completeness-check.md
│   ├── state/       ← current.yaml (retomada de contexto)
│   └── tools/       ← verify_specs.py (ferramenta de documentação: verifica IDs e gera a matriz)
├── docker/          ← compose de desenvolvimento (PG18 + Mailpit) e de produção de referência (Caddy), .env.example
├── backend/         ← API (Java 25 · Spring Boot 4.1 · JDBC · Liquibase) + Dockerfile
├── frontend/        ← UI (Angular 22 · Material · Transloco · Vitest) + Dockerfile/nginx
└── .github/         ← CI (build e testes)
```

Ponto de entrada da documentação: `requisitos/specs/_shared/index.md`.

## 3. Ordem de implementação proposta (quando autorizada)

Detalhe em `requisitos/specs/_shared/implementation-plan.md`:
E0 ambiente → E1 fundação do backend → E2 chaves/JWT e e-mail → E3 shell Angular (em paralelo) →
E4 registro de aplicações/APIs e setup → E5 cadastro e login → E6 senhas, e-mail, sessões →
E7 controle de tokens e administração → E8 login social → E9 fechamento (retenção, hardening, e2e).

## 4. Como trabalhar neste repositório

1. Abra a sessão do agente **escopada em `.ia/`**.
2. O orquestrador classifica a solicitação em uma intenção e monta o contexto mínimo.
3. Implementação de qualquer Spec exige **intenção explícita + GATE-AUT**; este README não é autorização.
4. Regras de Git em `.ia/config/git.yaml`: **nunca `git push`**; trabalhar em branch própria.
5. Para validar a documentação: `python3 requisitos/tools/verify_specs.py` (regenera `traceability.md`).

## 5. Pendências do operador (antes de produção)

Provedor SMTP, domínios/TLS, apps OAuth Google/Facebook, logotipo, geração da
chave-mestra e do segredo de bootstrap e janela de manutenção (A-02…A-07 em
`requisitos/discovery/system.md`). Nenhuma credencial real deve entrar no repositório.

---

## 3. Desenvolvimento local

Pré-requisitos: JDK 25, Maven 3.9+, **Node 24** (Angular 22 não roda em Node 22), Docker.

```bash
docker compose -f docker/docker-compose.yml up -d        # PostgreSQL 18 + Mailpit (http://localhost:8025)

cd backend
ACHERNAR_DEV_INSECURE_KEY=true ACHERNAR_BOOTSTRAP_TOKEN=dev-bootstrap-token-0123456789-0123456789 \
  ACHERNAR_DB_PASSWORD=achernar_identity mvn spring-boot:run     # http://localhost:8080 (perfil dev)

cd frontend
npm ci && npm start                                       # http://localhost:4200 (proxy → :8080)
```

Sem administrador, a UI leva a `/setup`; informe o token de bootstrap acima. `ACHERNAR_DEV_INSECURE_KEY=true`
existe só para desenvolvimento (em produção a chave mestra real é obrigatória).

### Testes

```bash
cd backend  && mvn verify          # unitários (*Test), arquitetura (ArchUnit) e integração (*IT; Testcontainers → Docker)
cd frontend && npm run verify      # i18n:check + lint + Vitest + build
```

### Produção (referência)

```bash
cp docker/.env.example docker/.env   # ACHERNAR_MASTER_KEY (openssl rand -base64 32), SMTP, host…
docker compose -f docker/docker-compose.prod.yml --env-file docker/.env up -d --build
```

O Caddy termina TLS e serve UI e API na mesma origem (cookie de refresh `HttpOnly; SameSite=Strict`, sem CORS).
O Actuator (9090) não é publicado. Remova `ACHERNAR_BOOTSTRAP_TOKEN` depois de criar o primeiro administrador.
