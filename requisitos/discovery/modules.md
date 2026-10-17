---
name: discovery-modules
type: discovery
description: "Discovery — Module Inventory — Achernar Identity"
---

# Discovery — Module Inventory

Todos os módulos têm origem `NEW`; “Evidência” = referência a requisito/decisão (não há legado).
Telas: IDs `SCR-xx` de `specs/_shared/ui-map.md`. Complexidade/Risco: L/M/H.

## M1 — Setup e primeiro administrador
- Pasta/origem: FT-01 · Status: APPROVED
- Telas: SCR-01 Setup do primeiro administrador
- Casos de uso: UC-001 (compl. M, risco H — segurança do bootstrap) · Evidência: RF-15, D-24
- Dependências: M6 (aplicação da UI semeada) · Integrações: —
- Reutilização potencial: política de senha de M4

## M2 — Contas e cadastro
- FT-02 · Telas: SCR-02 Cadastro, SCR-03 Verificar e-mail, SCR-04 Reenviar verificação, SCR-05 Confirmação “verifique seu e-mail”
- UCs: UC-010 (M/M), UC-011 (L/M), UC-012 (L/L) · Evidência: RF-01, RF-02, RF-16, D-11, D-13, D-14
- Dependências: M6, M10 · Integração: SMTP
- Reutilização: política de senha, anti-enumeração

## M3 — Autenticação e tokens
- FT-03 · Telas: SCR-06 Login, (estado) renovação silenciosa
- UCs: UC-020 (H/H), UC-021 (H/H — rotação, concorrência), UC-022 (L/L) · Evidência: RF-03, RF-05..07, D-04..D-08, D-48
- Dependências: M2, M6, M7 · Reuso: emissão de tokens compartilhada com M8

## M4 — Credenciais e conta
- FT-04 e FT-05 · Telas: SCR-07 Esqueci a senha, SCR-08 Redefinir senha, SCR-09 Conta (visão geral), SCR-10 Segurança (senha/e-mail/vínculos), SCR-11 Diálogo de reautenticação, SCR-12 Confirmar troca de e-mail, SCR-13 Definir senha local
- UCs: UC-030 (M/M), UC-031 (M/H), UC-032 (M/M), UC-033 (M/M), UC-037 (M/M), UC-034 (L/L), UC-035 (M/M), UC-036 (M/M)
- Evidência: RF-09..11, RF-18, RF-30, RF-31, D-21, D-22, D-47
- Dependências: M3, M10

## M5 — Sessões
- FT-06 · Telas: SCR-14 Minhas sessões
- UCs: UC-040 (L/L), UC-041 (L/M), UC-042 (M/M) · Evidência: RF-08, RF-12, D-44

## M6 — Aplicações e APIs consumidoras
- FT-09 · Telas: SCR-24 Lista de aplicações, SCR-25 Detalhe/edição de aplicação, SCR-26 Exibição única de segredo, SCR-27 Lista de APIs, SCR-28 Detalhe/edição de API
- UCs: UC-060 (M/H), UC-061 (L/L), UC-062 (M/M), UC-063 (M/H), UC-064 (M/M), UC-065 (L/L), UC-066 (M/M), UC-067 (M/H), UC-068 (L/L)
- Evidência: RF-19, RF-20, RF-24, RF-34, D-08, D-09, D-41, D-45, D-46

## M7 — Chaves, JWKS e introspecção
- FT-10 · Telas: SCR-29 Chaves de assinatura
- UCs: UC-070 (L/M), UC-071 (H/H — rotação sem interrupção), UC-072 (M/H), UC-073 (M/H)
- Evidência: RF-05, RF-21, RF-22, RF-32, D-04, D-10, D-25

## M8 — Login social
- FT-07 e FT-08 · Telas: SCR-15 Botões sociais (componente do login/cadastro), SCR-16 Retorno do login social (landing/troca de código), SCR-17 Vínculos sociais (em Segurança)
- UCs: UC-050 (M/H), UC-051 (H/H), UC-052 (M/H), UC-053 (H/H), UC-054 (M/M), UC-055 (M/M)
- Evidência: RF-04, RF-17, RF-18, §6, D-19..D-21
- Integrações: Google, Facebook

## M9 — Administração de contas
- FT-11 · Telas: SCR-18 Lista de contas, SCR-19 Detalhe da conta, SCR-20 Diálogos de ação (desativar/reativar/perfil/anonimizar)
- UCs: UC-080 (M/L), UC-081 (M/H), UC-082 (L/M), UC-083 (M/H), UC-084 (M/H)
- Evidência: RF-13, RF-27, RF-28, D-23, D-27

## M10 — E-mails transacionais
- FT-13 · Sem tela (administração de falhas por eventos)
- UCs: UC-095 (M/M) · Evidência: RF-23, D-18
- Integração: SMTP · Dependência: nenhuma (fundação)

## M11 — Eventos de segurança e retenção
- FT-12 · Telas: SCR-21 Eventos de segurança
- UCs: UC-090 (M/L), UC-091 (M/M), UC-092 (M/M) · Evidência: RF-14, RF-29, RF-33, D-26, D-27

## M12 — Interface base
- FT-14 · Telas: SCR-22 Shell/navegação, SCR-23 Página de erro/403/404, SCR-30 Preferências (tema/idioma), SCR-31 Estados globais
- UCs: UC-100 (M/M) · Evidência: RF-25, RF-26, RNF-07..09

## Fundações técnicas (não são casos de uso)

| ID | Fundação | Entrega | Habilita |
|---|---|---|---|
| FND-01 | Esqueleto de backend, migrações base, tratamento padronizado de erros/validação, logs com redação, configuração/validação de parâmetros | RNF-05, RNF-06, D-30, D-32, D-36 | todos |
| FND-02 | Gravação de eventos de segurança (serviço interno + tabela) | RF-14 | UC-001 e todos que auditam |
| FND-03 | Limitação de taxa/bloqueio (contadores) | D-16 | UC-010, UC-012, UC-020, UC-030, UC-050 |
| FND-04 | Outbox + templates + worker de e-mail (= UC-095) | D-18 | UC-010, UC-012, UC-030, UC-035, notificações |
| FND-05 | Esqueleto Angular: `core`, tema, i18n, roteamento, interceptors (= UC-100) | D-31 | todas as telas |
| FND-06 | Chaves + assinatura e validação de JWT (= UC-070, UC-071) | D-04, D-25 | UC-020, UC-052, UC-073 |

## Pontos UNKNOWN
Nenhum crítico (ver `system.md`, *Lacunas*).
