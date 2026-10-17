# Achernar Identity — Migrações, Backup e Restauração

Decisões D-36, D-27; requisitos RNF-12, SEC-39. Ferramenta: Liquibase (D-03). `hibernate.ddl-auto=validate`.

## 1. Política de migrações

| Regra | Descrição |
|---|---|
| Local | `backend/src/main/resources/db/changelog/` (já previsto no `.gitignore`) |
| Mestre | `db.changelog-master.yaml` inclui `changes/NNNN-descricao.yaml` em ordem |
| Identificação | `changeSet id="NNNN-k" author="achernar"`; `NNNN` sequencial de 4 dígitos; um propósito lógico por arquivo |
| Imutabilidade | changeSet aplicado **nunca** é editado; correções = novo changeSet |
| Compatibilidade | padrão **expandir → migrar → contrair**: adicionar colunas/tabelas (nullable ou com default) → backfill → só então remover/endurecer em release seguinte; permite *rollback* de aplicação sem restaurar banco |
| Destrutivas | `DROP`/`ALTER … TYPE` exigem avaliação de impacto registrada no PR e backup prévio (rules/database) |
| Rollback | cada changeSet declara `rollback` quando seguro (ex.: `dropTable` de tabela nova); migrações de dados irreversíveis marcam `rollback: empty` e são tratadas por backup |
| Pré-condições | `preConditions` com `onFail: HALT` para estados inesperados |
| Execução | na subida da aplicação (papel `migrator`); trava `databasechangeloglock` garante uma instância; em `prod` com várias instâncias, preferir executar a migração em *init container*/passo de deploy dedicado (`ACHERNAR_LIQUIBASE_ENABLED=false` nas réplicas) |
| Seeds | **nenhum dado com credenciais**; a aplicação `achernar-identity-ui` e a primeira chave são criadas pela aplicação na subida (idempotente) |
| Testes | CI aplica todo o changelog em PostgreSQL limpo e valida contra o mapeamento JPA (`validate`); teste de “migração a partir da versão anterior” antes de cada release |
| Papéis | `initdb/01-roles.sql`: `achernar_migrator` (DDL), `achernar_app` (DML; `REVOKE UPDATE ON security_event`; `GRANT UPDATE (ip, user_agent)`); extensões não necessárias |

## 2. Changelog inicial (ordem de criação)

| Arquivo | Conteúdo | Dependências |
|---|---|---|
| `0001-account.yaml` | `account`, `password_credential`, `social_link` + índices/CHECK de `data-model.md §2.1–2.3` | — |
| `0002-registry.yaml` | `resource_api`, `resource_api_secret`, `application`, `application_origin`, `application_redirect_uri`, `application_audience`, `application_secret` | — |
| `0003-session.yaml` | `session`, `session_audience`, `refresh_token` | 0001, 0002 |
| `0004-one-time-and-social.yaml` | `one_time_token`, `social_transaction`, `social_exchange_code` | 0001, 0002, 0003 |
| `0005-signing-key.yaml` | `signing_key` | — |
| `0006-security-event.yaml` | `security_event` + índices | — |
| `0007-email-outbox.yaml` | `email_outbox` | — |
| `0008-rate-limit.yaml` | `rate_limit_counter`, `login_lock` | — |

Entregues por Wave (`implementation-plan.md`): cada Wave só adiciona os arquivos de que precisa (ex.: Wave 0 ⇒ 0005–0008; Wave 1 ⇒ 0001–0003…), mantendo a numeração acima
(números reservados e não reutilizados).

## 3. Backup

| Item | Política |
|---|---|
| Escopo | banco PostgreSQL completo (inclui chaves privadas **cifradas**, eventos, contas) |
| Método | `pg_dump -Fc` diário (03:30 UTC, após os jobs) pelo serviço `backup`; opcional PITR via arquivamento de WAL em ambientes com RPO < 24 h |
| Retenção | 7 diários, 4 semanais, 3 mensais |
| Destino | volume dedicado **e cópia fora do host** (objeto/armazenamento remoto), criptografado em repouso (ex.: `age`/GPG ou criptografia do armazenamento) — SEC-39 |
| Segredos | `ACHERNAR_MASTER_KEY` e `BOOTSTRAP_TOKEN` guardados em cofre **separado** do backup; sem a chave-mestra o backup restaurado não decifra as chaves de assinatura |
| Verificação | checagem de integridade (`pg_restore --list`) a cada backup; restauração completa em ambiente descartável a cada trimestre (RNF-12) |
| Metas | RPO ≤ 24 h; RTO ≤ 4 h |
| Monitoração | alerta se último backup bem-sucedido > 26 h |

## 4. Restauração (runbook)

1. Declarar incidente; colocar a API em manutenção (proxy retorna 503) para evitar escrita durante a restauração.
2. Provisionar PostgreSQL limpo (mesma versão major ou superior) e papéis (`initdb/01-roles.sql`).
3. `pg_restore --clean --if-exists -d <db> <arquivo>` com o papel dono.
4. Subir a **mesma versão** da aplicação do backup (ou a próxima, se compatível); Liquibase concluirá migrações pendentes.
5. Garantir `ACHERNAR_MASTER_KEY` original; se perdida ⇒ runbook “Perda da chave-mestra”.
6. **Pós-restauração obrigatória (RISK-05):** o backup pode conter sessões/refresh tokens que foram **revogados depois** do backup e agora voltariam a valer:
   ```sql
   UPDATE session SET revoked_at = now(), revoke_reason = 'KEY_COMPROMISE' WHERE revoked_at IS NULL;  -- executar como papel app
   TRUNCATE rate_limit_counter; UPDATE login_lock SET locked_until = NULL;
   ```
   (`revoke_reason` reutilizado por não haver motivo específico; registrar evento manual `ALL_SESSIONS_REVOKED` com `details.count`.)
7. Verificar: `readiness`, JWKS, login de um usuário de teste, fila de e-mails; reabrir tráfego.
8. Avisar aplicações consumidoras: tokens existentes continuam válidos por assinatura até `exp` (≤ 10 min); usuários refarão login.
9. Post-mortem e ajuste de RPO se necessário.

Restauração parcial (uma conta apagada por engano) não é suportada: contas não são excluídas (apenas anonimizadas) — recuperar de backup em ambiente separado e reaplicar manualmente com aprovação.

## 5. Atualização de esquema em produção

1. Backup manual imediato. 2. Janela curta de manutenção só para migrações **não** compatíveis (excepcionais). 3. Aplicar changelog. 4. Subir nova versão. 5. Smoke tests (login, refresh, JWKS, e-mail). 6. Manter backup anterior ≥ 30 dias.
Mudanças futuras no tamanho da chave RSA, algoritmo ou `iss` exigem plano próprio de transição (D-04, D-25) — não são migrações de banco.

## 6. Ambientes de desenvolvimento

Volume `achernar_identity_pgdata` pode ser recriado a qualquer momento (`docker compose down -v`): sem dados valiosos; o setup cria novo administrador. Dumps de produção **não** devem ser usados em dev (dados pessoais).
