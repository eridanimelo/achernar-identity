# Achernar Identity

Provedor de identidade próprio para **autenticação centralizada e SSO** entre
aplicações, baseado em **OAuth2 e OpenID Connect**.

Organização: Eridani · Versão do projeto: 1.0.0

> **Estado atual:** o MVP está na **fase documental**. As 30 Specs e os artefatos
> compartilhados estão especificados e revisados; a trava do último administrador
> ativo (**RN-13**) está aprovada. A **aprovação final do conjunto (GATE-AUT)** e
> qualquer **implementação** ainda exigem decisão/autorização humana explícita —
> nenhum código do produto foi implementado.

---

## 1. O que é

O Achernar Identity centraliza login, sessões, credenciais e auditoria de
segurança para todas as aplicações da organização:

- **Login centralizado com SSO** — Authorization Code + PKCE (OIDC), access JWT
  de 15 min, refresh opaco rotativo de 7 dias com detecção de reuso, JWKS público
  e rotação de chaves de assinatura.
- **Gestão de usuários e acesso** — cadastro público com verificação de e-mail,
  cadastro administrativo, convites (72 h), bloqueio/desbloqueio, perfis
  `ADMIN`/`USER` e proteção contra lockout administrativo (RN-13).
- **Credenciais** — verificação de e-mail (24 h), recuperação de senha (1 h,
  resposta anti-enumeração), troca autenticada, sempre com auditoria garantida.
- **Sessões e logout** — sessões servidor-side de 8 h, consulta/encerramento pelo
  titular e logout global federado (front/backchannel, best-effort).
- **Auditoria sem perda** — operações críticas gravam negócio + outbox na mesma
  unidade atômica, com relay assíncrono idempotente e consulta administrativa.
- **Clientes OAuth2/OIDC** — registro, consulta, alteração e listagem de
  aplicações clientes (`confidential` com segredo + `public` com PKCE).

Fora do escopo do MVP: MFA, multiempresa/organizações, provedores externos de
login, exclusão física de usuário, Kafka/mensageria externa, Redis e provedor de
e-mail de produção contratado.

## 2. Stack alvo

Definida em `.ia/config/technology.yaml` (fonte normativa; não duplicar valores
neste README):

| Camada | Tecnologia |
|---|---|
| Backend | Java 27 · Spring Boot 4 · Spring Authorization Server · Maven · JPA/Hibernate (`ddl-auto=validate`) · Liquibase |
| Frontend | Angular 22 · Tailwind |
| Banco | PostgreSQL (auditoria em tabela `security_audit_event`, migrações Liquibase) |
| Execução local | Docker Compose (`docker/`, PostgreSQL + MailHog somente local) |
| E-mail local | MailHog (captura); produção via SMTP externo — **pendente de definição humana** |

Sem Redis. Sem HSM/KMS no MVP (chaves cifradas no PostgreSQL com segredo-mestre
externo — **pendente de provisão na implantação**, junto ao segredo de bootstrap).

## 3. Estrutura do repositório

```text
achernar-identity/
├── README.md            ← este arquivo
├── .ia/                 ← motor do framework de trabalho (portátil; abrir a
│                          sessão do agente escopada aqui, não na raiz)
│   ├── AGENTS.md        ← orquestração: intenção → Agent → contexto → gates
│   ├── agents/          ← contratos por intenção (review, backend, …)
│   ├── skills/          ← procedimentos · corporate/ ← regras normativas
│   ├── rules/ · gates/ · workflows/ · config/ ← tecnologia, paths, git…
├── requisitos/          ← documentos-fonte + saída do framework
│   ├── specs/           ← 30 Specs + _shared/ (9 documentos transversais)
│   ├── state/           ← estado do workflow (retomada: current.yaml)
│   ├── reviews/         ← findings e aprovações documentais
│   └── discovery/       ← discovery aprovado (GATE-PLN: PASS)
├── docker/              ← docker-compose.yml + .env.example (execução local)
├── backend/             ← (futuro) código do produto — ainda não criado
└── frontend/            ← (futuro) código do produto — ainda não criado
```

### Documentos de referência

| Documento | Conteúdo |
|---|---|
| `requisitos/specs/_shared/requirements.md` | RF-01…30, RNF-01…12, RN-01…13 |
| `requisitos/specs/_shared/decisions.md` | D-01…D-15 (IA sob delegação aprovada) |
| `requisitos/specs/_shared/architecture.md` | ADR-01…06 (tokens, sessões, chaves, outbox, setup, e-mail) |
| `requisitos/specs/_shared/data-model.md` | Modelo lógico + integridade |
| `requisitos/specs/_shared/security-baseline.md` | Controles BASE-A/Z/T/P/L/U + catálogo de erros |
| `requisitos/specs/_shared/emails.md` | Templates TPL-01…04 (pt-BR, HTML+texto) |
| `requisitos/specs/_shared/test-plan.md` | Estratégia de testes (planejamento, sem execução) |
| `requisitos/specs/_shared/index.md` | Ordem, dependências, Waves e matriz Feature→UC→Spec |
| `requisitos/state/current.yaml` | Estado corrente e próxima etapa (retomada) |
| `requisitos/reviews/` | Revisões documentais e aprovações |

## 4. Ordem de implementação (quando autorizada)

Calculada por dependências reais em `_shared/index.md` — sem ciclos:

- **Wave 0 (bases, em paralelo):** TX-01 auditoria/outbox · TX-02 e-mail/templates · TX-03 chaves/JWKS.
- **Wave 1:** UC-00 setup do primeiro admin → UC-06B/UC-11 → UC-06A/UC-13/UC-14 → UC-01 login → UC-02 refresh.
- **Wave 2:** senha (UC-15/16/17), convites (UC-21A/B), bloqueio (UC-09/10), sessões/logout (UC-04/05/03), gestão (UC-07A/B, UC-08A/B) e clientes (UC-12B-A/B, UC-12).
- **Wave 3:** UC-18 consulta de auditoria e UC-19 demonstração de SSO (prova fim a fim).

**Primeira candidata à futura implementação:** TX-01 (sem dependências).

## 5. Como trabalhar neste repositório

1. Abra a sessão do agente **escopada em `.ia/`** (a raiz não possui `AGENTS.md`
   de propósito — isso mantém a orientação inicial restrita ao motor).
2. O orquestrador classifica sua solicitação em **uma única intenção**, carrega o
   `agents/<intenção>/AGENT.md` correspondente e monta o contexto mínimo.
3. Uma etapa por vez: concluir uma etapa **não** inicia a próxima automaticamente.
4. Implementação de qualquer Spec exige **intenção explícita + GATE-AUT**; este
   README não constitui autorização.
5. Regras de Git em `.ia/config/git.yaml` e `.ia/rules/git/`: **nunca `git push`**;
   implementação ocorre em branch própria a partir de `develop`.

## 6. Pendências conhecidas

- **GATE-AUT** — aprovação humana final do conjunto documental (não equivale a
  autorizar implementação).
- **Resoluções delegadas em documentação** — Q-08B-01 (sessões do rebaixado),
  Q-08A-01 (troca de e-mail em conta bloqueada), Q-07A-01 (allowlist de
  filtros/ordenação) e PV-01 (segredo de bootstrap), com documentação autorizada
  e ainda em elaboração.
- **Pré-implantação** — provedor SMTP de produção (D-09), segredo de bootstrap e
  segredo-mestre de chaves via ambiente. Não contratar serviços nem criar
  credenciais reais nesta fase.
