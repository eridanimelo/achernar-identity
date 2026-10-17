# Business Spec — FT-09 Registro de aplicações e APIs

## 0. Identificação
FT-09 (UC-060…068) · `NEW` · Módulo M5 Registro de consumidores · `APPROVED` (documental)

## 1. Objetivo
Permitir ao administrador cadastrar, consultar, alterar e desativar **aplicações consumidoras** (clientes) e **APIs consumidoras** (audiências), gerir seus segredos/credenciais e publicar a configuração pública que as telas de cada aplicação precisam. A UI do próprio Identity é uma aplicação registrada (D-46).

## 2. História
**Como** administrador **quero** registrar cada aplicação e API do projeto com suas origens, retornos e audiências **para** que apenas consumidores autorizados usem o Identity.

## 3. Fluxos
### UC-060 — Cadastrar aplicação
Informa `client_id`, nome, tipo (`BROWSER`|`CONFIDENTIAL`), origens, URLs de retorno e de ação (verificação, redefinição, confirmação de troca), cadastro aberto, provedores sociais, acesso à API do Identity, TTL do access opcional e APIs permitidas (com padrão). Aplicação `CONFIDENTIAL` recebe o segredo **uma única vez**.
### UC-061 — Consultar aplicações: lista paginada com busca e detalhe (sem segredos).
### UC-062 — Alterar aplicação (inclui ativar/desativar), com controle otimista. Desativar revoga todas as sessões da aplicação.
### UC-063 — Rotacionar segredo: cria novo segredo (até 2 ativos) e permite revogar o antigo; o valor é exibido uma vez.
### UC-064/065/066 — Cadastrar, consultar e alterar API (audiência única e imutável, nome, descrição, estado).
### UC-067 — Rotacionar credencial de introspecção da API (mesmas regras de segredo).
### UC-068 — Consultar configuração pública da aplicação: nome, cadastro aberto, provedores habilitados, tipo.

## 4. Critérios de aceite
- **CA-FT09-01 — Cadastro de aplicação.** Dados válidos ⇒ 201 com a aplicação; `CONFIDENTIAL` devolve `client_secret` uma única vez (nunca mais retornado); evento `APPLICATION_CREATED`. Ref.: RF-19, RN-APP-01, 02, 06
- **CA-FT09-02 — `client_id`.** Deve casar `^[a-z0-9-]{3,64}$`, ser único e imutável; duplicado ⇒ `CONFLICT`. Ref.: RN-APP-01
- **CA-FT09-03 — Origens.** `scheme://host[:port]` exatos, sem caminho/curinga; `https` (exceto `http://localhost`/`127.0.0.1` em `dev`); `BROWSER` exige ≥ 1; ≤ 20. Ref.: RN-APP-03, P-74
- **CA-FT09-04 — URLs.** Retorno e ação: absolutas, sem fragmento/curinga, com origem registrada; comparação exata; ≤ 20 retornos. Ref.: RN-APP-04, SEC-16
- **CA-FT09-05 — Audiências permitidas.** Pelo menos uma API ativa se `identity_api_access=false`; uma ou mais marcadas como padrão; APIs inexistentes ⇒ erro; ≤ 20. Ref.: RN-APP-08
- **CA-FT09-06 — TTL do access.** Fora de 5–15 min ⇒ `VALIDATION_ERROR`. Ref.: RN-APP-09
- **CA-FT09-07 — Acesso à API do Identity.** `identity_api_access=true` inclui a audiência do Identity nos tokens da aplicação. Ref.: RN-APP-10
- **CA-FT09-08 — Consulta.** Lista paginada (25/100) com busca por nome ou `client_id`, ordenação permitida e filtro por estado; detalhe sem segredos/hashes; apenas ADMIN. Ref.: RF-19, RN-APP-06, SEC-27
- **CA-FT09-09 — Alteração com controle otimista.** `PATCH` com `version`; versão divergente ⇒ `CONFLICT`; campos desconhecidos ⇒ `VALIDATION_ERROR`; `client_id` e `type` imutáveis; evento `APPLICATION_UPDATED` com nomes dos campos. Ref.: RN-APP-13
- **CA-FT09-10 — Desativar aplicação.** `status=DISABLED` ⇒ novos login/refresh/exchange ⇒ `INVALID_CLIENT`; todas as sessões da aplicação revogadas (`APP_DISABLED`); reativar não restaura sessões; evento. Ref.: RN-APP-07
- **CA-FT09-11 — Aplicação do sistema.** `achernar-identity-ui` não pode ser desativada nem perder `identity_api_access`; sua URL e origens podem ser ajustadas. Ref.: D-46, RN-SET-05
- **CA-FT09-12 — Rotação de segredo.** `POST …/secrets` cria segredo (256 bits) exibido uma vez; no máximo 2 ativos (terceiro ⇒ `CONFLICT`); `DELETE …/secrets/{id}` revoga; revogar o último ativo de aplicação `CONFIDENTIAL` ⇒ `CONFLICT`; `BROWSER` não tem segredos; eventos `APPLICATION_SECRET_ROTATED`. Ref.: RN-APP-06, SEC-29
- **CA-FT09-13 — Segredo só como hash.** Nenhuma consulta devolve o segredo; ele é validado em tempo constante. Ref.: SEC-29, SEC-33
- **CA-FT09-14 — API consumidora.** Cadastro com `audience` única, imutável, ≤ 200 caracteres (`[A-Za-z0-9:/._-]`), nome ≤ 100; recebe credencial de introspecção exibida uma vez; evento `API_CREATED`. Ref.: RF-20, RN-APP-14, 16
- **CA-FT09-15 — Alterar API.** Nome/descrição/estado com versão; desativar ⇒ omitida de novos tokens e introspecção por ela ⇒ `INVALID_CLIENT`; tokens já emitidos valem até `exp`. Ref.: RN-APP-15
- **CA-FT09-16 — Credencial de introspecção.** Rotação/revogação com as mesmas regras (≤ 2 ativos; não revogar a última ativa de API ativa). Ref.: RN-APP-16, RF-22
- **CA-FT09-17 — Política de emissão.** `aud` = (pedidas ∩ permitidas ∩ APIs ativas) ∪ {audiência do Identity se acesso}; vazio ⇒ `AUDIENCE_NOT_ALLOWED`. Ref.: RN-APP-17
- **CA-FT09-18 — Configuração pública.** `GET /auth/applications/{client_id}/config` devolve só `name`, `type`, `allow_registration`, `providers` habilitados (global ∩ aplicação) e nada de origens, segredos ou audiências; aplicação inativa/desconhecida ⇒ 404 genérico; cache curto. Ref.: RF-24, RN-APP-12
- **CA-FT09-19 — CORS dinâmico.** Origens aceitas = união das origens de aplicações ativas para cada rota; alteração vale sem reinício; `Origin` desconhecida ⇒ sem cabeçalhos CORS. Ref.: D-41, SEC-26
- **CA-FT09-20 — Limites de registro.** ≤ 100 aplicações e ≤ 100 APIs; excedido ⇒ `VALIDATION_ERROR`. Ref.: P-74
- **CA-FT09-21 — Autorização.** Todas as rotas administrativas exigem ADMIN verificado no banco; usuário comum ⇒ `FORBIDDEN`. Ref.: RN-ADM-01
- **CA-FT09-22 — Semeadura da aplicação da UI.** Na primeira subida existe `achernar-identity-ui` (BROWSER) configurada por P-61/P-62/P-63. Ref.: RN-SET-05
- **CA-FT09-23 — Provedores sociais por aplicação.** Só pode habilitar provedor configurado globalmente; senão `VALIDATION_ERROR`. Ref.: RN-APP-11
- **CA-FT09-24 — Concorrência.** Duas rotações simultâneas respeitam o limite de 2 ativos; dois cadastros do mesmo `client_id` ⇒ um sucesso. Ref.: DM-09, RNF-20
- **CA-FT09-25 — Auditoria.** Toda criação/alteração/ativação/desativação/rotação gera evento com ator e campos alterados (sem valores sensíveis). Ref.: RN-ADM-08, RN-AUD-04

## 5. Regras: RN-APP-01…17, RN-SET-05, RN-ADM-01/08, D-41, D-45, D-46 · Perfil: ADMIN (UC-060…067); público (UC-068).
## 6. Dados: ver `data-model.md §2.4–2.5`; campos funcionais em `api.md`.
## 7. Rastreabilidade: UC-060 → CA-FT09-01…07, 11, 20…25 · UC-061 → CA-FT09-08 · UC-062 → CA-FT09-09…11 · UC-063 → CA-FT09-12, 13, 24 · UC-064…067 → CA-FT09-14…16 · UC-068 → CA-FT09-18, 19 · política de emissão → CA-FT09-17.
## 8. Pontos a validar / divergências / LEGACY: nenhum / nenhuma / `NOT_APPLICABLE`.
## 9. Resultado esperado: apenas consumidores registrados e configurados corretamente usam o Identity; segredos controlados.
