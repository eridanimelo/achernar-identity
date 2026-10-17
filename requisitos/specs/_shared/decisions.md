# Achernar Identity — Registro de Decisões (D-xx)

> Autoria: IA, sob a autorização do prompt de 2026-10-16 (“Autonomia e continuidade”). Nenhuma decisão aqui
> foi atribuída ao humano. Cada decisão registra **Decisão · Justificativa · Premissas · Consequências**.
> Precedência: prompt do humano > este registro > `.ia/config/*` > `.ia/rules/*` (ver D-40 para divergências).

## Índice

| ID | Tema |
|---|---|
| D-01 | Instalação por projeto; sem multi-tenancy, SSO ou federação |
| D-02 | Superfície de autenticação própria (API JSON); sem Spring Authorization Server |
| D-03 | Versões e compatibilidade (Java, Spring, Angular, PostgreSQL, Liquibase) |
| D-04 | Assinatura do JWT: RS256, RSA 3072, `kid` por thumbprint |
| D-05 | Access token: validade e claims |
| D-06 | Refresh token opaco, rotativo, família por sessão, detecção de reutilização e tolerância |
| D-07 | Estratégia de sessão no navegador (cookie HttpOnly de refresh + access em memória) |
| D-08 | Tipos de aplicação cliente: `BROWSER` e `CONFIDENTIAL` |
| D-09 | Modelo de audiências (múltiplas audiências por sessão) |
| D-10 | Bloqueio imediato: introspecção e obrigações das APIs |
| D-11 | E-mail não verificado: sem login, sem tokens |
| D-12 | Conta desativada |
| D-13 | Política anti-enumeração |
| D-14 | Normalização e unicidade de e-mail |
| D-15 | Senhas: Argon2id e política NIST |
| D-16 | Limitação de taxa e bloqueio de login em PostgreSQL (sem Redis) |
| D-17 | Tokens de uso único (verificação, recuperação, troca de e-mail) |
| D-18 | E-mail: outbox transacional + SMTP + MailHog local |
| D-19 | Login social: Identity como cliente confidencial do provedor |
| D-20 | Regras de criação e vinculação social |
| D-21 | Último método de acesso e senha local para conta social |
| D-22 | Reautenticação recente para operações sensíveis |
| D-23 | Perfis administrativos e proteção do último administrador |
| D-24 | Criação do primeiro administrador (bootstrap) |
| D-25 | Chaves de assinatura: armazenamento cifrado e rotação |
| D-26 | Eventos de segurança em tabela do PostgreSQL |
| D-27 | Retenção de dados e anonimização (LGPD) |
| D-28 | Integração com sistemas de negócio: `sub`, provisionamento preguiçoso, sem webhooks |
| D-29 | Estratégia de identificadores |
| D-30 | Arquitetura do backend |
| D-31 | Arquitetura do frontend |
| D-32 | Formato de erro (RFC 9457) |
| D-33 | Prefixos e versionamento de API |
| D-34 | Implantação (Docker Compose, TLS por proxy reverso) |
| D-35 | Observabilidade |
| D-36 | Política de migrações |
| D-37 | Internacionalização |
| D-38 | Identidade visual |
| D-39 | Granularidade das Specs |
| D-40 | Divergências com o repositório e com o framework `.ia/` |
| D-41 | CORS dinâmico por registro de aplicações |
| D-42 | Tempo, formatos e relógio |
| D-43 | Concorrência e consistência |
| D-44 | Limite de sessões simultâneas |
| D-45 | Provedores sociais: habilitação global e por aplicação |
| D-46 | A UI do Identity é uma aplicação registrada |
| D-47 | Troca de e-mail |
| D-48 | Equalização de tempo de resposta no login |
| D-49 | Neutralidade tecnológica das Specs |
| D-50 | Fora de escopo explícito |

---

## D-01 — Instalação por projeto

- **Decisão:** cada projeto freelancer opera **uma instalação** do Identity (um processo/ambiente, um banco, uma configuração). Uma instalação tem **uma única base de contas**; não há realms, tenants, organizações nem SSO entre aplicações independentes.
- **Justificativa:** reflete o escopo declarado (§1) e elimina complexidade de isolamento.
- **Premissas:** projetos distintos não compartilham contas; uma instalação pode atender várias aplicações do **mesmo** projeto (ex.: SPA, painel, relatórios).
- **Consequências:** a mesma conta (mesmo `sub`) pode usar todas as aplicações/APIs registradas na instalação; sessões são **por aplicação** (não há sessão compartilhada entre aplicações, logo não há SSO). Quem precisar de contas separadas por cliente instala outra instância.

## D-02 — Superfície de autenticação própria

- **Decisão:** o Identity expõe uma **API JSON de autenticação** (`/auth/*`) consumida pelas telas das próprias aplicações (cadastro, login, recuperação), mais uma API de conta/administração (`/api/v1/*`). Tokens são emitidos pelo próprio serviço usando Spring Security 7 (Resource Server, OAuth2 Client para login social, `JwtEncoder` com Nimbus). **Não** se usa Spring Authorization Server nem Authorization Code + PKCE como login primário.
- **Justificativa:** o requisito exige telas próprias nas aplicações consumidoras, não SSO e não ser “alternativa ao Keycloak”. Authorization Server traria consentimento, registro de clientes OIDC, endpoints `/oauth2/*` e fluxos de redirecionamento que o produto não precisa.
- **Premissas:** as telas de login ficam sob a origem da aplicação; o Identity só redireciona o navegador no login social.
- **Consequências:** divergência com `technology.yaml` (`spring_authorization_server`, `authorization_server_jdbc_components`) — ver D-40 e `config-changes.md`. O Identity publica **metadados mínimos** (`issuer`, `jwks_uri`) para que o Spring Security das APIs resolva as chaves, sem declarar-se um OIDC Provider completo.
- **Alternativas rejeitadas:** (a) Spring Authorization Server (complexidade e SSO implícito); (b) Keycloak (fora do escopo, instalação pesada por projeto).

## D-03 — Versões e compatibilidade

Verificado em 2026-10-17 em fontes oficiais (docs.spring.io/spring-boot system requirements; angular.dev/reference/versions; postagens spring.io):

| Item | Escolha | Evidência / racional |
|---|---|---|
| Java | **25 (LTS)** — `--release 25`; JDK 25 em runtime | Spring Boot 4.1.1 documenta compatibilidade “de Java 17 até Java 26”; **Java 27 não consta** como suportado e é versão não-LTS (suporte curto, ~6 meses). `technology.yaml` indicava 27: **desviado** (D-40). Atualização para o próximo LTS (Java 29) deve ser reavaliada em 2027. |
| Spring Boot | **4.1.x** (4.1.1 vigente em 2026-10) | Exige Spring Framework 7.0.9+; Maven 3.6.3+; Tomcat 11 (Servlet 6.1). |
| Spring Security | **7.x** gerenciado pelo BOM do Boot 4.1.x | Boot 4 → Security 7. Versão exata a confirmar com `mvn dependency:tree` na implementação. |
| Hibernate/JPA | versão do BOM (Hibernate 7.x, Jakarta Persistence 3.2) | `ddl-auto=validate`. |
| Banco | **PostgreSQL 18** | Versão estável vigente; Docker atual usa 16 (suportado até 2028) — atualização da imagem registrada em `config-changes.md`. |
| Migrações | **Liquibase** (versão do BOM do Boot 4.1) | Já definido em `technology.yaml`. |
| Build backend | **Maven 3.9+** (wrapper) | `technology.yaml`. |
| Frontend | **Angular 22.0.x**, componentes standalone, signals, Control Flow | `angular.dev/reference/versions`: Node `^22.22.3 \|\| ^24.15.0 \|\| ^26.0.0`, TypeScript `>=6.0.0 <6.1.0`, RxJS `^6.5.3 \|\| ^7.4.0`. |
| Angular Material | **22.0.x** (mesmo major do Angular), tema Material 3 | `technology.yaml`. O README citava Tailwind: **removido** (D-40). |
| Node (build) | **24 LTS** (≥ 24.15) | Dentro do intervalo suportado pelo Angular 22. |
| i18n frontend | **Transloco** (runtime, JSON) | D-37. |
| JOSE/JWT | Nimbus JOSE + JWT (dependência de `spring-security-oauth2-jose`) | Biblioteca consolidada; sem criptografia própria. |
| Hash de senha | `Argon2PasswordEncoder` (Spring Security Crypto + BouncyCastle) | D-15. |
| Cifra de dados em repouso (chaves, outbox) | `BytesEncryptor` do Spring Security Crypto (AES-256-GCM) | D-25, D-18. |
| E-mail | Spring Mail (SMTP) + Thymeleaf (templates) | D-18. |
| Testes | JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), GreenMail/MailHog API, Karma/Jest + Playwright | Escolha de ferramentas fica na implementação; listada aqui só como baseline de compatibilidade. |

- **Premissas:** versões “.x” serão fixadas (pin) no primeiro commit de implementação e revalidadas contra os requisitos acima; a fonte única é `pom.xml`/`package.json`.
- **Consequências:** `technology.yaml` precisa ser atualizado (D-40/`config-changes.md`) antes da implementação, por decisão registrada — o GATE-AUT de tecnologia só passa com o arquivo coerente.

## D-04 — Assinatura do JWT

- **Decisão:** algoritmo **RS256** (RSASSA-PKCS1-v1_5 + SHA-256); chaves RSA **3072 bits**; `kid` = thumbprint SHA-256 da chave pública (RFC 7638); **único** algoritmo aceito/emitido (`alg=none`, HS*, ES*, PS* rejeitados). Cabeçalho `typ: at+jwt` (RFC 9068).
- **Justificativa:** RS256 é o padrão do `NimbusJwtDecoder` do Spring Security (nenhuma configuração extra nas APIs consumidoras) e interoperável com qualquer linguagem.
- **Premissas:** custo de verificação RSA é aceitável. 
- **Consequências:** se uma API usar outra stack, deve suportar RS256 e JWKS. Alterar o algoritmo no futuro exigiria período de transição com duas famílias de chaves.

## D-05 — Access token

- **Decisão:** JWT assinado, validade **10 min** (P-01; limites 5–15 min; por aplicação pode ser reduzida/aumentada dentro dos limites). Claims: `iss`, `sub`, `aud` (array), `exp`, `iat`, `nbf`, `jti`, `sid`, `azp`, `amr`, `auth_time`; `roles` somente quando `aud` contém a audiência do próprio Identity. Sem `email`, nome ou dados de negócio. Detalhes em `token-lifecycle.md`.
- **Justificativa:** janela curta limita impacto da não-revogabilidade; `sid` permite introspecção e bloqueio imediato (D-10).
- **Consequências:** APIs de negócio **não** devem derivar autorização de negócio de claims do Identity além de `sub`.

## D-06 — Refresh token

- **Decisão:** opaco (256 bits aleatórios, base64url), armazenado **apenas como hash SHA-256**; **rotativo a cada uso**; todos os refresh tokens de uma sessão formam uma família (a sessão é a família). Reutilização de um token já rotado **fora da tolerância** revoga a sessão inteira e registra `REFRESH_REUSE_DETECTED`. **Tolerância de reutilização:** 10 s (P-04) apenas para o **antecessor imediato**; nesse caso o servidor devolve novo access token **sem** emitir novo refresh token (o vencedor da corrida já o recebeu). Validade deslizante 7 dias (P-02) limitada ao teto absoluto da sessão 30 dias (P-03).
- **Justificativa:** rotação + detecção de reuso é a prática recomendada (OAuth 2.0 Security BCP); a tolerância resolve a corrida entre múltiplas abas sem falso positivo.
- **Premissas:** cliente do navegador usa também trava entre abas (Web Locks) como primeira defesa.
- **Consequências:** clientes confidenciais devem serializar renovações por sessão.

## D-07 — Sessão no navegador

- **Decisão:** para aplicações `BROWSER`: **refresh token em cookie `HttpOnly; Secure; SameSite=Strict; Path=/auth`**, nome `__Secure-achrt_<client_id>` (um cookie por aplicação); **access token apenas em memória JS** (nunca `localStorage`/`sessionStorage`/IndexedDB); ao recarregar a página o SPA chama `POST /auth/refresh`. Proteção CSRF dos endpoints com cookie (`/auth/refresh`, `/auth/logout`): `SameSite=Strict` + verificação do cabeçalho `Origin` contra as origens da aplicação + cabeçalho obrigatório `X-Achernar-Client: <client_id>` e `Content-Type: application/json` (força preflight CORS).
- **Justificativa:** reduz exfiltração por XSS (refresh inacessível ao JS) e elimina persistência do access token. Atende telas próprias das aplicações.
- **Premissas (crítica):** o Identity e as aplicações `BROWSER` ficam no **mesmo site** (mesmo domínio registrável; ex.: `id.cliente.com.br` e `app.cliente.com.br`; em desenvolvimento, `localhost` com portas distintas conta como mesmo site). Aplicações em **site diferente** do Identity **não** usam o modo `BROWSER`: usam um BFF/servidor próprio como cliente `CONFIDENTIAL` (D-08).
- **Consequências:** XSS ainda permite usar a sessão enquanto a página está aberta (risco residual aceito e mitigado por CSP e por TTL curto); logout em uma aba propaga via `BroadcastChannel`.
- **Alternativas rejeitadas:** `localStorage` (XSS rouba refresh de longa duração); cookies `SameSite=None` entre sites (bloqueio de cookies de terceiros torna frágil).

## D-08 — Tipos de aplicação cliente

- **Decisão:** dois tipos. `BROWSER` (cliente público: SPA; autentica com `client_id` + origem; refresh só por cookie). `CONFIDENTIAL` (backend/BFF/servidor: autentica com `client_id` + segredo via HTTP Basic; refresh no corpo da resposta). Segredos: 256 bits, exibidos **uma vez**, armazenados como hash; até **2 segredos ativos** durante rotação.
- **Justificativa:** cobre SPA, SSR/BFF e microsserviços sem abstrações extras. Aplicações nativas/móveis ficam fora desta versão (D-50).
- **Consequências:** `client_id` e CORS **não são prova de autenticação** da aplicação (RN-APP-05); apenas `CONFIDENTIAL` é autenticada.

## D-09 — Audiências

- **Decisão:** cada **API consumidora** (`resource_api`) tem um identificador de audiência (`audience`, string até 200 chars, normalmente URL base da API). Cada aplicação lista as APIs permitidas. No login/exchange, o cliente pode pedir `audiences` (subconjunto); se omitido, vale o conjunto **padrão** da aplicação. A **sessão fixa** as audiências concedidas; o refresh pode **estreitar**, nunca ampliar. O token tem `aud` = audiências concedidas (+ audiência do Identity se `identity_api_access=true`).
- **Justificativa:** um único token atende monolito e microsserviços do mesmo projeto sem emissão de N tokens; cada API só aceita se **sua** audiência está em `aud`.
- **Consequências:** token multi-audiência aumenta o raio de vazamento entre APIs do mesmo projeto; recomenda-se separar por aplicação quando a confiança entre APIs for baixa (limites de confiança em `security-policy.md`).

## D-10 — Bloqueio imediato e obrigações das APIs

- **Decisão:** JWT é válido até `exp` mesmo após logout/revogação do refresh. Política:
  1. APIs de negócio **validam localmente** (assinatura via JWKS, `iss`, `aud`, `exp`, `nbf`, `alg`).
  2. Operações **sensíveis** (definidas por cada API) **devem** chamar a introspecção (`POST /api/v1/tokens/introspect`, credenciais da API) e podem cachear a resposta por no máximo 30 s (P-46).
  3. O próprio Identity valida `sid` no banco a cada chamada a `/api/v1/*` (revogação imediata para a API do Identity).
  4. Eventos de bloqueio imediato (desativação da conta, reutilização de refresh detectada, “encerrar todas as sessões”, troca de senha por recuperação) revogam as sessões; efeito nas APIs: imediato **somente** onde há introspecção; caso contrário em até a vida restante do access token (≤ 10 min).
- **Justificativa:** equilibra desempenho e segurança sem exigir chamada ao Identity a cada requisição.
- **Consequências:** o catálogo de APIs registra o campo `introspection_credential`; não há lista de revogação distribuída.

## D-11 — E-mail não verificado

- **Decisão:** conta criada por e-mail/senha nasce em `PENDING_VERIFICATION`. Enquanto pendente: **nenhum token é emitido**; login com senha correta retorna `EMAIL_NOT_VERIFIED` (403) e não revela o estado a quem não conhece a senha; pode reenviar a verificação; após verificar, a conta vira `ACTIVE` e o usuário precisa **fazer login** (não há login automático por link). Contas pendentes por mais de 7 dias são removidas (P-50).
- **Justificativa:** impede uso de contas com e-mail de terceiros e reduz vetor de pré-sequestro.

## D-12 — Conta desativada

- **Decisão:** estado `DISABLED` (por administrador): todas as sessões revogadas na desativação; login (senha ou social) **negado**; refresh negado; tokens de uso único pendentes invalidados; recuperação de senha **não** reativa a conta; e-mail permanece reservado (unicidade). Reativação (`ACTIVE`) **não** restaura sessões. Introspecção devolve `active=false`.

## D-13 — Anti-enumeração

- **Decisão:** respostas **idênticas em corpo, status e (tanto quanto possível) tempo** para: cadastro (e-mail já existente), recuperação de senha, reenvio de verificação, solicitação de troca de e-mail e login com credenciais inválidas (inclui conta inexistente, senha errada e conta bloqueada por tentativas). Para e-mail já cadastrado, o titular recebe e-mail informativo (TPL-05). Estados específicos (`EMAIL_NOT_VERIFIED`, `ACCOUNT_DISABLED`) só são revelados após prova de posse da senha ou do provedor social.
- **Consequências:** UI não pode confirmar “e-mail não encontrado”; mensagens de UX são genéricas (“se o e-mail existir, enviaremos…”).

## D-14 — E-mail: normalização e unicidade

- **Decisão:** `email_normalized` = aparar espaços → Unicode NFC → minúsculas (`Locale.ROOT`) → domínio convertido a ASCII (IDN/punycode). Parte local restrita a ASCII imprimível sem aspas/comentários (SMTPUTF8 não suportado). Comprimento máx. 254; 1 único `@`. **Sem** dobra de pontos nem remoção de `+tag`. Unicidade global por `email_normalized` (índice único), inclusive contas `PENDING_VERIFICATION`, `DISABLED` e anonimizadas (estas liberam o endereço). O e-mail é exibido como digitado (`email_display`).
- **Consequências:** `usuario+a@x.com` e `usuario+b@x.com` são contas distintas.

## D-15 — Senhas

- **Decisão:** hash **Argon2id** (parâmetros iniciais OWASP: memória 19 MiB, 2 iterações, paralelismo 1; ajustáveis P-16) via `DelegatingPasswordEncoder` (permite migrar algoritmo; rehash no login quando parâmetros mudarem). Política (NIST SP 800-63B): mínimo **12**, máximo **128** caracteres Unicode (NFKC antes do hash), sem regras de composição, sem expiração periódica; rejeita senhas da lista de comuns embutida (≥ 10.000 entradas), senhas que contenham a parte local do e-mail (≥ 4 caracteres) ou o domínio, e senhas só de espaços.
- **Justificativa:** padrão atual; evita limite de 72 bytes do bcrypt.

## D-16 — Limitação de taxa e bloqueio

- **Decisão:** contadores em PostgreSQL (tabela `rate_limit_counter`, janela fixa, upsert atômico); **sem Redis**. Bloqueio de login por conta/e-mail: 5 falhas em 15 min → 15 min de bloqueio, dobrando (máx. 2 h) em reincidência em 24 h; respostas indistinguíveis (D-13); e-mail de aviso ao titular (TPL-08) uma vez por bloqueio. Limites por IP e por rota em P-31…P-39.
- **Premissas:** IP real do cliente obtido do proxy confiável (`X-Forwarded-For` de proxies configurados em P-47).
- **Consequências:** o bloqueio por conta permite atacante bloquear temporariamente um titular; mitigado por crescimento limitado (2 h) e pela recuperação de senha, que ignora o bloqueio de login.

## D-17 — Tokens de uso único

- **Decisão:** verificação de e-mail, recuperação de senha e confirmação de troca de e-mail usam tokens de **256 bits**, armazenados com hash SHA-256, **um ativo por (conta, finalidade)** (emitir novo invalida o anterior), uso único com consumo atômico (`UPDATE … WHERE consumed_at IS NULL AND expires_at > now()`), validades: verificação 24 h, recuperação 30 min, troca de e-mail 1 h (P-20…P-22). O link usa **fragmento** (`#token=`) para não vazar em logs/Referer.
- **Consequências:** resposta única e genérica `TOKEN_INVALID_OR_EXPIRED` para inexistente, expirado, consumido ou invalidado.

## D-18 — E-mail

- **Decisão:** envio **assíncrono via outbox** (`email_outbox`) gravada na mesma transação da mudança de estado; worker com `FOR UPDATE SKIP LOCKED`, até 6 tentativas com backoff 1 min / 5 min / 15 min / 1 h / 6 h; payload com links cifrado (AES-GCM) e **apagado após envio**; falhas definitivas geram `EMAIL_DELIVERY_FAILED`. Transporte **SMTP genérico** (host/porta/TLS/credenciais por ambiente); **MailHog** apenas em desenvolvimento (já no Compose). Provedor de produção: **parâmetro de implantação** (não definido).
- **Consequências:** não há dependência de API proprietária de provedor; trocar de provedor = trocar variáveis SMTP.

## D-19 — Login social

- **Decisão:** o Identity é **cliente confidencial** (segredo no backend) do Google e do Facebook, usando Authorization Code com PKCE e `state` próprios (Spring Security OAuth2 Client). Google: **OpenID Connect** (valida ID token: assinatura via JWKS do Google, `iss`, `aud`, `exp`, `nonce`, `email_verified`). Facebook: **OAuth 2.0** (sem ID token na web); identidade = `id` (app-scoped) obtido do Graph API com `appsecret_proof`. Transação de login persistida em banco (`social_transaction`, 10 min, uso único) e **amarrada ao navegador** por cookie temporário `__Secure-achsoc` (HttpOnly, SameSite=Lax). Ao final, o Identity emite **código próprio de uso único (60 s)** vinculado a `client_id`, `redirect_uri` e `code_challenge` (PKCE do cliente) que o front troca em `POST /auth/social/exchange`. Tokens de Google/Facebook **nunca** são devolvidos nem armazenados (descartados após obter identidade).
- **Justificativa:** protege contra CSRF de login, injeção de código, reuso e open redirect (redirect_uri por correspondência exata).

## D-20 — Criação e vinculação social

Regras (também RN-SOC-xx):
1. Identidade social = (`provider`, `provider_subject`). Vínculo existente → login na conta vinculada (se `ACTIVE`).
2. Sem vínculo e **sem** conta com o e-mail → cria conta e vínculo. Google (`email_verified=true`): conta `ACTIVE` e e-mail verificado. Facebook ou e-mail não verificado: conta `PENDING_VERIFICATION` + e-mail de verificação próprio (nenhum token até verificar). Provedor sem e-mail → `SOCIAL_EMAIL_UNAVAILABLE`.
3. Sem vínculo e **com** conta local de mesmo e-mail (ativa/desativada) e e-mail **verificado pelo provedor**: **não vincula**; retorna `account_link_required`; o usuário deve entrar pelo método existente e vincular em *Conta → Segurança* (exige reautenticação recente **e** conclusão do fluxo no provedor). Se o provedor **não** verifica o e-mail (Facebook): nada muda, o titular recebe TPL-05 e o navegador recebe `email_verification_required`.
4. Sem vínculo e conta local de mesmo e-mail **pendente de verificação**: a conta pendente é **substituída** (senha descartada) se o e-mail social for verificado pelo provedor (defesa contra pré-sequestro).
5. Vínculo (UC-053) só ocorre com sessão autenticada **e** reautenticação ≤ 5 min **e** prova de controle da identidade no provedor; jamais por coincidência de e-mail.
6. Um vínculo (`provider`, `provider_subject`) pertence a **uma** conta; uma conta tem no máx. **um** vínculo por provedor.
7. Mudança de e-mail no provedor não altera o e-mail da conta.

## D-21 — Último método de acesso

- **Decisão:** toda conta `ACTIVE` mantém ≥ 1 método de acesso (senha local **ou** ≥ 1 vínculo social). Desvincular/remover o último é negado (`LAST_ACCESS_METHOD`). Conta social sem senha pode **definir senha local** (UC-037) com reautenticação via provedor; também pode usar “Esqueci a senha” (e-mail verificado comprova titularidade) para definir a primeira senha.

## D-22 — Reautenticação

- **Decisão:** operações sensíveis exigem `session.last_reauth_at ≥ now − 5 min` (P-07): alterar/definir senha, solicitar troca de e-mail, vincular/desvincular provedor, encerrar todas as sessões (as demais revogações de sessões individuais não exigem). Reautenticar: `POST /api/v1/account/reauth` com a senha, ou novo fluxo social (propósito `REAUTH`) para contas sem senha. Login recém-realizado conta como reautenticação.

## D-23 — Perfis administrativos

- **Decisão:** perfis `USER` (implícito) e `ADMIN`. A autorização administrativa é verificada **no banco a cada requisição** (conta ativa + perfil), não apenas pelo claim. **Proteção do último administrador:** é negado desativar, rebaixar ou anonimizar o último `ADMIN` ativo (`LAST_ADMIN_PROTECTED`), com serialização por trava consultiva para evitar corrida. Admin não pode desativar a si mesmo se for o último. Concessão/revogação do perfil (UC-083) incluída porque, sem ela, o requisito de “último administrador” e a continuidade operacional ficam sem utilidade.

## D-24 — Primeiro administrador

- **Decisão:** enquanto **não existir administrador ativo**, o endpoint `POST /setup/admin` aceita `{bootstrap_token, email, password}`; `bootstrap_token` é um segredo de implantação (`ACHERNAR_BOOTSTRAP_TOKEN`, ≥ 32 caracteres, comparação em tempo constante). Após criar, o endpoint passa a responder `404` e a UI de setup some; a variável é ignorada (recomenda-se removê-la). O admin é criado `ACTIVE`, e-mail verificado (operador confiável), evento `BOOTSTRAP_ADMIN_CREATED`. Sem credenciais no código/imagem/Compose; o `.env.example` traz apenas placeholder. Também serve de **recuperação** se todos os admins ficarem inativos por manipulação direta do banco. Limite de tentativas 5 / 15 min / IP (P-38).

## D-25 — Chaves de assinatura

- **Decisão:** pares RSA gerados pela própria aplicação (JCA, sem cripto própria), privada **cifrada com AES-256-GCM** usando a chave-mestra `ACHERNAR_MASTER_KEY` (externa, ≥ 32 bytes), armazenada em `signing_key`. Estados: `PUBLISHED` (no JWKS, ainda não assina) → `ACTIVE` (assina) → `RETIRED` (só no JWKS) → `REMOVED`. Rotação automática a cada **90 dias** (P-10): nova chave publicada **24 h antes** de assinar (P-11); chave antiga permanece no JWKS **48 h** após deixar de assinar (P-12). Rotação manual e “remoção emergencial” (chave comprometida sai imediatamente do JWKS; sessões continuam via refresh) por administrador. JWKS com `Cache-Control: public, max-age=300`.
- **Premissas:** sem HSM/KMS nesta versão; chave-mestra provida por ambiente/segredo do orquestrador. Perda da chave-mestra = regenerar chaves e forçar novo login.

## D-26 — Eventos de segurança

- **Decisão:** tabela `security_event` (somente inserção pela aplicação). Eventos de mudança de estado são gravados **na mesma transação** da mudança; eventos de falha (ex.: `LOGIN_FAILED`) em transação independente (`REQUIRES_NEW`) para sobreviver a rollback. Catálogo em `event-catalog.md`. Sem segredos/tokens/senhas. IP completo por 90 dias e então anulado; evento por 365 dias (P-53/P-54).

## D-27 — Retenção e anonimização

- **Decisão:** ver parâmetros P-50…P-56 e `data-model.md §Retenção`. **Anonimização** (UC-084) de conta `DISABLED`: troca e-mail por `anon+<id>@invalid.example` (não entregável), remove senha e vínculos sociais, revoga sessões, mantém o `sub` e eventos (com e-mail redigido) — atende pedido de eliminação do titular (LGPD) preservando integridade referencial das aplicações. Responsabilidade pelos dados de negócio permanece com cada aplicação.

## D-28 — Integração com sistemas de negócio

- **Decisão:** vínculo **exclusivamente por `sub`**. Padrão recomendado: **provisionamento preguiçoso e idempotente** — a API de negócio cria/atualiza seu cadastro (`PUT /me` / upsert por `sub`) no primeiro acesso autenticado ou no passo “completar cadastro” do front. Sem transação distribuída; falha parcial tratada por idempotência e reexecução. **Sem webhooks** do Identity nesta versão.
- **Consequências:** desativação/anonimização no Identity chegam às APIs por token expirado (≤ 10 min) ou introspecção.

## D-29 — Identificadores

- **Decisão:** `account.id` (= `sub`), `session.id` (= `sid`), `application`/`resource_api` ids: **UUID v4** (opacos, sem tempo embutido). Tabelas de alto volume (`security_event`, `email_outbox`): **UUID v7** gerado na aplicação (ordenável). `client_id` público: string `[a-z0-9-]{3,64}` única, escolhida pelo admin.

## D-30 — Arquitetura do backend

- **Decisão:** monólito modular por funcionalidade; pacote raiz `com.eridani.achernar.identity`. Cada módulo: `web` (controllers/DTOs/validação), `application` (casos de uso/transações), `domain` (entidades JPA com regras, enums, exceções), `infra` (repositórios, adaptadores externos). **Portas/adaptadores apenas onde há fronteira externa real:** envio de e-mail, provedores sociais, relógio. Entidades JPA servem de modelo de domínio (sem espelho duplicado). Módulos: `setup`, `account`, `credential`, `auth`, `token`, `session`, `social`, `application` (registro de aplicações e APIs), `mail`, `audit`, `ratelimit`, `admin`, `shared`.
- **Justificativa:** clareza sem cerimônia (regra “evite abstrações”).

## D-31 — Arquitetura do frontend

- **Decisão:** Angular standalone, rotas lazy por feature, `core/` (auth, http, guards, i18n, tema, config), `shared/` (componentes reutilizados por ≥ 2 features), `features/<feature>/{pages,components,services,models}`. Estado com **signals** + serviços; HTTP com `HttpClient` + interceptors. Detalhes em `ui-map.md`.

## D-32 — Erros

- **Decisão:** `application/problem+json` (RFC 9457): `type`, `title`, `status`, `detail`, `instance`, e extensões `code` (estável), `trace_id`, `errors[]` (campo, código, mensagem). Catálogo em `error-catalog.md`. Nunca expor stack trace.

## D-33 — Prefixos e versionamento

- **Decisão:** `/auth/*` (protocolo para telas das aplicações; evolui de forma aditiva; ruptura → `/auth/v2`), `/api/v1/*` (conta, sessões, admin, introspecção), `/setup/*`, `/.well-known/*`, `/actuator/*` (porta de gestão). Alterações incompatíveis exigem novo prefixo e período de convivência ≥ 90 dias.

## D-34 — Implantação

- **Decisão:** Docker Compose com perfis `dev` (Postgres, MailHog; backend/frontend podem rodar fora) e `prod` (Postgres, backend, frontend estático, proxy reverso Caddy com HTTPS automático, backup agendado). Imagens multi-estágio, usuário não-root, segredos por variáveis/arquivos fora do repositório. Mudanças em `docker/` **documentadas, não executadas** (`deployment-operations.md`, `config-changes.md`).

## D-35 — Observabilidade

- **Decisão:** logs JSON estruturados com `traceId`, redação de segredos (RNF-06); Actuator em porta separada (`health/liveness`, `health/readiness`, `metrics`, `prometheus` opcional); métricas de domínio (logins, falhas, reuso, outbox pendente).

## D-36 — Migrações

- **Decisão:** Liquibase com changelog mestre e arquivos numerados `NNNN-descricao.yaml` (um `changeSet` por mudança lógica, `id` = `NNNN-n`, `author` = `achernar`); sem `DROP` destrutivo no mesmo release (padrão expandir→migrar→contrair); `ddl-auto=validate`; execução na subida da aplicação com trava do Liquibase; backup obrigatório antes de upgrade em produção.

## D-37 — Internacionalização

- **Decisão:** idiomas `pt-BR` (padrão) e `en`; frontend com Transloco (JSON por feature); e-mails por idioma da conta (`account.locale`, capturado no cadastro via `Accept-Language`/parâmetro, padrão `pt-BR`); mensagens de erro da API são **códigos** (a UI traduz); `detail` em pt-BR/en conforme `Accept-Language`.

## D-38 — Identidade visual

- **Decisão:** não há logo no repositório; adota-se proposta documentada (`visual-proposal.md`): tema “Achernar” (estrela azul-branca de Eridanus), azul-estrela + índigo-noite, com marca tipográfica provisória e ícone de estrela de 4 pontas. Substituível quando houver logo oficial.

## D-39 — Granularidade das Specs

- **Decisão:** o Discovery decompõe em **casos de uso (UC-xxx)**; cada Spec (`FT-xx`) cobre uma feature coesa com vários UCs que compartilham dados, segurança e telas, mantendo **critérios de aceite por UC**. A implementação é sequenciada por UC (Waves).
- **Justificativa:** 50 Specs de um UC duplicariam dados/segurança/UI; o framework exige apenas que Feature ≠ UC no Discovery.

## D-40 — Divergências com o repositório e com o framework

| ID | Item | Situação encontrada | Resolução |
|---|---|---|---|
| DEV-01 | `technology.yaml`: `spring_authorization_server`, `authorization_server_jdbc_components` | conflita com D-02 | Remover na futura atualização do arquivo |
| DEV-02 | `technology.yaml`: Java 27 | Boot 4.1.1 suporta até 26; 27 não-LTS | Java 25 LTS (D-03) |
| DEV-03 | `README.md` e `PROJECT.yaml` descrevem SSO/OIDC, 30 Specs e “sem login social” | Specs inexistentes em `requisitos/`; escopo diverge do prompt | README atualizado nesta execução; `PROJECT.yaml` listado em `config-changes.md` |
| DEV-04 | README cita Angular + Tailwind | `technology.yaml`/prompt: Angular Material | Material 3 (D-03) |
| DEV-05 | Framework: “uma etapa por vez” e Specs sem tecnologia | prompt exige discovery + specs técnicas completas numa execução | Todas as etapas até Specification produzidas **sem iniciar implementação**; tecnologia confinada a documentos técnicos `_shared` (D-49) |
| DEV-06 | `jira.yaml` aponta projeto TS1506CS (não relacionado) | não solicitado | Nenhuma issue criada; registrado como pendência de config |
| DEV-07 | `git.yaml`: base `develop` inexistente (só `master`) | fora do escopo | Nada commitado; registrado |

## D-41 — CORS dinâmico

- **Decisão:** `CorsConfigurationSource` consulta o registro: a origem da requisição deve constar nas origens de **alguma** aplicação ativa (para rotas públicas `/auth/*`), e, nas rotas com cookie, na origem da **aplicação indicada por `client_id`**. Sem `*`; `Access-Control-Allow-Credentials: true` somente nesses casos; `Vary: Origin`; `max-age` 600 s; cabeçalhos permitidos: `Authorization, Content-Type, X-Achernar-Client, Accept-Language`. CORS **não** é autenticação (RN-APP-05).

## D-42 — Tempo

- **Decisão:** UTC em armazenamento e API (ISO-8601 com `Z`); `Clock` injetável; tolerância de relógio 60 s (P-06) na validação.

## D-43 — Concorrência e consistência

- **Decisão:** unicidade e invariantes por **restrições do banco**; consumo de tokens/códigos por `UPDATE` condicional; rotação de refresh com travamento de linha (`SELECT … FOR UPDATE` do token/sessão); controle otimista (`version`) em contas/aplicações; trava consultiva transacional para “último administrador” e limite de sessões. Idempotência por restrições + respostas equivalentes (ex.: cadastro duplicado concorrente → mesma resposta 202).

## D-44 — Limite de sessões

- **Decisão:** máx. **10** sessões ativas por conta (P-05); ao exceder, a sessão ativa menos recentemente usada é revogada (`reason=SESSION_LIMIT`).

## D-45 — Provedores sociais

- **Decisão:** habilitação **global** por provedor (credenciais presentes na configuração) e **por aplicação** (flags `google_enabled`, `facebook_enabled`). Provedor sem credenciais não aparece em `client-config`.

## D-46 — UI do Identity como aplicação

- **Decisão:** a UI de administração/conta é a aplicação `achernar-identity-ui` (tipo `BROWSER`, `identity_api_access=true`, `allow_registration=false` por padrão configurável), criada no setup a partir de `ACHERNAR_UI_ORIGIN`/`ACHERNAR_UI_BASE_URL`. Cadastro público pela UI do Identity é opcional (flag).

## D-47 — Troca de e-mail

- **Decisão:** exige reautenticação recente; confirmação por link enviado ao **novo** endereço (1 h); na confirmação: revalida unicidade, grava, **revoga demais sessões**, invalida tokens de recuperação pendentes e notifica o **endereço antigo** (TPL-07). Solicitação sempre responde 202 (D-13).

## D-48 — Equalização de tempo no login

- **Decisão:** para e-mail inexistente, executa-se verificação contra hash fictício de mesma configuração; limites e bloqueios aplicam-se por chave de e-mail normalizado, independentemente de a conta existir.

## D-49 — Neutralidade tecnológica das Specs

- **Decisão:** `business.md`, `api.md`, `database.md`, `frontend.md`, `security.md`, `tests.md` descrevem **comportamento e contrato**, citando **padrões de protocolo** (HTTP, JSON, JWT, JWKS, OAuth 2.0/OIDC, SMTP, cookies) por serem o contrato com consumidores, mas **não** frameworks, bibliotecas, ORM, SGBD ou ferramentas. Tipos físicos, índices, nomes de classes, versões e ferramentas ficam em `architecture.md`, `data-model.md`, `deployment-operations.md`, `implementation-plan.md`, `decisions.md`.

## D-50 — Fora de escopo explícito

SSO entre aplicações independentes · alternativa completa ao Keycloak · multi-tenancy · federação corporativa (SAML, OIDC enterprise) · Helm/Kubernetes · MFA/TOTP/passkeys · aplicações nativas/móveis como tipo de cliente · webhooks · permissões de negócio · SCIM · consentimento OAuth de terceiros · login por telefone/magic link · exclusão física de contas (há anonimização) · provedores sociais além de Google e Facebook · HSM/KMS · Redis · mensageria externa.

## D-51 — Convenções de JSON

- **Decisão:** todo JSON de requisição e resposta usa **snake_case** (`client_id`, `access_token`, `created_at`), inclusive erros (`trace_id`, `retry_after_seconds`) e a introspecção (RFC 7662). Datas ISO-8601 UTC com `Z`; IDs em UUID textual; booleanos verdadeiros; enumerações em `UPPER_SNAKE_CASE`. Controle otimista via campo `version` no corpo de PATCH.
- **Justificativa:** uma só convenção; compatível com clientes OAuth/OIDC e com o catálogo de claims.
- **Consequência:** os modelos Angular mapeiam snake_case→camelCase em um único adaptador (`core/http`).
