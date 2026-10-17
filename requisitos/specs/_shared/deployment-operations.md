# Achernar Identity — Configuração, Implantação e Operação

Documento técnico. **Nada aqui foi executado nesta fase** (§15): `docker/` permanece como está; as mudanças futuras estão descritas aqui e em `config-changes.md`.
Decisões D-34, D-35; parâmetros `parameters.md`; segurança `security-policy.md`.

## 1. Estado atual do ambiente (lido do repositório)

- `docker/docker-compose.yml`: serviços `postgres` (imagem `postgres:16-alpine`, volume `achernar_identity_pgdata`, healthcheck `pg_isready`) e `mailhog` (`mailhog/mailhog:latest`, SMTP 1025 / UI 8025). Backend e frontend rodam **fora** do Compose.
- `docker/.env.example`: `POSTGRES_*` e portas do MailHog.
- Não existem `backend/`, `frontend/` nem Dockerfiles.

## 2. Ambientes

| Ambiente | Perfil | Composição |
|---|---|---|
| Desenvolvimento local | `dev` | Compose: PostgreSQL + MailHog (existente). Backend (`mvn spring-boot:run`) e frontend (`ng serve`, proxy para a API) no host. `ACHERNAR_COOKIE_SECURE=false`, issuer `http://localhost:8080`, UI em `http://localhost:4200` (mesmo site ⇒ cookies funcionam). |
| CI | `test` | Testcontainers (PostgreSQL), GreenMail e WireMock; sem Compose. |
| Produção / homologação | `prod` | Compose com perfil `prod`: PostgreSQL, API, UI estática, proxy Caddy (HTTPS), backup. |

## 3. Mudanças futuras no Docker (documentadas, **não executadas**)

### 3.1 Estrutura-alvo de `docker/`

```text
docker/
  docker-compose.yml            # base: postgres, mailhog (dev) — atualizado
  docker-compose.prod.yml       # sobreposição: api, ui, proxy, backup (sem mailhog)
  .env.example                  # variáveis (placeholders)
  secrets/.gitkeep              # arquivos de segredo (ignorados pelo git)
  caddy/Caddyfile
  initdb/01-roles.sql           # papéis migrator/app e privilégios (SEC-30)
backend/Dockerfile · frontend/Dockerfile
```

### 3.2 Alterações no Compose base

| Item | Hoje | Alvo | Motivo |
|---|---|---|---|
| PostgreSQL | `postgres:16-alpine` | `postgres:18-alpine` | D-03 (migração de dados de dev: recriar volume ou `pg_upgrade`) |
| MailHog | `mailhog/mailhog:latest` | manter **ou** `axllent/mailpit` (mesmas portas) | MailHog sem manutenção; tag fixa em vez de `latest` |
| Redes | padrão | rede `achernar` interna | isolamento |
| Perfis | — | `dev` (padrão) e `prod` | separar mailhog do prod |
| Healthchecks | postgres, mailhog | + api (`/actuator/health/readiness` na 9090), ui | `depends_on: condition: service_healthy` |
| Limites | — | `mem_limit`, `cpus`, `pids_limit`, `read_only` onde possível | RNF-03 |
| Segredos | variáveis em `.env` | `*_FILE` apontando `docker/secrets/*` (ou secrets do orquestrador) | SEC-24 |
| `POSTGRES_PASSWORD` | padrão `achernar_identity` | **sem padrão no perfil prod** (obrigatório) | nada de credenciais fixas |

### 3.3 Esboço do perfil `prod` (ilustrativo)

```yaml
# docker-compose.prod.yml (SOBREPOSIÇÃO; ilustrativo — não aplicado)
services:
  api:
    build: { context: ../backend }
    profiles: ["prod"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      ACHERNAR_ISSUER: ${ACHERNAR_ISSUER:?obrigatório}
      ACHERNAR_DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      ACHERNAR_DB_USER_FILE: /run/secrets/db_app_user
      ACHERNAR_DB_PASSWORD_FILE: /run/secrets/db_app_password
      ACHERNAR_MASTER_KEY_FILE: /run/secrets/master_key
      ACHERNAR_BOOTSTRAP_TOKEN_FILE: /run/secrets/bootstrap_token
      ACHERNAR_SMTP_HOST: ${ACHERNAR_SMTP_HOST:?obrigatório}
      ACHERNAR_TRUSTED_PROXIES: 172.20.0.0/16
    secrets: [db_app_user, db_app_password, master_key, bootstrap_token, smtp_password, google_client_secret, facebook_app_secret]
    depends_on: { postgres: { condition: service_healthy } }
    healthcheck: { test: ["CMD", "wget", "-qO-", "http://localhost:9090/actuator/health/readiness"], interval: 15s, timeout: 5s, retries: 5, start_period: 40s }
    user: "10001:10001"
    read_only: true
    tmpfs: [/tmp]
  ui:
    build: { context: ../frontend }
    profiles: ["prod"]
  proxy:
    image: caddy:2
    profiles: ["prod"]
    ports: ["80:80", "443:443"]
    volumes: ["./caddy/Caddyfile:/etc/caddy/Caddyfile:ro", "caddy_data:/data"]
    depends_on: [api, ui]
  backup:
    image: postgres:18-alpine
    profiles: ["prod"]
    # job diário pg_dump → volume/objeto externo (ver migrations-backup.md)
secrets:
  db_app_user: { file: ./secrets/db_app_user }
  # …demais segredos como arquivos fora do git
```

### 3.4 Dockerfiles (requisitos)

- **Backend:** multi-estágio — build com Maven + JDK 25; runtime JRE 25 mínima (ex.: `eclipse-temurin:25-jre`), usuário não-root (UID 10001), `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`, fuso UTC, sem shell desnecessário, `ENTRYPOINT` direto no JAR em camadas; porta 8080 (API) e 9090 (gestão).
- **Frontend:** build com Node 24 LTS (`npm ci`, `ng build`), artefatos estáticos copiados para a imagem do proxy ou servidos por Nginx sem privilégios; `config.json` de runtime montado/gerado no deploy; cabeçalhos CSP no proxy.
- Imagens com tag fixa (digest quando possível); varredura de vulnerabilidades no CI (SEC-36).

### 3.5 Caddy (esboço)

```text
{$ACHERNAR_HOST} {
  encode zstd gzip
  header { Strict-Transport-Security "max-age=31536000; includeSubDomains"  X-Content-Type-Options nosniff  Referrer-Policy no-referrer  -Server }
  @api path /auth/* /api/* /setup/* /.well-known/*
  handle @api { reverse_proxy api:8080 { header_up X-Forwarded-For {remote_host} } }
  @admin path /api/v1/admin/*          # opcional: restringir por IP/VPN
  handle { root * /srv/ui ; try_files {path} /index.html ; file_server
           header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'; object-src 'none'" }
}
```
O proxy **descarta** `X-Forwarded-*` recebidos do cliente e define os seus (SEC-22/P-47); a API confia apenas nos proxies de `ACHERNAR_TRUSTED_PROXIES`.

## 4. Variáveis de ambiente

Catálogo completo em `parameters.md`. `.env.example` futuro (placeholders, **sem valores reais**):

```dotenv
# --- PostgreSQL ---
POSTGRES_DB=achernar_identity
POSTGRES_USER=achernar_owner          # papel dono/migrador
POSTGRES_PASSWORD=                    # OBRIGATÓRIO em prod (sem padrão)
POSTGRES_PORT=5432
# --- Identity (dev) ---
SPRING_PROFILES_ACTIVE=dev
ACHERNAR_ISSUER=http://localhost:8080
ACHERNAR_UI_BASE_URL=http://localhost:4200
ACHERNAR_UI_ORIGINS=http://localhost:4200
ACHERNAR_COOKIE_SECURE=false
ACHERNAR_SMTP_HOST=localhost
ACHERNAR_SMTP_PORT=1025
ACHERNAR_MAIL_FROM=no-reply@achernar.local
ACHERNAR_MASTER_KEY=                  # gerar: openssl rand -base64 32
ACHERNAR_BOOTSTRAP_TOKEN=             # gerar: openssl rand -base64 32 (≥ 32 chars)
# Login social (opcional): preencher para habilitar
ACHERNAR_GOOGLE_CLIENT_ID=
ACHERNAR_GOOGLE_CLIENT_SECRET=
ACHERNAR_FACEBOOK_APP_ID=
ACHERNAR_FACEBOOK_APP_SECRET=
ACHERNAR_FACEBOOK_GRAPH_VERSION=
# --- Compose / MailHog ---
MAILHOG_SMTP_PORT=1025
MAILHOG_UI_PORT=8025
```

Convenção `*_FILE`: se `VAR_FILE` existir, o valor é lido do arquivo (conteúdo aparado) — vale para todos os segredos (`MASTER_KEY`, `BOOTSTRAP_TOKEN`, `DB_PASSWORD`, `SMTP_PASSWORD`, segredos sociais).

## 5. Validações de subida (fail-fast)

1. Parâmetros dentro dos limites (PV-01…PV-06).
2. `prod`: `ACHERNAR_ISSUER` https; `ACHERNAR_COOKIE_SECURE=true`; `ACHERNAR_MASTER_KEY` ≥ 32 bytes; SMTP definido; sem senha de banco padrão.
3. Banco acessível; Liquibase aplicado; Hibernate `validate` ok.
4. Existe chave `ACTIVE` (gera K0 se não houver).
5. Se não há admin ativo: exige `ACHERNAR_BOOTSTRAP_TOKEN` (≥ 32) em `prod`; senão aviso e setup indisponível (RN-SET-04).
6. Semeia a aplicação `achernar-identity-ui` se ausente (RN-SET-05).
7. Registra no log (sem segredos) o resumo de configuração efetiva: perfil, issuer, provedores sociais habilitados, SMTP host, TTLs.

## 6. Operação

### 6.1 Monitoramento e alertas

| Sinal | Limiar sugerido | Ação |
|---|---|---|
| readiness ≠ UP | > 1 min | verificar banco/chave |
| `achernar_refresh_reuse_total{kind="detected"}` | > 5 em 10 min | investigar roubo de token |
| `achernar_security_alert_total{type="ACCOUNT_LOCKED_THROTTLE"}` | pico > 20/10 min | possível ataque; considerar bloqueio de IP no proxy |
| `achernar_mail_outbox_pending` | > 50 por > 15 min | verificar SMTP |
| `EMAIL_DELIVERY_FAILED` | qualquer | revisar remetente/credenciais |
| `achernar_signing_key_age_seconds` | > P-10 + 7 d | rotação falhou |
| Latência p95 login | > 1,2 s por 10 min | revisar custo Argon2/CPU |
| Erros 5xx | > 1 % por 5 min | investigar logs (`trace_id`) |

### 6.2 Runbooks

| Procedimento | Passos resumidos |
|---|---|
| **Primeira instalação** | preencher `.env`/segredos → `docker compose --profile prod up -d` → acessar `/setup` com o bootstrap token → criar admin → remover `BOOTSTRAP_TOKEN` → cadastrar APIs e aplicações → configurar provedores sociais → testar fluxo ponta a ponta |
| **Atualização de versão** | backup → `pull`/build → subir nova versão (Liquibase aplica) → verificar `readiness` → smoke tests; rollback = restaurar imagem anterior **se** migrações forem compatíveis (expandir/contrair) ou restaurar backup (ver `migrations-backup.md`) |
| **Chave de assinatura comprometida** | `/admin/keys` ⇒ remoção emergencial (+ opcional revogar sessões); avisar equipes das APIs para renovarem JWKS (≤ 5 min); revisar eventos |
| **Rotacionar `client_secret`/credencial de API** | gerar novo (UC-063/067) → atualizar o consumidor → revogar o antigo |
| **Perda da chave-mestra** | chaves privadas e payloads do outbox ficam ilegíveis ⇒ nova chave-mestra, apagar `signing_key` privada inválida (o job gera nova), **todas as sessões** passam a falhar na assinatura ⇒ usuários refazem login |
| **Troca da chave-mestra (rotina)** | procedimento de re-cifragem (job administrativo na implementação) com período de duas chaves; até lá, trocar = tratar como perda |
| **Perda de todos os admins** | com `ACHERNAR_BOOTSTRAP_TOKEN` definido e nenhum admin `ACTIVE`, `/setup` volta a funcionar (D-24) |
| **Troca de provedor SMTP** | atualizar variáveis → reiniciar → conferir `outbox` |
| **Habilitar login social** | criar app no provedor → registrar URL de retorno do Identity → definir variáveis → reiniciar → habilitar por aplicação |
| **Escala horizontal** | subir N instâncias iguais atrás do proxy; sem sticky session; jobs com trava consultiva |
| **Pós-restauração** | revogar **todas** as sessões; verificar chaves; limpar `rate_limit_counter` (runbook em `migrations-backup.md`) |

### 6.3 Capacidade de referência

2 vCPU / 2 GiB para a API + PostgreSQL 1 vCPU / 1 GiB atende ~50 req/s de login/refresh combinados e 10⁵ contas. Argon2id (19 MiB × concorrência P-79) domina memória em picos de login: dimensionar `ACHERNAR_HASH_CONCURRENCY × 19 MiB`.

### 6.4 Logs

Saída padrão em JSON; coletar com o driver do Docker/agente do ambiente; retenção de logs ≥ 30 dias (ajustável); nunca habilitar `DEBUG` com corpos de requisição em produção.

## 7. Checklist de entrada em produção

☐ DNS e certificado válidos (Caddy) · ☐ `ACHERNAR_ISSUER` definitivo (muda o `iss`: **não alterar depois** sem coordenar APIs) · ☐ SMTP/SPF/DKIM/DMARC
☐ segredos fora do git; `BOOTSTRAP_TOKEN` removido após o setup · ☐ backups automáticos testados · ☐ proxy descarta `X-Forwarded-*` externos
☐ porta 9090 não exposta · ☐ rota `/api/v1/admin/*` restrita por rede (recomendado) · ☐ provedores sociais em modo “live” · ☐ monitoração e alertas ativos
☐ APIs consumidoras testadas com `consumer-integration.md §10`
