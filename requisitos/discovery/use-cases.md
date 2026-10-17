---
name: discovery-use-cases
type: discovery
description: "Discovery — Inventário Funcional — Achernar Identity"
---

# Discovery — Inventário Funcional

Origem de todos: `NEW`. “Evidência/Origem” = requisito (RF), decisão (D) do `specs/_shared`. Operações plausíveis **não** pedidas foram
descartadas ou marcadas (ver *Operações avaliadas e não incluídas*). Atores: VIS (visitante), USR (usuário autenticado), ADM (administrador),
APP (aplicação consumidora chamando a API), API (API consumidora), SIS (sistema/job), OPE (operador).

## Features identificadas

| ID | Feature | Objetivo | Módulo | Origem/Evidência | Status |
|---|---|---|---|---|---|
| FT-01 | Setup do primeiro administrador | Criar o primeiro ADMIN sem credencial fixa | M1 | RF-15, D-24 | APPROVED |
| FT-02 | Cadastro e verificação de e-mail | Criar conta local e confirmar o e-mail | M2 | RF-01, RF-02, RF-16 | APPROVED |
| FT-03 | Autenticação por senha e tokens | Login, refresh rotativo e logout | M3 | RF-03, RF-05..07 | APPROVED |
| FT-04 | Recuperação e gestão de senha | Recuperar, redefinir, alterar, definir senha local, reautenticar | M4 | RF-09, RF-10, RF-18, RF-30 | APPROVED |
| FT-05 | Conta e alteração de e-mail | Consultar conta e trocar e-mail com verificação | M4 | RF-11, RF-31 | APPROVED |
| FT-06 | Sessões do usuário | Listar, revogar e encerrar todas | M5 | RF-08, RF-12 | APPROVED |
| FT-07 | Login social | Início, callback e troca de código (Google/Facebook) | M8 | RF-04, RF-17 | APPROVED |
| FT-08 | Vínculos sociais | Vincular, desvincular, reautenticar via provedor | M8 | RF-17, RF-18, RF-30 | APPROVED |
| FT-09 | Registro de aplicações e APIs | Cadastro/config. de aplicações e APIs, segredos, config pública | M6 | RF-19, RF-20, RF-24 | APPROVED |
| FT-10 | Chaves, JWKS e introspecção | Publicar chaves, rotacionar, introspectar | M7 | RF-05, RF-21, RF-22, RF-32 | APPROVED |
| FT-11 | Administração de contas | Consultar, desativar, reativar, perfil ADMIN, anonimizar | M9 | RF-13, RF-27, RF-28 | APPROVED |
| FT-12 | Eventos de segurança e retenção | Consultar eventos; jobs de retenção/manutenção | M11 | RF-14, RF-29, RF-33 | APPROVED |
| FT-13 | E-mails transacionais | Outbox, templates, retry, notificações | M10 | RF-23 | APPROVED |
| FT-14 | Interface base, tema e sessão no navegador | Shell, tema claro/escuro, i18n, sessão em memória + refresh por cookie | M12 | RF-25, RF-26 | APPROVED |

## Casos de uso

Legenda: Compl./Risco/Valor ∈ L, M, H. Telas = `SCR-xx` (ver `ui-map.md`).

| ID | Feature | Caso de uso | Ator | Telas/Entradas | Dependências | Compl. | Risco | Valor | Integrações | Reuso potencial | Evidência/Origem | Status |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| UC-001 | FT-01 | Criar o primeiro administrador | VIS/OPE | SCR-01; `POST /setup/admin` | FND-01, FND-02, UC-060 (serviço) | M | H | H | — | política de senha | RF-15, D-24 | APPROVED |
| UC-010 | FT-02 | Cadastrar conta com e-mail e senha | VIS | SCR-02, SCR-05; `POST /auth/register` | FND-01..04, UC-060 | M | M | H | SMTP | anti-enumeração, política de senha | RF-01 | APPROVED |
| UC-011 | FT-02 | Verificar e-mail | VIS | SCR-03; `POST /auth/email/verify` | UC-010 | L | M | H | — | token de uso único | RF-02 | APPROVED |
| UC-012 | FT-02 | Reenviar verificação de e-mail | VIS | SCR-04; `POST /auth/email/resend-verification` | UC-010, FND-03 | L | L | M | SMTP | — | RF-02 | APPROVED |
| UC-020 | FT-03 | Autenticar com e-mail e senha | VIS | SCR-06; `POST /auth/login` | UC-011, UC-060, UC-070, FND-03 | H | H | H | — | emissão de tokens | RF-03, RF-05 | APPROVED |
| UC-021 | FT-03 | Renovar sessão com refresh token | APP/USR | `POST /auth/refresh` | UC-020 | H | H | H | — | — | RF-06 | APPROVED |
| UC-022 | FT-03 | Encerrar a sessão atual (logout) | APP/USR | `POST /auth/logout` | UC-020 | L | L | H | — | — | RF-07 | APPROVED |
| UC-030 | FT-04 | Solicitar recuperação de senha | VIS | SCR-07; `POST /auth/password/forgot` | UC-011, FND-03, FND-04 | M | M | H | SMTP | anti-enumeração | RF-09 | APPROVED |
| UC-031 | FT-04 | Redefinir senha com token | VIS | SCR-08; `POST /auth/password/reset` | UC-030, UC-041 (serviço) | M | H | H | — | política de senha | RF-09 | APPROVED |
| UC-032 | FT-04 | Alterar senha (autenticado) | USR | SCR-10; `POST /api/v1/account/password` | UC-020, UC-033 | M | M | H | SMTP | — | RF-10 | APPROVED |
| UC-033 | FT-04 | Reautenticar com senha | USR | SCR-11; `POST /api/v1/account/reauth` | UC-020 | M | M | H | — | — | RF-30 | APPROVED |
| UC-037 | FT-04 | Definir senha local em conta social | USR | SCR-13; `POST /api/v1/account/password` | UC-055, UC-051 | M | M | M | SMTP | política de senha | RF-18 | APPROVED |
| UC-034 | FT-05 | Consultar minha conta | USR | SCR-09; `GET /api/v1/account` | UC-020 | L | L | H | — | — | RF-31 | APPROVED |
| UC-035 | FT-05 | Solicitar alteração de e-mail | USR | SCR-10; `POST /api/v1/account/email-change` | UC-033, UC-034, FND-04 | M | M | M | SMTP | anti-enumeração | RF-11 | APPROVED |
| UC-036 | FT-05 | Confirmar alteração de e-mail | USR/VIS | SCR-12; `POST /auth/email-change/confirm` | UC-035 | M | M | M | SMTP | token de uso único | RF-11 | APPROVED |
| UC-040 | FT-06 | Listar minhas sessões | USR | SCR-14; `GET /api/v1/sessions` | UC-020 | L | L | M | — | — | RF-12 | APPROVED |
| UC-041 | FT-06 | Revogar uma sessão | USR | SCR-14; `DELETE /api/v1/sessions/{id}` | UC-040 | L | M | M | — | serviço de revogação | RF-12 | APPROVED |
| UC-042 | FT-06 | Encerrar todas as sessões | USR | SCR-14; `DELETE /api/v1/sessions` | UC-041, UC-033 | M | M | M | SMTP | — | RF-08 | APPROVED |
| UC-050 | FT-07 | Iniciar login/cadastro social | VIS | SCR-15; `GET /auth/social/{provider}/start` | UC-060, UC-068, FND-03 | M | H | H | Google, Facebook | — | RF-04 | APPROVED |
| UC-051 | FT-07 | Processar retorno do provedor (callback) | VIS | `GET /auth/social/{provider}/callback` | UC-050, UC-010, UC-011 | H | H | H | Google, Facebook | criação de conta | RF-04, RF-17 | APPROVED |
| UC-052 | FT-07 | Concluir login social (trocar código) | APP | SCR-16; `POST /auth/social/exchange` | UC-051, UC-070 | M | H | H | — | emissão de tokens | RF-04, RF-05 | APPROVED |
| UC-053 | FT-08 | Vincular provedor social à conta | USR | SCR-17; `POST /api/v1/account/social-links/{p}/intent` + fluxo | UC-051, UC-033 | H | H | M | Google, Facebook | fluxo UC-050/051 | RF-17 | APPROVED |
| UC-054 | FT-08 | Desvincular provedor social | USR | SCR-17; `DELETE /api/v1/account/social-links/{p}` | UC-053 | M | M | M | SMTP | — | RF-18 | APPROVED |
| UC-055 | FT-08 | Reautenticar com provedor social | USR | SCR-11; fluxo `REAUTH` | UC-051, UC-053 | M | M | M | Google, Facebook | fluxo UC-050/051 | RF-30 | APPROVED |
| UC-060 | FT-09 | Cadastrar aplicação consumidora | ADM | SCR-25; `POST /api/v1/admin/applications` | FND-01, FND-02 | M | H | H | — | — | RF-19 | APPROVED |
| UC-061 | FT-09 | Consultar aplicações | ADM | SCR-24, SCR-25; `GET …/applications` | UC-060 | L | L | M | — | — | RF-19 | APPROVED |
| UC-062 | FT-09 | Alterar aplicação (inclui ativar/desativar) | ADM | SCR-25; `PATCH …/applications/{id}` | UC-060 | M | M | H | — | — | RF-19 | APPROVED |
| UC-063 | FT-09 | Rotacionar segredo da aplicação | ADM | SCR-25, SCR-26; `POST …/secrets` | UC-060 | M | H | M | — | — | RF-19 | APPROVED |
| UC-064 | FT-09 | Cadastrar API consumidora | ADM | SCR-28; `POST …/apis` | FND-01 | M | M | H | — | — | RF-20 | APPROVED |
| UC-065 | FT-09 | Consultar APIs | ADM | SCR-27, SCR-28 | UC-064 | L | L | M | — | — | RF-20 | APPROVED |
| UC-066 | FT-09 | Alterar API (inclui ativar/desativar) | ADM | SCR-28 | UC-064 | M | M | M | — | — | RF-20 | APPROVED |
| UC-067 | FT-09 | Rotacionar credencial de introspecção | ADM | SCR-28, SCR-26 | UC-064 | M | H | M | — | — | RF-22 | APPROVED |
| UC-068 | FT-09 | Consultar configuração pública da aplicação | VIS/APP | `GET /auth/applications/{client_id}/config` | UC-060 | L | L | M | — | — | RF-24 | APPROVED |
| UC-070 | FT-10 | Publicar chaves públicas (JWKS e metadados) | API/APP | `GET /.well-known/jwks.json`, metadados | FND-01 | L | M | H | — | — | RF-05, RF-21 | APPROVED |
| UC-071 | FT-10 | Rotacionar chaves automaticamente | SIS | job | UC-070 | H | H | H | — | — | RF-21 | APPROVED |
| UC-072 | FT-10 | Consultar e rotacionar chaves (manual/emergencial) | ADM | SCR-29; `…/admin/signing-keys` | UC-070, UC-001 | M | H | M | — | — | RF-32 | APPROVED |
| UC-073 | FT-10 | Introspectar token (bloqueio imediato) | API | `POST /api/v1/tokens/introspect` | UC-064, UC-070, UC-020 | M | H | H | — | — | RF-22 | APPROVED |
| UC-080 | FT-11 | Consultar contas (lista e detalhe) | ADM | SCR-18, SCR-19 | UC-001, UC-020 | M | L | H | — | — | RF-13 | APPROVED |
| UC-081 | FT-11 | Desativar conta | ADM | SCR-19, SCR-20 | UC-080, UC-041 (serviço) | M | H | H | SMTP | — | RF-13 | APPROVED |
| UC-082 | FT-11 | Reativar conta | ADM | SCR-19, SCR-20 | UC-081 | L | M | M | SMTP | — | RF-13 | APPROVED |
| UC-083 | FT-11 | Conceder/revogar perfil administrador | ADM | SCR-19, SCR-20 | UC-080 | M | H | M | — | — | RF-27 | APPROVED |
| UC-084 | FT-11 | Anonimizar conta desativada | ADM | SCR-19, SCR-20 | UC-081 | M | H | M | — | — | RF-28 | APPROVED |
| UC-090 | FT-12 | Consultar eventos de segurança | ADM | SCR-21; `GET …/security-events` | FND-02, UC-001 | M | L | H | — | — | RF-33 | APPROVED |
| UC-091 | FT-12 | Executar retenção de dados | SIS | job diário | FND-02 | M | M | M | — | — | RF-29 | APPROVED |
| UC-092 | FT-12 | Executar manutenção periódica (pendentes, tokens, outbox, contadores) | SIS | jobs | FND-03, FND-04 | M | M | M | — | — | RF-29 | APPROVED |
| UC-095 | FT-13 | Enviar e-mails transacionais | SIS | outbox/worker | FND-01 | M | M | H | SMTP | — | RF-23 | APPROVED |
| UC-100 | FT-14 | Prover shell, tema, i18n e sessão no navegador | USR/VIS | SCR-22, 23, 30, 31 | FND-05 | M | M | H | — | — | RF-25, RF-26 | APPROVED |

## Operações avaliadas e não incluídas

| Operação plausível | Decisão | Motivo |
|---|---|---|
| Admin criar conta de usuário / forçar reset de senha | Fora | Não solicitado (§3 lista consulta/ativação/desativação); cadastro é autosserviço |
| Admin ver/encerrar sessões individuais de outra conta | Fora | Desativação já revoga todas; evita ampliar superfície |
| Exclusão física de conta | Fora | Substituída por anonimização (UC-084) |
| Webhooks de eventos de conta para aplicações | Fora | D-28 (provisionamento preguiçoso + introspecção) |
| Editar perfil (nome, foto) | Fora | Dados de perfil/negócio pertencem às aplicações (§4) |
| MFA/passkeys | Fora | Não solicitado (D-50) |
| Exclusão de aplicação/API | Fora | Somente desativar (RN-APP-13) |
| Consulta de eventos pelo próprio usuário | `REQUIRES_REVIEW` → rejeitada | Não solicitado; notificações por e-mail cobrem o aviso |
