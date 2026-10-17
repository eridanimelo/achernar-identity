# Achernar Identity — Requisitos Funcionais e Não Funcionais

Fonte: prompt do humano de 2026-10-16 (seções §1–§15), decisões `decisions.md`. Situação de todos: `CONFIRMED` (derivados do
prompt) ou `INFERRED` (derivados de decisão para tornar o requisito executável — marcados).
Mapeamento: `FT-xx` = Spec; `UC-xxx` = caso de uso (Discovery); `RN-*` = regras (`business-rules.md`).

## 1. Requisitos funcionais

| ID | Requisito | Origem | Situação | Spec | UCs | Regras principais |
|---|---|---|---|---|---|---|
| RF-01 | Cadastro de conta com e-mail e senha pela aplicação consumidora | §3 | CONFIRMED | FT-02 | UC-010 | RN-ACC-01..07, RN-PWD-01 |
| RF-02 | Verificação de e-mail (com reenvio limitado) | §3 | CONFIRMED | FT-02 | UC-011, UC-012 | RN-ACC-09..11, RN-EML-03 |
| RF-03 | Login com e-mail e senha | §3 | CONFIRMED | FT-03 | UC-020 | RN-AUT-01..11 |
| RF-04 | Login com Google e Facebook (início, callback, troca de código) | §3, §6 | CONFIRMED | FT-07 | UC-050, UC-051, UC-052 | RN-SOC-01..14 |
| RF-05 | Emissão de access token JWT assinado de forma assimétrica | §3, §7 | CONFIRMED | FT-03, FT-10 | UC-020, UC-052, UC-070 | RN-TOK-01, RN-TOK-12..15 |
| RF-06 | Renovação de sessão com refresh token rotativo | §3, §7 | CONFIRMED | FT-03 | UC-021 | RN-TOK-02..08 |
| RF-07 | Logout da sessão atual | §3 | CONFIRMED | FT-03 | UC-022 | RN-AUT-09, RN-TOK-10 |
| RF-08 | Encerramento de todas as sessões | §3 | CONFIRMED | FT-06 | UC-042 | RN-TOK-10, RN-SES-03 |
| RF-09 | Recuperação de senha por e-mail | §3 | CONFIRMED | FT-04 | UC-030, UC-031 | RN-PWD-04..06 |
| RF-10 | Alteração de senha pelo usuário autenticado | §3 | CONFIRMED | FT-04 | UC-032 | RN-PWD-02, RN-PWD-03 |
| RF-11 | Alteração de e-mail com verificação do novo endereço | §3 | CONFIRMED | FT-05 | UC-035, UC-036 | RN-ACC-14, RN-EML-* |
| RF-12 | Consulta e revogação das sessões do usuário | §3 | CONFIRMED | FT-06 | UC-040, UC-041 | RN-SES-01..04 |
| RF-13 | Administração de contas: consulta, ativação e desativação | §3 | CONFIRMED | FT-11 | UC-080, UC-081, UC-082 | RN-ADM-01..08 |
| RF-14 | Registro de eventos relevantes de autenticação e administração | §3 | CONFIRMED | FT-12 | UC-090 + todos | RN-AUD-01..06 |
| RF-15 | Criação do primeiro administrador sem credenciais fixas | §3 | CONFIRMED | FT-01 | UC-001 | RN-SET-01..05 |
| RF-16 | Comportamento definido para e-mail não verificado e conta desativada | §3 | CONFIRMED | FT-02, FT-03, FT-11 | UC-010, UC-020, UC-081 | RN-ACC-09, RN-ACC-12 |
| RF-17 | Criação e vinculação de contas sociais sem vincular por coincidência de e-mail | §3 | CONFIRMED | FT-07, FT-08 | UC-051, UC-053 | RN-SOC-03..08 |
| RF-18 | Senha local para conta social e desvinculação sem perder acesso | §3 | CONFIRMED | FT-04, FT-08 | UC-037, UC-054 | RN-ACC-05, RN-SOC-09..11 |
| RF-19 | Cadastro e configuração de aplicações consumidoras (ids, segredos, origens, URLs de retorno) | §5 | CONFIRMED | FT-09 | UC-060..063 | RN-APP-01..13 |
| RF-20 | Cadastro de APIs consumidoras (audiências) e política de emissão por aplicação | §5, §7 | CONFIRMED | FT-09 | UC-064..067 | RN-APP-14..17 |
| RF-21 | Publicação de chaves públicas (JWKS) e rotação com período de transição | §7 | CONFIRMED | FT-10 | UC-070..072 | RN-TOK-12..13 |
| RF-22 | Bloqueio imediato de tokens via introspecção e obrigações das APIs | §7 | CONFIRMED | FT-10 | UC-073 | RN-TOK-11, RN-TOK-09 |
| RF-23 | E-mails: verificação, recuperação, troca de e-mail e notificações de segurança | §9 | CONFIRMED | FT-13 | UC-095 | RN-EML-01..08 |
| RF-24 | Configuração pública da aplicação para suas telas (provedores, cadastro aberto) | §5 | INFERRED (D-45) | FT-09 | UC-068 | RN-APP-12 |
| RF-25 | Frontend do Identity: telas de autenticação, conta, sessões e administração (aplicações/APIs/chaves/contas/eventos) | §5, §11 | CONFIRMED | FT-14 + telas nas FTs | UC-100 | `ui-map.md` |
| RF-26 | Temas claro e escuro (preferência do sistema + escolha persistida), estados de UI e acessibilidade | §11 | CONFIRMED | FT-14 | UC-100 | RNF-07, RNF-08 |
| RF-27 | Conceder/revogar perfil administrativo | §3 (continuidade do administrador) | INFERRED (D-23) | FT-11 | UC-083 | RN-ADM-03, RN-ADM-04 |
| RF-28 | Anonimização de conta desativada (retenção/LGPD) | §4 (retenção) | INFERRED (D-27) | FT-11 | UC-084 | RN-ACC-13, RN-ADM-06 |
| RF-29 | Retenção e manutenção automática de dados | §4 | CONFIRMED | FT-12 | UC-091, UC-092 | RN-RET-01..06 |
| RF-30 | Reautenticação recente para operações sensíveis | §8 | INFERRED (D-22) | FT-04 | UC-033, UC-055 | RN-PWD-08, RN-SOC-12 |
| RF-31 | Consulta da própria conta (dados, métodos de acesso, vínculos sociais) | §3 | INFERRED | FT-05 | UC-034 | RN-ACC-04 |
| RF-32 | Consulta e rotação manual/emergencial de chaves de assinatura | §7 | INFERRED (D-25) | FT-10 | UC-072 | RN-TOK-12 |
| RF-33 | Consulta administrativa de eventos de segurança | §3 | CONFIRMED | FT-12 | UC-090 | RN-AUD-03 |
| RF-34 | Integração documentada com API monolítica e microsserviços (Spring Security) | §5 | CONFIRMED | `consumer-integration.md` | — | RN-APP-05, RN-TOK-14 |

## 2. Requisitos não funcionais

| ID | Requisito | Meta / critério | Verificação |
|---|---|---|---|
| RNF-01 | Armazenamento seguro de senhas | Argon2id (P-16); nenhuma senha/hash em log ou resposta | teste de revisão + CT-RNF-01 |
| RNF-02 | Desempenho | p95 login ≤ 600 ms; p95 refresh ≤ 150 ms; JWKS p95 ≤ 50 ms; introspecção p95 ≤ 80 ms, com 50 req/s sustentados em 2 vCPU/2 GiB e PostgreSQL local | teste de carga (`test-plan.md` §6) |
| RNF-03 | Disponibilidade | 99,5 % mensal por instalação (1 instância); readiness só `UP` com banco e chave ativa disponíveis | monitoração |
| RNF-04 | Transporte | HTTPS obrigatório em `prod`; HSTS `max-age=31536000`; cookies `Secure` | verificação de deploy |
| RNF-05 | Observabilidade | logs JSON com `traceId`; health liveness/readiness; métricas Prometheus | inspeção |
| RNF-06 | Logs sem segredos | senhas, tokens, `code`, `state`, `Authorization`, cookies, hash e chaves **nunca** logados | teste automatizado de redação (CT-RNF-06) |
| RNF-07 | Acessibilidade | WCAG 2.2 nível AA; navegação 100 % por teclado; foco visível; rótulos/ARIA; contraste ≥ 4.5:1 (texto) e 3:1 (UI) | auditoria + axe |
| RNF-08 | Responsividade | de 320 px a 1920 px sem rolagem horizontal | testes visuais |
| RNF-09 | Internacionalização | `pt-BR` e `en` completos em UI e e-mails | verificação de catálogos |
| RNF-10 | Implantação | Docker Compose reproduzível; configuração por variáveis | `deployment-operations.md` |
| RNF-11 | Qualidade de código | cobertura ≥ 80 % linhas em `application`/`domain`; 100 % dos cenários críticos CT automatizados | pipeline |
| RNF-12 | Backup/restauração | RPO ≤ 24 h; RTO ≤ 4 h; restauração testada trimestralmente | runbook |
| RNF-13 | Compatibilidade | versões conforme D-03; APIs consumidoras Spring Security 7 validam tokens sem código proprietário | CT-INT-* |
| RNF-14 | Escalabilidade horizontal | estado só no PostgreSQL; N instâncias sem sticky session | teste com 2 instâncias |
| RNF-15 | Privacidade | minimização (D-05, D-27); retenção P-50…P-56; anonimização | revisão |
| RNF-16 | Resistência a enumeração e temporização | respostas idênticas (D-13); diferença de tempo médio login existente×inexistente ≤ 15 % | CT-RNF-16 |
| RNF-17 | Resiliência de e-mail | indisponibilidade do SMTP não bloqueia cadastro/recuperação; retry (P-40) | CT-FT13 |
| RNF-18 | Navegadores | baseline do Angular 22 (evergreen: Chrome, Edge, Firefox, Safari — 2 últimas versões estáveis) | testes e2e |
| RNF-19 | Segurança de cabeçalhos | CSP restritiva na UI, `X-Content-Type-Options`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store` em respostas com tokens | CT-SEC-HDR |
| RNF-20 | Idempotência e concorrência | cenários de `test-plan.md` §5 passam sob carga concorrente | testes de concorrência |
