# Achernar Identity — Catálogo de Parâmetros Configuráveis (P-xx)

Todo valor numérico de política tem **padrão** e **limites**. Parâmetros são lidos de variáveis de ambiente (prefixo
`ACHERNAR_`), validados na subida (fail-fast em `prod`) e **nunca** contêm segredos reais no repositório. Durações em ISO-8601
(`PT10M`, `P7D`). “Aplicação” = pode ser sobrescrito por aplicação dentro dos limites (ver `FT-09`).

## 1. Tokens e sessões

| ID | Variável | Padrão | Limites | Descrição | Origem |
|---|---|---|---|---|---|
| P-01 | `ACHERNAR_TOKEN_ACCESS_TTL` | `PT10M` | `PT5M`–`PT15M` (aplicação) | validade do access token | D-05 |
| P-02 | `ACHERNAR_SESSION_REFRESH_TTL` | `P7D` | `PT1H`–`P30D` | validade deslizante do refresh | D-06 |
| P-03 | `ACHERNAR_SESSION_MAX_LIFETIME` | `P30D` | ≥ P-02, ≤ `P90D` | teto absoluto da sessão | D-06 |
| P-04 | `ACHERNAR_REFRESH_REUSE_GRACE` | `PT10S` | `PT0S`–`PT30S` | tolerância a reuso do antecessor | D-06 |
| P-05 | `ACHERNAR_SESSION_MAX_ACTIVE` | `10` | 1–50 | sessões ativas por conta | D-44 |
| P-06 | `ACHERNAR_CLOCK_SKEW` | `PT60S` | `PT0S`–`PT120S` | tolerância de relógio | D-42 |
| P-07 | `ACHERNAR_REAUTH_WINDOW` | `PT5M` | `PT1M`–`PT15M` | validade da reautenticação | D-22 |
| P-08 | `ACHERNAR_SOCIAL_EXCHANGE_CODE_TTL` | `PT60S` | `PT10S`–`PT120S` | código de troca social | D-19 |
| P-09 | `ACHERNAR_SOCIAL_TRANSACTION_TTL` | `PT10M` | `PT2M`–`PT15M` | transação de login social | D-19 |

## 2. Chaves de assinatura

| ID | Variável | Padrão | Limites | Descrição |
|---|---|---|---|---|
| P-10 | `ACHERNAR_KEY_ROTATION_INTERVAL` | `P90D` | `P30D`–`P365D` | intervalo de rotação automática |
| P-11 | `ACHERNAR_KEY_PUBLISH_LEAD` | `PT24H` | `PT1H`–`P7D` | antecedência de publicação antes de assinar |
| P-12 | `ACHERNAR_KEY_RETIRED_RETENTION` | `PT48H` | ≥ P-01 + P-06 + P-14 | permanência no JWKS após deixar de assinar |
| P-13 | `ACHERNAR_KEY_RSA_BITS` | `3072` | 3072 ou 4096 | tamanho da chave RSA |
| P-14 | `ACHERNAR_JWKS_CACHE_MAX_AGE` | `PT5M` | `PT1M`–`PT1H` | `Cache-Control: max-age` do JWKS |

## 3. Senhas

| ID | Variável | Padrão | Limites | Descrição |
|---|---|---|---|---|
| P-15 | `ACHERNAR_PASSWORD_MIN_LENGTH` | `12` | 12–64 | mínimo de caracteres |
| P-17 | `ACHERNAR_PASSWORD_MAX_LENGTH` | `128` | 64–256 | máximo de caracteres |
| P-16 | `ACHERNAR_ARGON2_MEMORY_KIB` / `_ITERATIONS` / `_PARALLELISM` | `19456` / `2` / `1` | memória ≥ 19456; iterações ≥ 2 | custo do Argon2id (alvo ≈ 100–300 ms por hash na máquina de produção) |
| P-18 | (arquivo embutido) `common-passwords.txt` | ≥ 10.000 entradas | — | lista de senhas comuns |

## 4. Tokens de uso único

| ID | Variável | Padrão | Limites | Descrição |
|---|---|---|---|---|
| P-20 | `ACHERNAR_VERIFY_EMAIL_TTL` | `PT24H` | `PT1H`–`P7D` | verificação de e-mail |
| P-21 | `ACHERNAR_PASSWORD_RESET_TTL` | `PT30M` | `PT10M`–`PT2H` | recuperação de senha |
| P-22 | `ACHERNAR_EMAIL_CHANGE_TTL` | `PT1H` | `PT10M`–`PT24H` | troca de e-mail |
| P-23 | (constante) | 32 bytes | fixo | entropia (256 bits) |

## 5. Limites de taxa e bloqueios (janela fixa; resposta `429` + `Retry-After`)

| ID | Escopo | Padrão | Observação |
|---|---|---|---|
| P-30 | falhas de login por e-mail normalizado | 5 falhas / 15 min ⇒ bloqueio de 15 min; reincidência em 24 h dobra (30, 60, 120 min; teto 120) | resposta idêntica a credencial inválida; zera após 24 h sem bloqueio ou após recuperação de senha |
| P-31 | tentativas de login por IP | 20 / min e 200 / h | `429` |
| P-32 | cadastro | 10 / h por IP; 3 / h por e-mail | por e-mail: silencioso (202) |
| P-33 | recuperação de senha | 3 / h por e-mail; 10 / h por IP | silencioso (202) |
| P-34 | reenvio de verificação | 1 / 60 s e 5 / h por e-mail; 10 / h por IP | silencioso (202) |
| P-35 | solicitação de troca de e-mail | 3 / dia por conta | `429` |
| P-36 | refresh | 30 / min por sessão; 120 / min por IP | `429` |
| P-37 | início de login social | 30 / min por IP | `429` |
| P-38 | setup do primeiro admin | 5 / 15 min por IP | `429` |
| P-39 | reautenticação / troca de senha (senha atual) | compartilha P-30 por conta; 10 / 15 min por sessão | `429` |
| P-45 | introspecção | 600 / min por API | `429` |
| P-46 | cache de introspecção na API consumidora (recomendado) | ≤ 30 s | obrigação documentada |
| P-47 | proxies confiáveis (`ACHERNAR_TRUSTED_PROXIES`) | lista CIDR; vazio = só conexão direta | origem do IP real |
| P-48 | tamanho máx. de corpo | 16 KiB (JSON) | `413` |
| P-49 | `ACHERNAR_CORS_MAX_AGE` | `PT10M` | pré-voo |

## 6. E-mail

| ID | Variável | Padrão | Descrição |
|---|---|---|---|
| P-40 | `ACHERNAR_MAIL_MAX_ATTEMPTS` / backoff | `6` tentativas; 1 min, 5 min, 15 min, 1 h, 6 h | outbox |
| P-41 | `ACHERNAR_MAIL_FAILED_RETENTION` | `P7D` | linhas `FAILED` mantidas (sem payload) |
| P-42 | `ACHERNAR_SMTP_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD`, `_STARTTLS` (`true`), `_AUTH` | `localhost:1025` em `dev` | transporte; **sem padrão em prod** |
| P-43 | `ACHERNAR_MAIL_FROM`, `ACHERNAR_MAIL_FROM_NAME`, `ACHERNAR_MAIL_REPLY_TO` | sem padrão em prod | remetente |
| P-58 | `ACHERNAR_MAIL_WORKER_INTERVAL` / `_BATCH` | `PT10S` / `20` | frequência e lote do worker |

## 7. Retenção

| ID | Variável | Padrão | Descrição |
|---|---|---|---|
| P-50 | `ACHERNAR_RETENTION_UNVERIFIED_ACCOUNT` | `P7D` | remove contas `PENDING_VERIFICATION` sem verificação |
| P-51 | `ACHERNAR_RETENTION_ONE_TIME_TOKENS` | `P7D` após expirar/consumir | purga de tokens de uso único, transações e códigos sociais |
| P-52 | `ACHERNAR_RETENTION_SESSIONS` | `P30D` após fim da sessão | purga de sessões e refresh tokens encerrados |
| P-53 | `ACHERNAR_RETENTION_SECURITY_EVENTS` | `P365D` | purga de eventos |
| P-54 | `ACHERNAR_RETENTION_EVENT_IP` | `P90D` | IP/UA anulados após o prazo |
| P-55 | `ACHERNAR_RETENTION_OUTBOX_SENT` | `P3D` | linhas `SENT` (metadados) |
| P-56 | `ACHERNAR_RETENTION_RATE_LIMIT` | `P1D` | contadores |
| P-59 | `ACHERNAR_RETENTION_RETIRED_KEYS` | `P30D` | remoção física de chaves `REMOVED` |
| P-57 | paginação | padrão 25, máx. 100 | listagens administrativas |

## 8. URLs, origens e identidade do serviço

| ID | Variável | Exemplo (não real) | Descrição |
|---|---|---|---|
| P-60 | `ACHERNAR_ISSUER` | `https://id.exemplo.com.br` | valor do `iss`; URL pública base do Identity; HTTPS obrigatório em `prod` |
| P-61 | `ACHERNAR_UI_BASE_URL` | `https://id.exemplo.com.br/app` | base da UI própria (links de fallback) |
| P-62 | `ACHERNAR_UI_ORIGINS` | `https://id.exemplo.com.br` | origens da UI própria (CORS) |
| P-63 | `ACHERNAR_UI_AUDIENCE` | `achernar-identity-api` | audiência da API do Identity (fixa por instalação) |
| P-64 | `ACHERNAR_DEFAULT_LOCALE` | `pt-BR` | idioma padrão |

## 9. Bootstrap, segredos e provedores

| ID | Variável | Regra |
|---|---|---|
| P-65 | `ACHERNAR_BOOTSTRAP_TOKEN` | ≥ 32 caracteres aleatórios; obrigatório apenas enquanto não houver admin ativo; fail-fast em `prod` se faltar e não houver admin |
| P-66 | `ACHERNAR_MASTER_KEY` | ≥ 32 bytes (base64); cifra chaves privadas e payload do outbox; obrigatório em `prod`; nunca em banco/repositório |
| P-70 | `ACHERNAR_GOOGLE_CLIENT_ID`, `ACHERNAR_GOOGLE_CLIENT_SECRET` | ambos presentes ⇒ Google habilitado globalmente |
| P-71 | `ACHERNAR_FACEBOOK_APP_ID`, `ACHERNAR_FACEBOOK_APP_SECRET` | ambos presentes ⇒ Facebook habilitado globalmente |
| P-72 | `ACHERNAR_FACEBOOK_GRAPH_VERSION` | versão do Graph API vigente na implantação (ex.: `v23.0` ou superior) — parâmetro, não fixado em código |
| P-73 | `ACHERNAR_DB_URL`, `_USER`, `_PASSWORD` | credenciais fora do repositório |
| P-74 | limites do registro | ≤ 20 origens, ≤ 20 redirects, ≤ 20 audiências por aplicação; ≤ 100 aplicações; ≤ 100 APIs |
| P-75 | cookies | `ACHERNAR_COOKIE_SECURE` (`true`; `false` só no perfil `dev`), `ACHERNAR_COOKIE_DOMAIN` (vazio = host-only) |
| P-76 | `ACHERNAR_MANAGEMENT_PORT` | `9090` (não exposto publicamente) |
| P-77 | `SPRING_PROFILES_ACTIVE` | `dev` \| `prod` |

## 10. Regras de validação de parâmetros

- PV-01: valores fora dos limites impedem a subida (`prod`) ou geram aviso (`dev`).
- PV-02: `ACHERNAR_ISSUER` deve ser `https://` em `prod`; em `dev` aceita `http://localhost*`.
- PV-03: P-12 ≥ P-01 + P-06 + P-14 (senão a subida falha: tokens válidos perderiam a chave).
- PV-04: P-03 ≥ P-02.
- PV-05: na subida, se P-66 ausente em `prod` ⇒ falha; em `dev` usa chave derivada fixa **apenas** se `ACHERNAR_DEV_INSECURE_KEY=true` (explícito) e registra WARN.

## 11. Complementos

| ID | Variável | Padrão | Descrição |
|---|---|---|---|
| P-78 | `ACHERNAR_MAIL_SUPPORT_CONTACT` | vazio (opcional) | contato exibido no rodapé dos e-mails de segurança |
| P-79 | `ACHERNAR_HASH_CONCURRENCY` | `2 × núcleos` | máximo de hashes Argon2id simultâneos (SEC-06) |
| P-80 | `ACHERNAR_JOB_KEY_ROTATION_CRON` / `_RETENTION_CRON` | `0 0 * * * *` (hora em hora) / `0 0 3 * * *` (03:00 UTC) | agendamento dos jobs |
| PV-06 | P-11 ≥ 2 × P-14 | — | senão a subida falha (consumidores podem não ter a chave nova em cache) |
