# Achernar Identity — Catálogo de Erros

Formato: `application/problem+json` (RFC 9457) com `type` (`https://<issuer>/problems/<code-kebab>`), `title`, `status`, `detail`,
`instance`, `code` (estável, abaixo), `trace_id` e, quando aplicável, `errors[]` = `{field, code, message}` e `retry_after_seconds`.
`WWW-Authenticate: Bearer error="invalid_token"` em 401 de API. Mensagens ao usuário final são traduzidas pelo cliente a partir de `code`.

| Código | HTTP | Quando | Observações de segurança |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | corpo/parâmetro inválido (formato, tamanho, obrigatório) | lista `errors[]` por campo (inclui `SAME_AS_CURRENT` na troca de senha) |
| `PASSWORD_POLICY_VIOLATION` | 400 | senha fora da política | `errors[].code` ∈ {`TOO_SHORT`,`TOO_LONG`,`COMMON`,`CONTAINS_EMAIL`,`BLANK`} |
| `MALFORMED_REQUEST` | 400 | JSON inválido | — |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | `Content-Type` ≠ JSON | — |
| `PAYLOAD_TOO_LARGE` | 413 | corpo > P-48 | — |
| `INVALID_CREDENTIALS` | 401 | e-mail/senha inválidos, conta inexistente **ou** bloqueio por tentativas | idêntico nos três casos (D-13) |
| `INVALID_CLIENT` | 401 | `client_id` desconhecido/inativo ou segredo inválido (`CONFIDENTIAL`) | não diferenciar desconhecido de inativo |
| `ORIGIN_NOT_ALLOWED` | 403 | `Origin` fora das origens da aplicação | — |
| `REDIRECT_URI_NOT_ALLOWED` | 400 | URL de retorno não registrada (correspondência exata) | **nunca** redirecionar para ela |
| `AUDIENCE_NOT_ALLOWED` | 400 | audiência não permitida à aplicação/sessão | — |
| `REGISTRATION_DISABLED` | 403 | aplicação não permite cadastro | — |
| `PROVIDER_NOT_ENABLED` | 400 | provedor social não habilitado | — |
| `EMAIL_NOT_VERIFIED` | 403 | senha correta, conta pendente | só após provar a senha |
| `ACCOUNT_DISABLED` | 403 | conta desativada (após provar senha / provedor) | — |
| `INVALID_REFRESH_TOKEN` | 401 | refresh ausente, desconhecido, expirado, revogado ou reutilizado | único código para todos os casos |
| `SESSION_REVOKED` | 401 | access token com `sid` revogado na API do Identity | — |
| `TOKEN_INVALID_OR_EXPIRED` | 400 | token de uso único inexistente/expirado/consumido/invalidado | único código |
| `INVALID_GRANT` | 400 | código de troca social inválido/expirado/usado/`code_verifier` errado | único código |
| `REAUTH_REQUIRED` | 403 | operação sensível sem reautenticação recente | UI abre o diálogo de reautenticação |
| `REAUTH_FAILED` | 403 | senha de reautenticação incorreta | conta para P-30 |
| `FORBIDDEN` | 403 | sem perfil/permissão | — |
| `NOT_FOUND` | 404 | recurso inexistente (ou `/setup` após concluído) | — |
| `METHOD_NOT_ALLOWED` | 405 | método incorreto | — |
| `CONFLICT` | 409 | conflito genérico (ex.: `client_id` já existe, controle otimista) | — |
| `LAST_ADMIN_PROTECTED` | 409 | ação removeria o último administrador ativo | — |
| `LAST_ACCESS_METHOD` | 409 | removeria o último método de acesso | — |
| `SOCIAL_ALREADY_LINKED` | 409 | identidade social já vinculada a outra conta, ou conta já tem vínculo desse provedor | — |
| `SOCIAL_EMAIL_UNAVAILABLE` | 400 (redirect: `error=email_unavailable`) | provedor não informou e-mail | — |
| `ACCOUNT_LINK_REQUIRED` | (redirect: `error=account_link_required`) | e-mail social coincide com conta local ativa | sem vínculo automático |
| `SETUP_TOKEN_INVALID` | 403 | token de bootstrap inválido | tempo constante; conta para P-38 |
| `SETUP_NOT_AVAILABLE` | 404 | já existe administrador ativo | — |
| `RATE_LIMITED` | 429 | limite excedido | `Retry-After` obrigatório |
| `INTERNAL_ERROR` | 500 | falha inesperada | `detail` genérico + `trace_id` |
| `SERVICE_UNAVAILABLE` | 503 | dependência indisponível (ex.: provedor social) | — |

## Erros de redirecionamento (login social)

O Identity só redireciona para `redirect_uri` **previamente validada**, anexando `error` (RFC 6749 §4.1.2.1) e `state` do cliente:
`access_denied` (usuário cancelou ou provedor negou) · `account_link_required` · `account_disabled` · `email_verification_required`
· `email_unavailable` · `already_linked` · `reauth_failed` · `temporarily_unavailable` (falha no provedor/timeouts) · `invalid_request` (transação inválida/expirada/reutilizada
— somente se `redirect_uri` já tiver sido validada na transação; caso contrário, página de erro do próprio Identity).
