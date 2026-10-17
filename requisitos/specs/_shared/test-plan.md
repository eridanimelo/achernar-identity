# Achernar Identity — Plano de Testes (especificação; sem execução)

Especifica estratégia e cenários transversais. Cenários por feature: `specs/FT-xx/tests.md` (IDs `CT-FTnn-nn`). Nada foi executado (corporate/testing): sem PASS/FAIL aqui.
Regras: RNF-02, RNF-06, RNF-07, RNF-11, RNF-14, RNF-16, RNF-20.

## 1. Estratégia

| Nível | Escopo | Ferramentas (D-03) | Meta |
|---|---|---|---|
| Unidade | domínio e `application` (política de senha, normalização de e-mail, regras de conta, rotação de refresh, máquina de estados de chaves, validação de URLs) | JUnit 5, AssertJ | ≥ 80 % linhas em `domain`/`application` (RNF-11) |
| Arquitetura | camadas e dependências entre módulos (§2 de `architecture.md`) | ArchUnit | 0 violações |
| Integração (API) | controllers + segurança + PostgreSQL real + e-mail (GreenMail) + provedores simulados (WireMock) | Spring Boot Test, Testcontainers | todos os `api.md` |
| Contrato | respostas conforme `api.md`/`error-catalog.md` (formato, códigos, cabeçalhos) | testes de contrato sobre JSON Schema derivado | 100 % dos endpoints |
| Concorrência | corridas de refresh, consumo de tokens, últimos admins, cadastro duplicado | testes com `ExecutorService`/CountDownLatch sobre PostgreSQL real | §5 |
| Segurança | CSRF, CORS, enumeração, tempo, redação de logs, cabeçalhos, JWT inválidos | testes de integração + checklist OWASP ASVS (V2, V3, V4, V7, V8, V13, V14) | §8 |
| Integração com consumidor | API Spring Security 7 de referência validando tokens reais | projeto de teste descartável em CI (não é produto) | CT-INT-* |
| UI (componente) | serviços/guards/interceptors, formulários, estados | Vitest + Testing Library | cobertura de `core/` ≥ 85 % |
| E2E | fluxos críticos no navegador (Chromium, Firefox, WebKit) | Playwright + MailHog API | §3 |
| Acessibilidade | cada rota nos 2 temas | axe (`@axe-core/playwright`) + roteiro manual de teclado/leitor de tela | 0 violações AA |
| Desempenho | carga de login/refresh/JWKS/introspecção | k6 (ou Gatling) | §6 |
| Operação | migrações, backup/restore, fail-fast de configuração | scripts em CI | CT-OPS-* |

Dados de teste: contas geradas por teste (UUID no e-mail `@example.test`); sem dados reais; relógio controlável (`Clock` injetável) para expiração/rotação; provedores sociais simulados (WireMock/servidor OIDC de teste) — **nenhum teste automatizado depende de Google/Facebook reais**; um roteiro manual pré-produção valida contas reais (A-04).

## 2. Ambientes

| Ambiente | Uso |
|---|---|
| `test` (CI) | Testcontainers; sem Docker Compose |
| `dev` | Compose (PostgreSQL + MailHog) para testes manuais/e2e locais |
| homologação | cópia do `prod` com SMTP de teste e apps sociais de teste |

## 3. Fluxos críticos (cobertura E2E obrigatória)

| ID | Fluxo | Cenários de FT |
|---|---|---|
| CT-E2E-01 | setup do primeiro admin → login → cadastro de API e aplicação | FT-01, FT-09, FT-03 |
| CT-E2E-02 | cadastro → e-mail (MailHog) → verificação → login → acesso a API de negócio de teste | FT-02, FT-03, CT-INT-01 |
| CT-E2E-03 | renovação silenciosa e recarga de página (reidratação por cookie) | FT-03, FT-14 |
| CT-E2E-04 | duas abas renovando simultaneamente | CT-CON-01 |
| CT-E2E-05 | logout propagado entre abas | FT-03, FT-14 |
| CT-E2E-06 | esqueci a senha → redefinir → sessões revogadas → novo login | FT-04 |
| CT-E2E-07 | alterar senha / alterar e-mail com reautenticação | FT-04, FT-05 |
| CT-E2E-08 | listar e revogar sessões; encerrar todas | FT-06 |
| CT-E2E-09 | login social Google (simulado): conta nova, existente vinculada, conflito de e-mail | FT-07 |
| CT-E2E-10 | vincular/desvincular provedor; último método; definir senha local | FT-08, FT-04 |
| CT-E2E-11 | admin desativa conta → sessões caem → introspecção inativa → reativa | FT-11, FT-10 |
| CT-E2E-12 | rotação de chaves sem interrupção (token antigo e novo validam) | FT-10 |
| CT-E2E-13 | tema claro/escuro/sistema e idioma persistem | FT-14 |

## 4. Cenários transversais de segurança e conformidade

| ID | Cenário | Esperado | Refs |
|---|---|---|---|
| CT-SEC-01 | Em `prod`, requisição HTTP simples e cabeçalhos de uma resposta HTTPS | redireciona/recusa HTTP; `Strict-Transport-Security` presente; cookies com `Secure` | SEC-01, RNF-04 |
| CT-SEC-02 | Cadastrar e inspecionar banco/logs/eventos | nenhuma senha em claro; `password_hash` com prefixo do algoritmo Argon2id; nenhum campo `password` em logs/eventos | SEC-02, RNF-01 |
| CT-SEC-06 | 200 logins simultâneos com `ACHERNAR_HASH_CONCURRENCY=2` | memória limitada; requisições excedentes recebem `429/503` em ≤ 2 s sem OOM | SEC-06 |
| CT-SEC-07 | Cadastro/recuperação/reenvio/troca de e-mail/login com e-mail existente × inexistente | corpo, status e cabeçalhos idênticos; diferença de tempo médio ≤ 15 % | SEC-07, RNF-16 |
| CT-SEC-14 | POST em `/auth/refresh` e `/auth/logout` sem `Origin`, com `Origin` de outra aplicação, sem `X-Achernar-Client`, com `Content-Type: text/plain` e via formulário | todos rejeitados (403/415) e sem rotacionar | SEC-14 |
| CT-SEC-22 | Requisição com `Host`/`X-Forwarded-Host` adulterados disparando e-mail | links usam apenas configuração | SEC-22 |
| CT-SEC-26 | Pré-voo e requisições de origem não registrada, de aplicação desativada e de origem registrada em outra aplicação | sem `Access-Control-Allow-Origin` (ou 403) exceto para a origem da própria aplicação; nunca `*` | SEC-26, D-41 |
| CT-SEC-32 | Entradas inválidas/gigantes/Unicode estranho em todos os campos | `400 VALIDATION_ERROR`/`413`; sem 500 | SEC-32 |
| CT-SEC-37 | Provocar erro interno (falha de banco simulada) | 500 genérico com `trace_id`; sem stack trace | SEC-37 |
| CT-SEC-HDR | Verificar cabeçalhos em API e UI | CSP restritiva na UI; `nosniff`; `Referrer-Policy`; `no-store` em respostas sensíveis; sem `Server` detalhado | SEC-23, SEC-35 |
| CT-RNF-01 | Parâmetros do Argon2id em runtime | ≥ P-16; hash ≈ 100–300 ms no ambiente de referência | RNF-01 |
| CT-RNF-06 | Executar todos os fluxos com corpo contendo valores-sentinela (senha `S3NT1N3LA…`, tokens, `code`, `state`) e varrer logs | nenhuma ocorrência dos sentinelas; e-mails mascarados | RNF-06, SEC-31 |
| CT-RNF-16 | 1000 logins com e-mail existente e 1000 inexistente | resposta idêntica; diferença de latência média ≤ 15 % | RNF-16 |
| CT-RNF-14 | Duas instâncias da API sobre o mesmo banco | login numa, refresh na outra funciona; jobs não duplicam | RNF-14 |

## 5. Concorrência (RNF-20) — executar sobre PostgreSQL real

| ID | Cenário | Esperado |
|---|---|---|
| CT-CON-01 | 2 requisições `/auth/refresh` simultâneas com o mesmo refresh | uma recebe novo refresh (`refresh_rotated=true`); a outra recebe só access (`false`); sessão segue ativa; evento `REFRESH_REUSE_TOLERATED` |
| CT-CON-02 | N=20 refresh simultâneos com o mesmo refresh | no máximo 1 rotação; demais tolerados (dentro de P-04) ou, se fora da janela, `INVALID_REFRESH_TOKEN` e sessão revogada; nunca dois refresh correntes |
| CT-CON-03 | Refresh reutilizado após P-04 (relógio avançado) | sessão revogada, `REFRESH_REUSE_DETECTED`, access antigo ainda válido localmente mas introspecção `active=false` |
| CT-CON-04 | Dois cadastros simultâneos do mesmo e-mail | uma conta; ambas as respostas 202; um TPL-01 e um TPL-05 |
| CT-CON-05 | Dois usos simultâneos do mesmo token de verificação/recuperação/troca de e-mail | exatamente um sucesso; o outro `TOKEN_INVALID_OR_EXPIRED` |
| CT-CON-06 | Duas trocas de código social simultâneas | uma sessão criada; a outra `INVALID_GRANT`; a sessão do código é revogada se houver 2º uso após consumo |
| CT-CON-07 | Dois administradores se desativam/rebaixam simultaneamente (restam 2 ADMIN) | no máximo um sucesso; sempre ≥ 1 admin ativo |
| CT-CON-08 | 15 logins simultâneos da mesma conta com limite 10 | ao final ≤ 10 sessões ativas; as mais antigas `SESSION_LIMIT` |
| CT-CON-09 | Dois PATCH simultâneos da mesma aplicação com a mesma `version` | um 200, outro `409 CONFLICT` |
| CT-CON-10 | Rotação de chave durante emissão contínua de tokens | nenhum token com `kid` fora do JWKS; validação OK antes/depois |
| CT-CON-11 | Duas confirmações de troca de e-mail para o mesmo endereço por contas diferentes | uma confirma; a outra falha por unicidade, sem alterar a conta |
| CT-CON-12 | Worker de e-mail em 2 instâncias | cada mensagem enviada uma vez (exceto falha entre envio e commit ⇒ reenvio aceitável) |
| CT-CON-13 | Revogação de sessão concorrente com refresh | resultado consistente: ou refresh vence (e a revogação o invalida em seguida) ou revogação vence (refresh falha) |

## 6. Desempenho (RNF-02) — roteiro k6

| ID | Carga | Critério |
|---|---|---|
| CT-PERF-01 | 50 req/s de `POST /auth/login` por 10 min (contas pré-criadas) | p95 ≤ 600 ms; erro < 0,5 % |
| CT-PERF-02 | 100 req/s de `POST /auth/refresh` | p95 ≤ 150 ms |
| CT-PERF-03 | 500 req/s de `GET /.well-known/jwks.json` | p95 ≤ 50 ms |
| CT-PERF-04 | 100 req/s de introspecção | p95 ≤ 80 ms |
| CT-PERF-05 | Listagem de contas (10⁵ registros) com filtros/páginas | p95 ≤ 300 ms; plano de consulta usa índices |
| CT-PERF-06 | Consulta de eventos com 10⁶ linhas, filtros período+tipo | p95 ≤ 500 ms |

## 7. Acessibilidade e UI

| ID | Cenário | Esperado |
|---|---|---|
| CT-A11Y-01 | axe em todas as rotas × 2 temas × `xs` e `lg` | 0 violações AA |
| CT-A11Y-02 | Percorrer cada fluxo só com teclado | todas as ações alcançáveis; foco visível; ordem lógica; diálogos prendem e devolvem o foco |
| CT-A11Y-03 | Leitor de tela (NVDA/VoiceOver) em login, cadastro, segurança, sessões | rótulos, erros e anúncios `aria-live` compreensíveis |
| CT-A11Y-04 | Zoom 200 %/400 % e largura 320 px | sem perda de função nem rolagem horizontal da página |
| CT-A11Y-05 | `prefers-reduced-motion` | animações removidas |
| CT-UI-01 | Alternar tema, recarregar, abrir em outra aba | escolha persiste; “Sistema” acompanha o SO sem recarregar |
| CT-UI-02 | Trocar idioma | textos, `lang` e formatos trocam; e-mails seguem o `locale` da conta |

## 8. Integração com consumidor (RN-TOK-14, RF-34)

| ID | Cenário | Esperado |
|---|---|---|
| CT-INT-01 | API de teste Spring Security 7 (config de `consumer-integration.md §4`) recebe token válido da audiência dela | 200; `sub` acessível |
| CT-INT-02 | Token com `aud` de outra API | 401 |
| CT-INT-03 | Token expirado / `nbf` futuro (> skew) / `iss` errado / assinatura adulterada / `alg=none` / `alg=HS256` com chave pública como segredo / `typ` ≠ `at+jwt` | 401 em todos |
| CT-INT-04 | Rotação de chave: token assinado pela nova chave é aceito sem reiniciar a API (refetch por `kid`); token da chave antiga aceito até expirar | 200 |
| CT-INT-05 | Dois serviços (monolito e relatórios) aceitando o **mesmo** token multi-audiência | ambos 200; um terceiro serviço sem a audiência ⇒ 401 |
| CT-INT-06 | Introspecção de sessão revogada/conta desativada | `active=false` na hora; a API em modo estrito nega a operação sensível |
| CT-INT-07 | Identity indisponível | API continua aceitando tokens válidos (validação local); operação sensível falha fechada |
| CT-INT-08 | Provisionamento preguiçoso: `PUT /me/perfil` duas vezes (repetição) | um único registro por `sub` |

## 9. Operação, migração e backup (RNF-12)

| ID | Cenário | Esperado |
|---|---|---|
| CT-OPS-01 | Aplicar changelog em banco vazio e validar contra JPA (`validate`) | sucesso |
| CT-OPS-02 | Migrar a partir do esquema da versão anterior com dados | sucesso; dados preservados |
| CT-OPS-03 | Backup → restauração em ambiente descartável → runbook pós-restauração | login funciona; nenhuma sessão antiga válida; JWKS íntegro |
| CT-OPS-04 | Subir `prod` com parâmetro inválido / sem master key / issuer http / senha de banco padrão | falha de subida com mensagem clara, sem expor segredos |
| CT-OPS-05 | Papel `app` tenta `UPDATE security_event SET event_type=…` e `DROP TABLE` | negado; `UPDATE (ip,user_agent)` permitido |
| CT-OPS-06 | Repositório e imagens: varredura de segredos | nenhum segredo/credencial real |
| CT-OPS-07 | Health: derrubar PostgreSQL / remover chave ativa | readiness DOWN; liveness UP |
| CT-OPS-08 | Job de retenção com dados sintéticos de idades variadas | apenas dados fora da retenção são removidos; evento `RETENTION_PURGE_RUN` |

## 10. Critérios de saída (por Wave)

Todos os CT de FT da Wave definidos + CT transversais aplicáveis implementados e **executados com PASS**; cobertura ≥ metas; 0 violações de arquitetura; 0 vulnerabilidades altas/críticas conhecidas em dependências; revisão de segurança dos controles SEC aplicáveis (`GATE-QLT`).

## 9. Cobertura dos requisitos não funcionais (RNF → verificação)

| RNF | Verificação |
|---|---|
| RNF-01 | CT-RNF-01; CT-FT04-18; CT-FT03-10 |
| RNF-02 | CT-PERF-01…06; CT-FT10-17; CT-FT12-22 |
| RNF-03 | CT-OPS-07 (readiness/liveness) e monitoração de disponibilidade descrita em `deployment-operations.md` (meta mensal medida em operação) |
| RNF-04 | CT-OPS-04 (issuer `http` em `prod` falha a subida); verificação de HSTS/cookies `Secure` em `deployment-operations.md` |
| RNF-05 | CT-FT13-14; CT-OPS-07 |
| RNF-06 | CT-RNF-06; CT-FT01-14; CT-FT02-18; CT-FT03-23 |
| RNF-07 | CT-A11Y-*; CT-FT14-12 |
| RNF-08 | CT-FT14-13 |
| RNF-09 | CT-FT14-09; CT-FT13-08 |
| RNF-10 | CT-OPS-01…06 (subida reproduzível do ambiente por Compose e variáveis) |
| RNF-11 | critério de pipeline (cobertura ≥ 80 %; cenários críticos automatizados) — `implementation-plan.md` |
| RNF-12 | CT-OPS-03 |
| RNF-13 | CT-INT-01…05; CT-FT10-20 |
| RNF-14 | CT-CON-12; CT-FT10-04; CT-FT12-18 |
| RNF-15 | CT-FT06-11; CT-FT12-12 |
| RNF-16 | CT-RNF-16; CT-FT02-03; CT-FT03-03 |
| RNF-17 | CT-FT13-02; CT-FT02-15 |
| RNF-18 | CT-E2E-01…13 executados nos navegadores do baseline (Chrome, Edge, Firefox, Safari — 2 últimas versões estáveis) |
| RNF-19 | CT-SEC-HDR; CT-FT14-16 |
| RNF-20 | CT-CON-01…13; CT-FT03-20; CT-FT01-11 |

