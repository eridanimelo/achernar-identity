---
name: discovery-system
type: discovery
description: "Discovery — System Inventory — Achernar Identity"
---

# Discovery — System Inventory

## Identificação

- Projeto: **Achernar Identity** (organização Eridani)
- Origens presentes: `NEW` (todas as features; não há sistema anterior — o repositório contém apenas o framework `.ia/` e a configuração Docker)
- Fonte de requisitos: `BUSINESS` (`config/PROJECT.yaml`); o documento-fonte é o **prompt “Achernar Identity — Discovery e especificações completas” de 2026-10-16**, que prevalece sobre `README.md`/`PROJECT.yaml` antigos (DEV-03 em `decisions.md`)
- Data do inventário: 2026-10-17
- Status: `APPROVED` por autorização prévia do humano (§13 do prompt); sem revisão humana adicional exigida. Decisões e premissas atribuídas à IA em `specs/_shared/decisions.md`.

## Resumo

- Quantidade de módulos/domínios: **12** (M1–M12) + 6 fundações técnicas (FND-01…06)
- Quantidade de telas conhecidas: **31** (ver `specs/_shared/ui-map.md`)
- Quantidade de casos de uso: **47** (UC-001 … UC-100) em **14 features/Specs** (FT-01…FT-14)
- Quantidade de integrações: **6** (Google, Facebook, SMTP, aplicações `BROWSER`, aplicações `CONFIDENTIAL`, APIs consumidoras)
- Quantidade de pontos `UNKNOWN` relevantes: **0 críticos**; 7 premissas de implantação listadas em *Lacunas* (todas viram parâmetros)

## Contexto

Cada projeto freelancer opera sua instalação do Identity (D-01) com base de contas própria. O Identity cuida de **contas, autenticação e sessões**;
as aplicações consumidoras cuidam dos dados e permissões de negócio e vinculam-se ao Identity **apenas pelo `sub`**. O produto oferece uma API de
autenticação para as **telas das próprias aplicações** (cadastro, login, recuperação), login com Google e Facebook, emissão de JWT assinado
assimetricamente com publicação de chaves (JWKS), sessões renováveis com rotação, e uma interface própria (conta, sessões, administração).

Fora do escopo (D-50): SSO entre aplicações independentes, alternativa completa ao Keycloak, multi-tenancy, federação corporativa, Helm/Kubernetes,
MFA, apps nativas, webhooks.

Atores: **Visitante**, **Usuário** (conta autenticada), **Administrador**, **Aplicação consumidora** (frontend `BROWSER` ou backend `CONFIDENTIAL`),
**API consumidora** (resource server), **Provedor social** (Google, Facebook), **Operador** (implantação/backup), **Sistema** (jobs).

## Módulos / Domínios

| Módulo/Domínio | Origem | Telas | Casos de uso | Dependências | Observações |
|---|---|---:|---:|---|---|
| M1 Setup e primeiro administrador | NEW | 1 | 1 | M6 | sem credencial fixa |
| M2 Contas e cadastro | NEW | 4 | 3 | M6, M10 | verificação de e-mail |
| M3 Autenticação e tokens | NEW | 2 | 3 | M2, M6, M7 | login, refresh, logout |
| M4 Credenciais e conta | NEW | 7 | 8 | M3, M10 | senha, reautenticação, troca de e-mail |
| M5 Sessões | NEW | 1 | 3 | M3 | listar, revogar, encerrar todas |
| M6 Aplicações e APIs consumidoras | NEW | 5 | 9 | — | cadastro, origens, audiências, segredos |
| M7 Chaves, JWKS e introspecção | NEW | 1 | 4 | — | rotação, bloqueio imediato |
| M8 Login social | NEW | 3 | 6 | M2, M3, M6 | Google/Facebook; vínculos |
| M9 Administração de contas | NEW | 3 | 5 | M3, M5 | desativar, perfil admin, anonimizar |
| M10 E-mails transacionais | NEW | 0 | 1 | — | outbox, templates, retry |
| M11 Eventos de segurança e retenção | NEW | 1 | 3 | todos | auditoria, jobs |
| M12 Interface base | NEW | 3 | 1 | M3 | shell, tema, sessão no navegador |

## Integrações

- Google (OpenID Connect) e Facebook (OAuth 2.0 + Graph API) — somente login/identidade.
- Provedor SMTP (qualquer; MailHog em dev).
- Aplicações `BROWSER` (SPA do projeto) e `CONFIDENTIAL` (BFF/servidor); APIs consumidoras (monolito e microsserviços) que validam JWT via JWKS e opcionalmente fazem introspecção.

## Persistência

Contas, credenciais, vínculos sociais, sessões e refresh tokens (hash), tokens de uso único (hash), transações e códigos do login social, registro de
aplicações/APIs/segredos (hash), chaves de assinatura (privadas cifradas), eventos de segurança, outbox de e-mail, contadores de limite de taxa.
Dados de negócio **não** são persistidos (D-28).

## Riscos gerais

| ID | Risco | Mitigação |
|---|---|---|
| RISK-01 | Roubo de refresh token / XSS no SPA | cookie HttpOnly, rotação, detecção de reuso, CSP, TTL curto (D-06/D-07) |
| RISK-02 | Aplicação em site diferente do Identity quebra o modelo de cookie | premissa A-01 + BFF `CONFIDENTIAL` (D-07/D-08) |
| RISK-03 | Access token vive após revogação | TTL 10 min + introspecção para operações sensíveis (D-10) |
| RISK-04 | Conta sequestrada via login social/pré-sequestro | sem vínculo automático; substituição de conta pendente; reautenticação (D-20) |
| RISK-05 | Perda da chave-mestra / backup restaurado revive sessões revogadas | runbook: revogar todas as sessões após restore; guardar a chave-mestra separadamente |
| RISK-06 | Dependência de política dos provedores (revisão do app Google/Facebook) | parâmetros; testes manuais pré-produção; mensagens de falha graciosas |
| RISK-07 | Bloqueio por tentativas usado como DoS | teto 120 min, recuperação de senha ignora o bloqueio, aviso por e-mail |
| RISK-08 | Versão de Java/Spring/Angular desatualiza | pin de versões, calendário de revisão (D-03) |

## Lacunas / Decisões pendentes

Nenhuma bloqueia a implementação. Premissas de implantação (viram parâmetros, sem inventar valores reais):

| ID | Premissa | Tratamento |
|---|---|---|
| A-01 | Aplicações `BROWSER` e Identity compartilham o mesmo site (domínio registrável) | documentado; BFF para exceções |
| A-02 | Provedor SMTP de produção ainda não definido | P-42/P-43 |
| A-03 | Domínios reais e certificados TLS | P-60..P-62; Caddy automático |
| A-04 | Credenciais Google/Facebook (apps OAuth criados pelo operador) | P-70..P-72 |
| A-05 | Logo oficial inexistente | proposta visual provisória (D-38) |
| A-06 | Chave-mestra e token de bootstrap gerados no deploy | P-65/P-66 |
| A-07 | Janela de manutenção para migrações em produção | `deployment-operations.md` |
