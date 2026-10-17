# Achernar Identity — Especificação de E-mails

Normativo para FT-13 (UC-095) e para os fluxos que enfileiram mensagens. Decisão D-18, D-37; regras RN-EML-01…08; controles SEC-21, SEC-38.

## 1. Arquitetura de envio

```text
transação de negócio ──▶ INSERT email_outbox (PENDING, payload cifrado) ──commit──▶ worker (a cada P-58, lote 20)
                                                                                  │ SELECT … FOR UPDATE SKIP LOCKED (lease 2 min)
                                                                                  ├─ renderiza template (locale) + SMTP
                                                                                  ├─ sucesso → SENT, payload apagado
                                                                                  └─ falha  → attempts+1, next_attempt_at (backoff) | FAILED (esgotado) + EMAIL_DELIVERY_FAILED
```
- A API **nunca** espera o SMTP: cadastro/recuperação respondem 202 mesmo com SMTP fora (RNF-17).
- Reentrega: *at-least-once*. Mensagens são idempotentes para o usuário (links de uso único; novo envio invalida o anterior).
- Backoff (P-40): 1 min → 5 min → 15 min → 1 h → 6 h (6 tentativas no total); depois `FAILED` (metadados mantidos P-41; sem payload).
- Erros permanentes do SMTP (5xx de destinatário inválido) ⇒ `FAILED` imediato; temporários (4xx, timeout, conexão) ⇒ nova tentativa.
- Multi-instância: múltiplos workers convivem pelo `SKIP LOCKED`; `locked_until` recupera mensagens de worker que caiu.
- Saúde: métrica `achernar_mail_outbox_pending` e `…_failed_total`; readiness **não** depende do SMTP.

## 2. Configuração

| Item | Variável | Observação |
|---|---|---|
| Transporte | `ACHERNAR_SMTP_HOST/PORT/USERNAME/PASSWORD/STARTTLS/AUTH` (P-42) | `prod`: STARTTLS obrigatório (ou SMTPS) e autenticação; `dev`: MailHog `localhost:1025`, sem auth/TLS |
| Remetente | `ACHERNAR_MAIL_FROM`, `_FROM_NAME`, `_REPLY_TO` (P-43) | domínio com SPF/DKIM/DMARC (checklist SEC-38) |
| URLs públicas | P-60 (issuer), P-61 (UI), URLs de ação por aplicação | configuráveis; nunca derivadas do `Host` |
| Idioma | `account.locale` (TPL-05: `Accept-Language`) | `pt-BR` padrão, `en` |
| Suporte | P-78 | rodapé |
| Provedor de produção | **não definido** (A-02) | qualquer SMTP; nada proprietário no código |

Credenciais **nunca** no repositório: `.env.example` traz apenas placeholders (ver `config-changes.md`).

## 3. Layout e conteúdo comuns

- Multipart: `text/plain` + `text/html` (UTF-8, `Content-Transfer-Encoding` quoted-printable); sem rastreadores, sem imagens remotas; marca em texto/ícone embutido (CID) opcional.
- HTML acessível: estrutura semântica, `lang`, contraste AA, botão com texto descritivo **e** URL em texto puro abaixo (cliente sem HTML), tamanho de fonte ≥ 16 px, largura fluida ≤ 600 px.
- Cabeçalho: nome da aplicação de origem quando houver; rodapé: “Você recebeu este e-mail porque … Se não foi você, …”, contato de suporte (P-78), sem links de cancelamento (transacional).
- Todo e-mail com link informa a validade e “Se você não solicitou, ignore”.
- Assunto ≤ 70 caracteres. Variáveis disponíveis: `app_name`, `action_url`, `expires_in` (texto), `occurred_at` (local do usuário? UTC + fuso da conta — usa UTC e `locale`), `ip_approx` (IP truncado /24 ou /48), `support_contact`.

## 4. Catálogo de templates

| ID | Gatilho (UC) | Destinatário | Prazo do link | Link (fallback = UI do Identity) |
|---|---|---|---|---|
| TPL-01 | cadastro (UC-010), reenvio (UC-012), social pendente (UC-051), recuperação de conta pendente (UC-030) | e-mail da conta | 24 h (P-20) | `verify_email_url#token=…` (`{UI}/verify-email#token=…`) |
| TPL-02 | recuperação de senha (UC-030) | e-mail da conta `ACTIVE` | 30 min (P-21) | `reset_password_url#token=…` (`{UI}/reset-password#token=…`) |
| TPL-03 | solicitação de troca de e-mail (UC-035) | **novo** endereço | 1 h (P-22) | `confirm_email_change_url#token=…` (`{UI}/confirm-email-change#token=…`) |
| TPL-04 | idem (UC-035) | **endereço atual** | — | sem link (aviso: “alguém pediu para trocar seu e-mail”) |
| TPL-05 | cadastro duplicado (UC-010); social não verificado com conta existente (UC-051) | e-mail existente | — | sem token; link para entrar / recuperar senha |
| TPL-06 | senha alterada (UC-032), definida (UC-037), redefinida (UC-031) | e-mail da conta | — | aviso + link “Não fui eu” → `{UI}/forgot-password` |
| TPL-07 | e-mail alterado (UC-036) | **endereço antigo** | — | aviso + contato |
| TPL-08 | bloqueio por tentativas (UC-020) | e-mail da conta existente (1 por bloqueio) | — | aviso + link recuperação |
| TPL-09 | conta desativada/reativada (UC-081/082) | e-mail da conta | — | aviso + contato |
| TPL-10 | vínculo social criado/removido (UC-053/054) | e-mail da conta | — | aviso + link “Não fui eu” |
| TPL-11 | todas as sessões encerradas (UC-042) | e-mail da conta | — | aviso |

Enfileiramento condicional: TPL-02/06/08/09/10/11 só para contas existentes e em estado compatível (RN-PWD-04); resposta da API é idêntica com ou sem envio.

## 5. Textos (pt-BR / en)

| ID | Assunto pt-BR | Assunto en | Corpo (pt-BR) | Body (en) |
|---|---|---|---|---|
| TPL-01 | Confirme seu e-mail em {{app_name}} | Confirm your email for {{app_name}} | Olá! Para ativar sua conta, confirme seu e-mail. O link vale {{expires_in}}. [Confirmar e-mail] Se você não criou uma conta, ignore esta mensagem. | Hello! To activate your account, confirm your email. The link is valid for {{expires_in}}. [Confirm email] If you did not create an account, ignore this message. |
| TPL-02 | Redefinição de senha — {{app_name}} | Password reset — {{app_name}} | Recebemos um pedido para redefinir sua senha. O link vale {{expires_in}} e só pode ser usado uma vez. [Redefinir senha] Se não foi você, ignore; sua senha continua a mesma. | We received a request to reset your password. The link is valid for {{expires_in}} and can be used once. [Reset password] If this wasn’t you, ignore this; your password is unchanged. |
| TPL-03 | Confirme seu novo e-mail | Confirm your new email | Você pediu para usar este endereço em {{app_name}}. Confirme em {{expires_in}}. [Confirmar novo e-mail] Se não foi você, ignore. | You asked to use this address for {{app_name}}. Confirm within {{expires_in}}. [Confirm new email] If this wasn’t you, ignore it. |
| TPL-04 | Pedido para alterar seu e-mail | Request to change your email | Foi solicitada a troca do e-mail da sua conta. Nada mudará até a confirmação no novo endereço. Se não foi você, altere sua senha agora. | A change of your account email was requested. Nothing changes until it is confirmed at the new address. If this wasn’t you, change your password now. |
| TPL-05 | Tentativa de cadastro com seu e-mail | Sign-up attempt with your email | Alguém tentou criar uma conta com este e-mail. Se foi você, entre ou recupere sua senha: [Entrar] [Esqueci minha senha]. Se não foi, nenhuma ação é necessária. | Someone tried to create an account with this email. If it was you, sign in or reset your password: [Sign in] [Forgot password]. Otherwise no action is needed. |
| TPL-06 | Sua senha foi alterada | Your password was changed | Sua senha foi alterada em {{occurred_at}} (IP aproximado {{ip_approx}}). Outras sessões foram encerradas. Não foi você? [Recuperar acesso] | Your password was changed on {{occurred_at}} (approx. IP {{ip_approx}}). Other sessions were signed out. Not you? [Recover access] |
| TPL-07 | Seu e-mail foi alterado | Your email was changed | O e-mail desta conta foi alterado em {{occurred_at}}. Se não foi você, contate {{support_contact}} imediatamente. | This account’s email was changed on {{occurred_at}}. If this wasn’t you, contact {{support_contact}} immediately. |
| TPL-08 | Muitas tentativas de acesso | Too many sign-in attempts | Detectamos várias tentativas de entrada na sua conta. Por segurança, novas tentativas ficam bloqueadas por {{expires_in}}. Se foi você, aguarde; senão, [redefina sua senha]. | We detected several sign-in attempts on your account. For security, new attempts are blocked for {{expires_in}}. If it was you, wait; otherwise [reset your password]. |
| TPL-09 | Status da sua conta | Your account status | Sua conta foi {{desativada|reativada}} em {{occurred_at}}. Dúvidas: {{support_contact}}. | Your account was {{disabled|re-enabled}} on {{occurred_at}}. Questions: {{support_contact}}. |
| TPL-10 | Login social {{provedor}} {{vinculado|removido}} | {{Provider}} sign-in {{linked|removed}} | O acesso com {{provedor}} foi {{vinculado|removido}} à sua conta em {{occurred_at}}. Não foi você? [Proteger conta] | {{Provider}} sign-in was {{linked|removed}} on your account on {{occurred_at}}. Not you? [Secure account] |
| TPL-11 | Sessões encerradas | Sessions signed out | Todas as sessões da sua conta foram encerradas em {{occurred_at}}. Se não foi você, altere sua senha. | All sessions of your account were signed out on {{occurred_at}}. If this wasn’t you, change your password. |

Os arquivos de template (`templates/mail/<id>.{html,txt}.<locale>`) serão criados na implementação, sem texto fixo no código Java.

## 6. Limites de reenvio e proteção (resumo; valores em P-33…P-35)

| Fluxo | Limite | Excedido |
|---|---|---|
| Reenvio de verificação | 1 / 60 s e 5 / h por e-mail; 10 / h por IP | 202 silencioso (não envia) |
| Recuperação de senha | 3 / h por e-mail; 10 / h por IP | 202 silencioso |
| Troca de e-mail | 3 / dia por conta | `429 RATE_LIMITED` (usuário autenticado) |
| Cadastro duplicado (TPL-05) | 1 / h por e-mail destinatário | silencioso |
| TPL-08 | 1 por bloqueio | — |

## 7. Ambiente local

`docker/docker-compose.yml` já provê MailHog (`1025` SMTP / `8025` UI). A aplicação em `dev` usa `localhost:1025`; testes de integração leem `GET http://localhost:8025/api/v2/messages`
(ou GreenMail embutido em testes unitários). **Recomendação** (opcional, `config-changes.md`): substituir MailHog (projeto sem manutenção ativa) por Mailpit, compatível com a mesma porta/UI/API de teste.

## 8. Critérios de verificação (resumo — completos em FT-13)

Mensagem enfileirada na mesma transação; rollback não gera e-mail; SMTP fora ⇒ API 200/202 e reentrega; 6 falhas ⇒ `FAILED` + evento; payload apagado após envio;
link correto por aplicação e fallback; locale respeitado; nenhum segredo (senha) no corpo; token apenas no fragmento da URL.
