---
name: dependency-map
type: discovery
description: "Discovery — Dependency Map — Achernar Identity"
---

# Discovery — Dependency Map

## Dependências entre casos de uso e fundações

Tipos: `FUNCTIONAL`, `DATA`, `TECHNICAL`, `INTEGRATION`. Todas as dependências são `Bloqueante: YES` salvo indicação.

| Origem | Depende de | Tipo | Bloqueante | Evidência | Motivo | Impacto na ordem/Wave |
|---|---|---|---|---|---|---|
| UC-060 | FND-01, FND-02 | TECHNICAL | YES | D-30, D-26 | persistência/erros/auditoria | Wave 0 antes de 1 |
| UC-001 | UC-060 (serviço de aplicação) | DATA | YES | RN-SET-05 | semeia a aplicação da UI | UC-060 antes de UC-001 |
| UC-010 | FND-03, FND-04, UC-060 | TECHNICAL/DATA | YES | RN-ACC-07, RN-EML-02 | limite de taxa, e-mail, `client_id` | Wave 1 |
| UC-011 | UC-010 | FUNCTIONAL | YES | RN-ACC-10 | token vem do cadastro | — |
| UC-012 | UC-010 | FUNCTIONAL | YES | RN-ACC-09 | reenvia verificação | — |
| UC-020 | UC-011, UC-060, UC-070, FND-03 | FUNCTIONAL/DATA | YES | RN-AUT-01 | conta ativa, cliente, chave de assinatura | Wave 1 |
| UC-021, UC-022 | UC-020 | FUNCTIONAL | YES | RN-TOK-02 | sessão e refresh nascem no login | — |
| UC-030 | UC-011, FND-03, FND-04 | FUNCTIONAL/TECHNICAL | YES | RN-PWD-04 | existe conta ativa | Wave 2 |
| UC-031 | UC-030, serviço de revogação de sessões (UC-041) | FUNCTIONAL | YES | RN-PWD-05 | reset revoga sessões | revogação de sessões implementada antes |
| UC-033 | UC-020 | FUNCTIONAL | YES | RN-PWD-08 | sessão com `last_reauth_at` | — |
| UC-032 | UC-020, UC-033 | FUNCTIONAL | YES | RN-PWD-02 | senha atual/reauth | — |
| UC-034 | UC-020 | FUNCTIONAL | YES | RN-ACC-04 | identifica conta | Wave 1 |
| UC-035 | UC-033, UC-034, FND-04 | FUNCTIONAL | YES | RN-ACC-14 | reauth + e-mail | — |
| UC-036 | UC-035 | FUNCTIONAL | YES | RN-ACC-14 | confirma solicitação | — |
| UC-040 → UC-041 → UC-042 | UC-020; UC-040; UC-041+UC-033 | FUNCTIONAL | YES | RN-SES-* | sequência de gestão de sessões | Wave 2 |
| UC-050 | UC-060, UC-068, FND-03 | DATA | YES | RN-SOC-02 | valida cliente/provedor | Wave 3 |
| UC-051 | UC-050, UC-010 (regras de conta), UC-011 | FUNCTIONAL | YES | RN-SOC-04 | cria conta/pendente | — |
| UC-052 | UC-051, UC-070 | FUNCTIONAL/TECHNICAL | YES | RN-SOC-14 | emite tokens | — |
| UC-053 | UC-051, UC-033, UC-034 | FUNCTIONAL | YES | RN-SOC-07 | reuso do fluxo + reauth | — |
| UC-054 | UC-053 | FUNCTIONAL | YES | RN-SOC-09 | só desvincula o que existe | — |
| UC-055 | UC-051, UC-053 | FUNCTIONAL | YES | RN-SOC-12 | exige vínculo existente | — |
| UC-037 | UC-055, UC-051 | FUNCTIONAL | YES | RN-PWD-07 | conta social + reauth social | — |
| UC-051 | Google/Facebook | INTEGRATION | YES | §6 | identidade vem do provedor | credenciais da implantação (A-04) |
| UC-061..063 | UC-060 | FUNCTIONAL | YES | — | gestão sobre cadastro | — |
| UC-065..067 | UC-064 | FUNCTIONAL | YES | — | idem | — |
| UC-068 | UC-060 | DATA | YES | RN-APP-12 | lê config | Wave 1 (para UI) |
| UC-071 | UC-070 | TECHNICAL | YES | RN-TOK-12 | rotação sobre armazenamento de chaves | Wave 0 |
| UC-072 | UC-070, UC-001 | FUNCTIONAL | YES | RN-ADM-01 | admin autenticado | — |
| UC-073 | UC-064, UC-070, UC-020 | FUNCTIONAL | YES | RN-TOK-11, RN-APP-16 | credencial da API + sessão | — |
| UC-080 | UC-001, UC-020 | FUNCTIONAL | YES | RN-ADM-01 | admin autenticado | Wave 2 |
| UC-081 | UC-080, serviço de revogação (UC-041) | FUNCTIONAL | YES | RN-ADM-02 | revoga sessões | — |
| UC-082 | UC-081 | FUNCTIONAL | YES | RN-ADM-05 | só reativa o desativado | — |
| UC-083 | UC-080 | FUNCTIONAL | YES | RN-ADM-03 | — | — |
| UC-084 | UC-081 | FUNCTIONAL | YES | RN-ACC-13 | só desativadas | Wave 4 |
| UC-090 | FND-02, UC-001 | DATA | YES | RN-AUD-03 | lê eventos gravados | Wave 2 |
| UC-091, UC-092 | FND-02, FND-03, FND-04 | TECHNICAL | NO | RN-RET-01 | dependem dos dados a purgar | Wave 4 |
| UC-095 | FND-01 | TECHNICAL | YES | D-18 | fundação | Wave 0 |
| UC-100 | FND-05 | TECHNICAL | YES | D-31 | fundação | Wave 0 |
| Todos os UCs de UI | UC-100 | TECHNICAL | YES | D-31 | shell/tema/sessão | — |

**Ciclo aparente resolvido:** UC-001 (admin) × UC-060 (cadastrar aplicação via API de admin). A semeadura da aplicação da UI usa o **serviço de
domínio** de UC-060, não o endpoint; o endpoint de administração fica disponível depois que UC-001 + UC-020 existem. Sem ciclo real.

## Cadeias relevantes

```text
FND-01 ─▶ UC-060 ─▶ UC-001 ─▶ UC-010 ─▶ UC-011 ─▶ UC-020 ─▶ UC-021 ─▶ UC-022
                                                      │
                                                      ├─▶ UC-033 ─▶ UC-032 / UC-035 ─▶ UC-036
                                                      ├─▶ UC-040 ─▶ UC-041 ─▶ UC-042
                                                      └─▶ UC-073 (com UC-064)
FND-06 (UC-070,071) ─▶ UC-020 / UC-052 / UC-073
UC-050 ─▶ UC-051 ─▶ UC-052 ─▶ UC-053 ─▶ UC-054 / UC-055 ─▶ UC-037
UC-081 ─▶ UC-082 / UC-084
```

## Casos fundacionais

| Caso de uso | Motivo | Habilita |
|---|---|---|
| FND-01 | persistência, erros, logs, config | todos |
| UC-070/071 (FND-06) | sem chave não há token | UC-020, UC-052, UC-073 |
| UC-060 | cliente registrado | UC-001, UC-010, UC-020, UC-050 |
| UC-095 (FND-04) | e-mails | UC-010, UC-012, UC-030, UC-035 |
| FND-02 | auditoria | todos que mudam estado |
| FND-03 | limites | UC-010, UC-012, UC-020, UC-030, UC-050 |
| UC-020 | sessão autenticada | todo o autosserviço e administração |

## Casos independentes (podem avançar em paralelo após Wave 0)

UC-064/UC-065/UC-066 (APIs), UC-068, UC-072 parcial, UC-100 (UI shell) e as telas de UC-060..063 do lado Angular.

## Dependências externas

| Caso de uso | Sistema/Serviço | Tipo | Bloqueante | Impacto |
|---|---|---|---|---|
| UC-010/012/030/035 | SMTP (MailHog em dev) | INTEGRATION | NO (fila com retry) | produção depende de A-02 |
| UC-050..055 | Google OAuth/OIDC | INTEGRATION | YES para a Wave 3 em ambiente real | credenciais A-04; testes automatizados usam provedor simulado |
| UC-050..055 | Facebook Login (Graph API) | INTEGRATION | YES (idem) | idem; versão do Graph via P-72 |
| Todos | PostgreSQL | TECHNICAL | YES | Docker já existente |

## Dependências incertas / UNKNOWN

| Origem | Possível dependência | Dúvida | Impacto | Ação necessária |
|---|---|---|---|---|
| UC-051 (Facebook) | Revisão de app Facebook para permissão `email` | Standard access costuma bastar; confirmar no momento do registro do app | Cria conta sem e-mail ⇒ `email_unavailable` | Registrado como risco RISK-06; fluxo trata ausência de e-mail |
| UC-050 (Google) | Verificação da tela de consentimento | escopos básicos (`openid email profile`) dispensam verificação sensível | nenhum na lógica | checklist de implantação |

Nenhuma dependência incerta altera ordem ou Wave ⇒ sem `REQUIRES_REVIEW`.
