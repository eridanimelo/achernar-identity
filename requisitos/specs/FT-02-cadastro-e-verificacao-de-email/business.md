# Business Spec — FT-02 Cadastro e verificação de e-mail

## 0. Identificação
- ID: FT-02 (UC-010, UC-011, UC-012) · Origem: `NEW` · Módulo: M2 Contas e credenciais · Status: `APPROVED` (documental)

## 1. Objetivo
Permitir que uma pessoa crie conta com e-mail e senha por uma aplicação consumidora e comprove a titularidade do e-mail, sem que o sistema revele se um e-mail já está cadastrado e sem emitir tokens enquanto o e-mail não for verificado.

## 2. História / Necessidade
**Como** visitante de uma aplicação **quero** me cadastrar e confirmar meu e-mail **para** poder entrar com segurança. **Como** titular de um e-mail já cadastrado **quero** ser avisado de tentativas de cadastro com meu endereço **para** perceber uso indevido.

## 3. Fluxo funcional

### UC-010 — Cadastrar conta com e-mail e senha
1. O visitante abre o cadastro da aplicação; a tela consulta a configuração pública (FT-09) para saber se o cadastro é permitido.
2. Informa e-mail e senha (com confirmação) e envia.
3. O sistema valida aplicação ativa e cadastro permitido, valida e-mail e senha pela política, normaliza o e-mail.
4. E-mail inexistente: cria conta `PENDING_VERIFICATION` com credencial de senha, gera token de verificação e enfileira o e-mail TPL-01.
5. E-mail existente (qualquer estado): nada é criado/alterado; o titular recebe TPL-05.
6. Em ambos os casos a resposta é idêntica (aceito) e a tela mostra “Verifique seu e-mail”.
Alternativas: A1 aplicação sem cadastro aberto ⇒ recusa clara. A2 excesso de tentativas por IP ⇒ recusa com tempo de espera; por e-mail ⇒ aceito sem efeito. A3 senha fora da política ⇒ erros por campo. A4 conta social pendente de mesmo e-mail ⇒ tratada como existente.
Exceções: E1 aplicação inválida/inativa. E2 origem não permitida. E3 cadastros simultâneos do mesmo e-mail ⇒ uma conta, o outro é “duplicado”.

### UC-011 — Verificar e-mail
1. O titular abre o link do e-mail (token no fragmento da URL) na aplicação.
2. A aplicação envia o token; o sistema consome o token (uso único), marca e-mail verificado e ativa a conta.
3. A tela informa sucesso e conduz ao login. Nenhuma sessão é criada.
Exceções: token inexistente, expirado, usado, invalidado ou de outra finalidade ⇒ mensagem única com ação de reenviar.

### UC-012 — Reenviar verificação
1. O visitante informa o e-mail em “Reenviar verificação”.
2. Resposta sempre neutra. Se existe conta pendente e os limites permitem, o token anterior é invalidado e novo é enviado.
Exceções: limite excedido ⇒ aceito sem envio; conta ativa/desativada/inexistente ⇒ aceito sem envio.

## 4. Critérios de aceite

### CA-FT02-01 — Cadastro cria conta pendente
**Dado** aplicação ativa com cadastro aberto e e-mail inédito **Quando** o visitante envia e-mail e senha válidos **Então** a resposta é “aceito” (202), existe conta `PENDING_VERIFICATION` com e-mail normalizado, sem tokens de acesso, um e-mail TPL-01 é enfileirado e o evento `ACCOUNT_REGISTERED` é registrado. **Ref.:** RF-01, RN-ACC-01, RN-ACC-07, RN-ACC-09

### CA-FT02-02 — Anti-enumeração no cadastro
**Dado** e-mail já cadastrado (pendente, ativo ou desativado) **Quando** alguém tenta cadastrá-lo **Então** a resposta (corpo, status e cabeçalhos) é idêntica à do CA-FT02-01, nada é criado ou alterado, o titular recebe TPL-05 e o evento `REGISTRATION_DUPLICATE_ATTEMPT` é registrado. **Ref.:** RN-ACC-06, SEC-07, RNF-16

### CA-FT02-03 — Tempo equivalente
**Dado** cadastros de e-mail novo e existente **Quando** medidos **Então** a diferença do tempo médio de resposta é ≤ 15 %. **Ref.:** RNF-16

### CA-FT02-04 — Normalização e unicidade
**Dado** `Ana@Exemplo.com` cadastrado **Quando** se tenta `ana@exemplo.com ` (espaços, maiúsculas) **Então** é tratado como duplicado. **Ref.:** RN-ACC-02, D-14

### CA-FT02-05 — Política de senha
**Dado** senha com 11 caracteres, só espaços, comum, ou contendo o e-mail **Quando** cadastro **Então** erro `PASSWORD_POLICY_VIOLATION` com código por campo e nada criado; senha de 12 a 128 caracteres é aceita. **Ref.:** RN-PWD-01

### CA-FT02-06 — Cadastro desabilitado ou aplicação inválida
**Dado** aplicação com `allow_registration=false` **Quando** cadastro **Então** `REGISTRATION_DISABLED`; aplicação desconhecida/inativa ⇒ `INVALID_CLIENT`; origem fora da lista ⇒ `ORIGIN_NOT_ALLOWED`. **Ref.:** RN-ACC-07, RN-AUT-04

### CA-FT02-07 — Limites
**Dado** 10 cadastros/h do mesmo IP **Quando** o 11º chega **Então** `RATE_LIMITED` com `Retry-After`; **dado** 3 cadastros/h do mesmo e-mail **Quando** o 4º chega **Então** resposta “aceito” sem criar nem enviar. **Ref.:** P-32, RN-EML-03

### CA-FT02-08 — Verificar e-mail
**Dado** token válido **Quando** enviado **Então** 204; conta `ACTIVE` com `email_verified_at`; token consumido; evento `EMAIL_VERIFIED`; nenhuma sessão. **Ref.:** RF-02, RN-ACC-10

### CA-FT02-09 — Token inválido
**Dado** token inexistente, expirado (24 h), já usado, invalidado ou de outra finalidade **Quando** enviado **Então** `TOKEN_INVALID_OR_EXPIRED` (mesmo código para todos). **Ref.:** RN-ACC-11, D-17

### CA-FT02-10 — Uso único sob concorrência
**Dado** o mesmo token enviado 2 vezes simultaneamente **Quando** processado **Então** exatamente uma requisição tem sucesso. **Ref.:** DM-11, RNF-20

### CA-FT02-11 — Reenvio
**Dado** conta pendente **Quando** pede reenvio **Então** resposta neutra (202), token anterior invalidado, novo e-mail TPL-01 e evento `EMAIL_VERIFICATION_RESENT`; limites 1/60 s e 5/h por e-mail e 10/h por IP; conta inexistente/ativa/desativada ⇒ mesma resposta sem envio. **Ref.:** RN-ACC-11, P-34

### CA-FT02-12 — Login antes de verificar
**Dado** conta pendente **Quando** tenta login com a senha correta **Então** `EMAIL_NOT_VERIFIED` e nenhum token (verificado em FT-03). **Ref.:** RN-ACC-09, RF-16

### CA-FT02-13 — Purga de pendentes
**Dado** conta pendente com mais de 7 dias **Quando** a manutenção diária roda **Então** a conta e seus dados são removidos e o e-mail fica livre. **Ref.:** RN-RET-05, P-50

### CA-FT02-14 — Link de verificação
**Dado** o e-mail TPL-01 **Quando** gerado **Então** o link usa a URL de verificação da aplicação de origem (ou a UI do Identity) com o token no fragmento, nunca baseado em `Host` da requisição. **Ref.:** RN-EML-04, SEC-22

### CA-FT02-15 — Falha de e-mail não bloqueia
**Dado** SMTP indisponível **Quando** cadastro **Então** a resposta continua “aceito” e o envio é tentado novamente conforme P-40. **Ref.:** RN-EML-02, RNF-17

## 5. Regras de negócio
RN-ACC-01, 02, 06, 07, 08, 09, 10, 11, RN-PWD-01, RN-EML-02, 03, 04, RN-RET-05 (definidas em `business-rules.md`).

## 6. Dados funcionais
| Dado | Significado | Obrigatório | Restrição | Regra |
|---|---|---|---|---|
| `client_id` | aplicação de origem | sim | ativa | RN-AUT-04 |
| e-mail | identificação da conta | sim | válido, ≤ 254 | RN-ACC-02 |
| senha | credencial | sim | política | RN-PWD-01 |
| idioma | idioma dos e-mails | não | `pt-BR`/`en` | RN-ACC-08 |
| token de verificação | prova de titularidade | sim (UC-011) | 24 h, uso único | RN-ACC-10 |

## 7. Ações funcionais
Cadastrar; Verificar e-mail; Reenviar verificação (pré-condições e resultados nos fluxos da §3).

## 8. Perfis e permissões
Visitante (sem autenticação) executa as três ações; nenhuma exige perfil.

## 9. Rastreabilidade
UC-010 → RF-01, CA-FT02-01…07, 14, 15 · UC-011 → RF-02, CA-FT02-08…10 · UC-012 → RF-02, CA-FT02-11 · RF-16 → CA-FT02-12 · RN-RET-05 → CA-FT02-13.

## 10. Pontos a validar
Nenhum.

## 11. Divergências
Nenhuma específica (ver DEV-01..07 em `decisions.md`).

## 12. LEGACY
`NOT_APPLICABLE`.

## 13. Resultado esperado
Cadastro seguro e resistente a enumeração; conta só passa a `ACTIVE` após comprovação do e-mail.
