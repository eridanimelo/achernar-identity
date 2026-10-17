---
name: prioritization
type: discovery
description: "Discovery — Prioritization & Waves — Achernar Identity"
---

# Discovery — Prioritization & Waves

Critérios: dependências, valor, fundações, risco, complexidade, prontidão, integrações externas, paralelismo. Prioridade de criação das Specs e de
implementação são independentes. “Prontidão” = grau de definição das Specs (todas `DEFINED` ao fim desta execução).

## Prioridade para criação das Specs (informativa — todas foram produzidas nesta execução)

| Prioridade | Módulo | Spec / UCs | Dependências | Compl. | Risco | Valor | Motivo | Wave sugerida |
|---:|---|---|---|---|---|---|---|---|
| 1 | M6 | FT-09 (UC-060..068) | — | M | H | H | define cliente/audiência de que todos dependem | 1 |
| 2 | M7 | FT-10 (UC-070..073) | — | H | H | H | modelo de tokens condiciona login/APIs | 0/1 |
| 3 | M2/M3 | FT-02, FT-03 | FT-09, FT-10 | H | H | H | caminho crítico de acesso | 1 |
| 4 | M13 | FT-13 | — | M | M | H | e-mails sustentam FT-02/04/05 | 0 |
| 5 | M1 | FT-01 | FT-09 | M | H | H | primeiro admin | 1 |
| 6 | M4/M5 | FT-04, FT-05, FT-06 | FT-03 | M | M | H | autosserviço | 2 |
| 7 | M9/M11 | FT-11, FT-12 | FT-03 | M | H | H | administração e auditoria | 2/4 |
| 8 | M8 | FT-07, FT-08 | FT-02, FT-03, FT-09 | H | H | H | login social | 3 |
| 9 | M12 | FT-14 | — | M | M | H | UI base | 0 |

(“M13” = M10 E-mails.)

## Prioridade para implementação

| Prio. | Módulo | Caso de uso / Fundação | Dependências | Compl. | Risco | Valor | Prontidão | Motivo | Wave |
|---:|---|---|---|---|---|---|---|---|---|
| 1 | — | FND-01 esqueleto, erros, logs, parâmetros, migração base | — | M | M | H | pronta | sem base nada roda | 0 |
| 2 | M11 | FND-02 gravação de eventos | FND-01 | L | M | H | pronta | auditoria atômica desde o início | 0 |
| 3 | M7 | UC-070, UC-071 (FND-06) | FND-01 | H | H | H | pronta | emissão de tokens | 0 |
| 4 | M10 | UC-095 (FND-04) | FND-01 | M | M | H | pronta | e-mails | 0 |
| 5 | — | FND-03 limites de taxa | FND-01 | M | M | H | pronta | proteção desde a 1ª rota pública | 0 |
| 6 | M12 | UC-100 (FND-05) shell Angular | — | M | M | H | pronta | paralelo ao backend | 0 |
| 7 | M6 | UC-060 (+061..063 mínimo) | FND-01/02 | M | H | H | pronta | semeia a UI e habilita clientes | 1 |
| 8 | M1 | UC-001 | UC-060 | M | H | H | pronta | primeiro admin | 1 |
| 9 | M2 | UC-010, UC-011, UC-012 | FND-03/04, UC-060 | M | M | H | pronta | cadastro | 1 |
| 10 | M3 | UC-020, UC-021, UC-022 | UC-011, UC-070 | H | H | H | pronta | login/sessão | 1 |
| 11 | M4 | UC-034 | UC-020 | L | L | H | pronta | “eu” para UI | 1 |
| 12 | M6 | UC-064, UC-068 | FND-01 / UC-060 | M | M | H | pronta | APIs + config pública | 1 |
| 13 | M4 | UC-033, UC-032, UC-030, UC-031 | UC-020/FND-04 | M | H | H | pronta | senhas | 2 |
| 14 | M5 | UC-040, UC-041, UC-042 | UC-020 | M | M | M | pronta | sessões | 2 |
| 15 | M4 | UC-035, UC-036 | UC-033 | M | M | M | pronta | troca de e-mail | 2 |
| 16 | M7 | UC-073, UC-072 | UC-064 / UC-001 | M | H | H | pronta | introspecção e chaves | 2 |
| 17 | M9 | UC-080, UC-081, UC-082, UC-083 | UC-001/UC-041 | M | H | H | pronta | administração | 2 |
| 18 | M11 | UC-090 | FND-02 | M | L | H | pronta | consulta de eventos | 2 |
| 19 | M6 | UC-061..063, 065..067 (completar) | UC-060/064 | M | M | M | pronta | gestão completa | 2 |
| 20 | M8 | UC-050, UC-051, UC-052 | UC-060, UC-010/011, UC-070 | H | H | H | pronta | login social | 3 |
| 21 | M8 | UC-053, UC-054, UC-055, UC-037 | UC-051, UC-033 | H | H | M | pronta | vínculos e senha local | 3 |
| 22 | M9 | UC-084 | UC-081 | M | H | M | pronta | anonimização | 4 |
| 23 | M11 | UC-091, UC-092 | FND-02/03/04 | M | M | M | pronta | retenção e manutenção | 4 |
| 24 | — | Hardening, e2e, carga, guia de integração validado | todos | M | M | H | pronta | fechamento | 4 |

## Waves

### Wave 0 — Fundação / decisões estruturais

| Caso de uso/Decisão | Motivo | Bloqueia |
|---|---|---|
| FND-01 | base técnica e parâmetros (D-03, D-30..D-36) | tudo |
| FND-02 | auditoria atômica | UC com mudança de estado |
| UC-070 + UC-071 | chaves/JWKS/rotação | UC-020, UC-052, UC-073 |
| UC-095 | outbox/templates/worker | UC-010, UC-012, UC-030, UC-035 |
| FND-03 | limites e bloqueios | rotas públicas |
| UC-100 | shell Angular, tema, i18n | telas |

### Wave 1 — Acesso mínimo (caminho crítico)

| Caso de uso | Pré-requisitos | Pode executar em paralelo | Motivo |
|---|---|---|---|
| UC-060 | Wave 0 | com UC-064, UC-068 (UI) | registra clientes |
| UC-001 | UC-060 | — | primeiro admin |
| UC-010 → UC-011, UC-012 | UC-060, UC-095, FND-03 | UC-012 após UC-010 | cadastro |
| UC-020 → UC-021 → UC-022 | UC-011, UC-070 | UC-034 em paralelo após UC-020 | login e sessão |
| UC-034, UC-064, UC-068 | UC-020 / UC-060 | sim | conta, APIs, config pública |

### Wave 2 — Autosserviço, administração e controle de tokens

| Caso de uso | Pré-requisitos | Pode executar em paralelo | Motivo |
|---|---|---|---|
| UC-033, UC-032, UC-030/031 | UC-020, UC-095 | UC-030/031 ∥ UC-033/032 | senhas |
| UC-040, UC-041, UC-042 | UC-020 (UC-042 após UC-033) | ∥ senhas | sessões |
| UC-035, UC-036 | UC-033 | ∥ UC-040.. | e-mail |
| UC-073, UC-072 | UC-064, UC-070, UC-020 | ∥ | bloqueio imediato |
| UC-080..083, UC-090, UC-061..067 | UC-001, UC-041 | ∥ | administração |

### Wave 3 — Login social

| Caso de uso | Pré-requisitos | Pode executar em paralelo | Motivo |
|---|---|---|---|
| UC-050, UC-051, UC-052 | Waves 1–2 | provedores Google ∥ Facebook | login |
| UC-053, UC-054, UC-055, UC-037 | UC-051, UC-033 | ∥ entre si após UC-053 | vínculos |

### Wave 4 — Retenção, anonimização e fechamento

| Caso de uso | Pré-requisitos | Pode executar em paralelo | Motivo |
|---|---|---|---|
| UC-084, UC-091, UC-092 | UC-081, FND-02..04 | sim | retenção/anonimização |
| Guia de integração validado, e2e, carga, segurança | todos | sim | critérios `acceptance-criteria` |

## Caminho crítico

`FND-01 → UC-070/071 → UC-060 → UC-001 → UC-010 → UC-011 → UC-020 → UC-021 → UC-073 → UC-051 → UC-052 → UC-053`.

## Riscos de sequenciamento

- Implementar UC-020 sem UC-021 deixa sessões que expiram em 10 min sem renovação: entregar UC-020/021/022 juntos.
- UC-031/UC-081 dependem do serviço de revogação de sessões (UC-041): construir o **serviço** em Wave 1 junto com UC-020, expor UC-040..042 em Wave 2.
- Social (Wave 3) depende de credenciais reais ou provedor simulado: preparar o simulador de provedor em Wave 0/1 para não bloquear testes.
- Telas Angular podem avançar em paralelo usando contratos de `api.md` e um mock; integração final por Wave.

## Revisão humana

- Status: `APPROVED` (por autorização prévia — prompt §13; não há aprovação adicional exigida)
- Revisado por: IA sob delegação do humano
- Data: 2026-10-17
- Observações: Waves recalculadas por dependências reais (não por ID). `GATE-PLN`: **PASS** (ver `reviews/completeness-check.md`).
