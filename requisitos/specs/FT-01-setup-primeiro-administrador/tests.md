# Test Spec — FT-01 Setup do primeiro administrador

## 0. Identificação
- ID: FT-01 · Origem: `NEW` · Status da especificação: `APPROVED` (documental)

> Define cenários a serem testados posteriormente; não representa execução.

## 1. Escopo de validação
Disponibilidade do setup, criação segura, limites, fechamento, recuperação, concorrência e ausência de credenciais fixas.

## 2. Cenários

### CT-FT01-01 — Estado do setup disponível
**Objetivo:** validar a consulta com setup necessário.
**Dado** nenhum administrador ativo e segredo configurado **Quando** consulta o estado **Então** `setup_required=true` e `setup_available=true`.
**Referências:** CA-FT01-01, API-FT01-01

### CT-FT01-02 — Criação com sucesso
**Dado** setup disponível **Quando** envia segredo correto, e-mail e senha válidos **Então** 201; conta com perfil administrador, ativa, e-mail verificado; sem tokens; evento `BOOTSTRAP_ADMIN_CREATED`; novo estado `setup_required=false`.
**Referências:** CA-FT01-02, API-FT01-02

### CT-FT01-03 — Segredo incorreto
**Dado** setup disponível **Quando** envia segredo errado **Então** 403 `SETUP_TOKEN_INVALID`; nenhuma conta; evento `SETUP_ATTEMPT_FAILED` sem o valor.
**Referências:** CA-FT01-03

### CT-FT01-04 — Limite de tentativas
**Dado** 5 falhas em 15 min pelo mesmo IP **Quando** envia o 6º pedido (inclusive com segredo correto) **Então** 429 com `Retry-After`; passado o prazo, volta a aceitar.
**Referências:** CA-FT01-04

### CT-FT01-05 — Setup encerrado
**Dado** administrador ativo existente **Quando** consulta e cria **Então** `setup_required=false`; criação ⇒ 404 `SETUP_NOT_AVAILABLE` mesmo com segredo correto.
**Referências:** CA-FT01-05

### CT-FT01-06 — Segredo não configurado
**Dado** nenhum administrador e variável ausente **Quando** a aplicação inicia **Então** em `dev` inicia com aviso e `setup_available=false`; em `prod` a inicialização é recusada com mensagem clara.
**Referências:** CA-FT01-06

### CT-FT01-07 — Validações
**Dado** setup disponível **Quando** senha curta / comum / contém o e-mail / e-mail inválido / segredo vazio **Então** 400 com `errors[]` por campo; nada criado.
**Referências:** CA-FT01-07

### CT-FT01-08 — Recuperação
**Dado** apenas administradores desativados **Quando** usa o setup **Então** cria novo administrador e fecha.
**Referências:** CA-FT01-08

### CT-FT01-09 — E-mail em uso
**Dado** conta existente com o e-mail **Quando** setup com esse e-mail **Então** 409 `CONFLICT`; conta existente inalterada.
**Referências:** CA-FT01-09

### CT-FT01-10 — Varredura de credenciais
**Dado** repositório, Compose, `.env.example`, imagens **Quando** varridos por detector de segredos **Então** nenhum segredo real; variáveis do setup vazias.
**Referências:** CA-FT01-10

### CT-FT01-11 — Concorrência
**Dado** 10 requisições válidas simultâneas **Quando** processadas **Então** exatamente 1 sucesso (201); demais 404/409; 1 administrador criado pelo setup.
**Referências:** CA-FT01-11

### CT-FT01-12 — Aplicação da UI semeada
**Dado** banco vazio **Quando** a aplicação inicia **Então** existe `achernar-identity-ui` (tipo `BROWSER`, acesso à API do Identity) com origens/URLs de configuração; reinício não duplica.
**Referências:** CA-FT01-12

## 3. Cenários de erro
Cobertos em CT-FT01-03, 04, 07, 09.

## 4. Limites e validações
E-mail com 254/255 caracteres; senha com 11/12/128/129 caracteres; segredo com 256/257 caracteres.

## 5. API
CT-FT01-01…09, 11.

## 6. UI/UX
### CT-FT01-13 — Tela de setup
**Dado** `setup_required=true`, `setup_available=true` **Quando** abre `/setup` **Então** formulário completo, validações de cliente, foco/erros acessíveis, redirecionamento ao login após sucesso; com `setup_available=false` mostra instrução sem formulário.
**Referências:** CA-FT01-01, 02, 06; SCR-01

## 7. Segurança
### CT-FT01-14 — Segredo não vaza
**Dado** requisições com valores-sentinela como segredo **Quando** varridos logs/eventos/respostas/banco **Então** nenhuma ocorrência.
**Referências:** CA-FT01-03, CA-FT01-10; SEC-31, SEC-FT01-02

### CT-FT01-15 — Tempo constante
**Dado** segredos com prefixos corretos de tamanhos diferentes **Quando** medido o tempo de resposta **Então** sem correlação estatisticamente significativa com o comprimento do prefixo correto.
**Referências:** CA-FT01-03; SEC-33, SEC-FT01-02

## 8. Pontos a validar
Nenhum.

## 9. Rastreabilidade
| Cenário | Critério/Regra/Contrato |
|---|---|
| CT-FT01-01 | CA-FT01-01 |
| CT-FT01-02 | CA-FT01-02 |
| CT-FT01-03 | CA-FT01-03 |
| CT-FT01-04 | CA-FT01-04 |
| CT-FT01-05 | CA-FT01-05 |
| CT-FT01-06 | CA-FT01-06 |
| CT-FT01-07 | CA-FT01-07 |
| CT-FT01-08 | CA-FT01-08 |
| CT-FT01-09 | CA-FT01-09 |
| CT-FT01-10 | CA-FT01-10 |
| CT-FT01-11 | CA-FT01-11 |
| CT-FT01-12 | CA-FT01-12 |
| CT-FT01-13 | CA-FT01-01, 02, 06 |
| CT-FT01-14 | CA-FT01-03, 10; SEC-31 |
| CT-FT01-15 | CA-FT01-03; SEC-33 |
