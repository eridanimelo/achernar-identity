# Achernar Identity — Catálogo de Eventos de Segurança

Persistidos em `security_event` (D-26). Campos comuns: `occurred_at`, `event_type`, `outcome` (`SUCCESS`|`FAILURE`), `account_id`,
`actor_account_id` (admin/solicitante), `client_id`, `session_id`, `ip`, `user_agent` (≤ 255), `request_id`, `details` (chaves
**whitelisted**, sem segredos). **Severidade**: `INFO`, `NOTICE`, `ALERT`. “TX” = gravado na mesma transação; “IND” = transação independente.

| Evento | Sev. | Grav. | Gatilho | `details` permitidos |
|---|---|---|---|---|
| `ACCOUNT_REGISTERED` | INFO | TX | cadastro concluído (e-mail/senha) | `locale` |
| `REGISTRATION_DUPLICATE_ATTEMPT` | NOTICE | TX | cadastro com e-mail existente | — |
| `EMAIL_VERIFIED` | INFO | TX | verificação concluída | — |
| `EMAIL_VERIFICATION_RESENT` | INFO | TX | reenvio aceito | — |
| `LOGIN_SUCCEEDED` | INFO | TX | login por senha | `amr` |
| `LOGIN_FAILED` | NOTICE | IND | credenciais inválidas | `reason` (`BAD_PASSWORD`,`UNKNOWN_ACCOUNT`,`THROTTLED`) |
| `LOGIN_BLOCKED_UNVERIFIED` | NOTICE | IND | senha correta, e-mail pendente | — |
| `LOGIN_BLOCKED_DISABLED` | NOTICE | IND | senha/provedor correto, conta desativada | — |
| `ACCOUNT_LOCKED_THROTTLE` | ALERT | IND | bloqueio por tentativas aplicado | `lock_seconds` |
| `SOCIAL_LOGIN_SUCCEEDED` | INFO | TX | login social concluído | `provider` |
| `SOCIAL_LOGIN_FAILED` | NOTICE | IND | falha/cancelamento/validação | `provider`,`reason` |
| `SOCIAL_ACCOUNT_CREATED` | INFO | TX | conta criada via provedor | `provider`,`email_verified` |
| `SOCIAL_LINK_CONFLICT` | NOTICE | IND | e-mail social coincide com conta local | `provider` |
| `PENDING_ACCOUNT_SUPERSEDED` | NOTICE | TX | conta pendente substituída por social | `provider` |
| `SOCIAL_LINKED` | NOTICE | TX | vínculo criado | `provider` |
| `SOCIAL_UNLINKED` | NOTICE | TX | vínculo removido | `provider` |
| `TOKEN_ISSUED` | INFO | TX | tokens emitidos (login/exchange) — apenas para primeira emissão da sessão | `audiences` |
| `REFRESH_REUSE_TOLERATED` | NOTICE | IND | reuso dentro da tolerância | — |
| `REFRESH_REUSE_DETECTED` | ALERT | IND+TX | reuso fora da tolerância; sessão revogada | — |
| `LOGOUT` | INFO | TX | logout da sessão atual | — |
| `SESSION_REVOKED` | INFO | TX | sessão revogada (usuário, limite, admin, reuso) | `reason` |
| `ALL_SESSIONS_REVOKED` | NOTICE | TX | encerramento global | `count`,`include_current` |
| `PASSWORD_RESET_REQUESTED` | INFO | TX | solicitação aceita (conta existente) | — |
| `PASSWORD_RESET_COMPLETED` | NOTICE | TX | senha redefinida (sessões revogadas) | — |
| `PASSWORD_CHANGED` | NOTICE | TX | senha alterada autenticado | — |
| `PASSWORD_SET` | NOTICE | TX | primeira senha local definida | — |
| `REAUTH_SUCCEEDED` | INFO | TX | reautenticação válida | `method` |
| `REAUTH_FAILED` | NOTICE | IND | reautenticação inválida | `method` |
| `EMAIL_CHANGE_REQUESTED` | NOTICE | TX | solicitação aceita | — |
| `EMAIL_CHANGED` | ALERT | TX | e-mail alterado | — |
| `ACCOUNT_DISABLED` | ALERT | TX | desativação por admin | `reason` |
| `ACCOUNT_ENABLED` | NOTICE | TX | reativação por admin | — |
| `ADMIN_GRANTED` / `ADMIN_REVOKED` | ALERT | TX | perfil ADMIN concedido/revogado | — |
| `ACCOUNT_ANONYMIZED` | ALERT | TX | anonimização | — |
| `BOOTSTRAP_ADMIN_CREATED` | ALERT | TX | primeiro admin criado | — |
| `SETUP_ATTEMPT_FAILED` | ALERT | IND | token de bootstrap inválido | — |
| `APPLICATION_CREATED` / `_UPDATED` / `_DISABLED` / `_ENABLED` | NOTICE | TX | gestão de aplicações | `client_id`,`changed` (nomes dos campos) |
| `APPLICATION_SECRET_ROTATED` | ALERT | TX | segredo rotacionado/revogado | `client_id` |
| `API_CREATED` / `_UPDATED` / `_DISABLED` / `_ENABLED` | NOTICE | TX | gestão de APIs | `audience` |
| `API_CREDENTIAL_ROTATED` | ALERT | TX | credencial de introspecção rotacionada | `audience` |
| `INTROSPECTION_AUTH_FAILED` | NOTICE | IND | API autenticou mal | `audience` |
| `SIGNING_KEY_ROTATED` | ALERT | TX | rotação automática/manual | `kid`,`mode` |
| `SIGNING_KEY_REMOVED` | ALERT | TX | remoção (inclui emergencial) | `kid`,`emergency` |
| `RATE_LIMIT_EXCEEDED` | NOTICE | IND | limite excedido (amostrado: 1 por chave/janela) | `rule` |
| `EMAIL_DELIVERY_FAILED` | ALERT | IND | outbox esgotou tentativas | `template` |
| `RETENTION_PURGE_RUN` | INFO | TX | job de retenção executado | `counts` |
