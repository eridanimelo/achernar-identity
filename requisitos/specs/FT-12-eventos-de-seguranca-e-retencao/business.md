# Business Spec — FT-12 Eventos de segurança e retenção

## 0. Identificação
FT-12 (UC-090, UC-091, UC-092) · `NEW` · Módulo M8 Auditoria e manutenção · `APPROVED` (documental). Catálogo: `event-catalog.md`.

## 1. Objetivo
Registrar de forma confiável e imutável os eventos relevantes de autenticação e administração, permitir sua consulta por administradores e aplicar retenção, anonimização e limpeza automáticas dentro de prazos definidos.

## 2. História
**Como** administrador **quero** investigar o que aconteceu com contas e acessos **para** responder a incidentes. **Como** operador **quero** retenção automática **para** cumprir privacidade e manter o banco enxuto.

## 3. Fluxos
### UC-090 — Consultar eventos de segurança
Filtros por período (máx. 90 dias por consulta), tipo (múltiplos), conta (id ou e-mail exato), aplicação, resultado, severidade; paginação por cursor; somente leitura.
### UC-091 — Executar retenção de dados
Job diário (03:00 UTC): remove eventos além de 365 dias, anula IP e agente após 90 dias, remove contas pendentes não verificadas (7 dias), sessões/refresh encerrados (30 dias), tokens de uso único/transações/códigos sociais (7 dias após expirar/consumir), linhas de outbox, contadores ociosos e chaves removidas; registra `RETENTION_PURGE_RUN` com contagens.
### UC-092 — Executar manutenção periódica
Rotinas curtas: expirar/ invalidar pendências, reprocessar envios (FT-13), limpar contadores de limite vencidos, liberar travas de envio expiradas.

## 4. Critérios de aceite
- **CA-FT12-01 — Cobertura.** Cada evento do catálogo é gerado nas condições descritas, com `occurred_at`, tipo, resultado, severidade, `request_id`, conta/ator/cliente/sessão aplicáveis, IP e agente (truncado a 255). Ref.: RF-14, RN-AUD-01
- **CA-FT12-02 — Atomicidade.** Eventos de mudança de estado são gravados na mesma transação da mudança (rollback remove ambos); eventos de falha (`LOGIN_FAILED`, `RATE_LIMIT_EXCEEDED` etc.) usam transação independente e persistem mesmo com a requisição recusada. Ref.: RN-AUD-01
- **CA-FT12-03 — Sem segredos.** `details` só aceita chaves da lista do catálogo; nenhum evento contém senha, token, `code`, `state`, hash ou segredo (verificável por varredura). Ref.: RN-AUD-04, RNF-06
- **CA-FT12-04 — Imutabilidade.** A aplicação não consegue alterar nem excluir eventos, exceto anulação de IP/agente e a purga da retenção. Ref.: RN-AUD-02, SEC-30
- **CA-FT12-05 — Correlação.** O `request_id` do evento coincide com `trace_id` do erro/resposta e dos logs. Ref.: RN-AUD-05
- **CA-FT12-06 — Alertas.** Evento `ALERT` gera log `WARN` e incrementa a métrica `achernar_security_alert_total{type}`. Ref.: RN-AUD-06
- **CA-FT12-07 — Amostragem de limite.** `RATE_LIMIT_EXCEEDED` é registrado no máximo uma vez por chave/janela. Ref.: event-catalog
- **CA-FT12-08 — Consulta.** ADMIN consulta com filtros e cursor; ordem decrescente por tempo; página padrão 50 (máx. 100); `next_cursor` nulo ao final; período > 90 dias ⇒ `VALIDATION_ERROR`; tipo desconhecido ⇒ `VALIDATION_ERROR`. Ref.: RF-33, RN-AUD-03
- **CA-FT12-09 — Filtro por conta.** Por id ou por e-mail exato (normalizado); e-mail inexistente ⇒ lista vazia. Ref.: RN-AUD-03
- **CA-FT12-10 — Autorização da consulta.** Somente ADMIN verificado no banco; usuário comum ⇒ `FORBIDDEN`. Ref.: RN-ADM-01
- **CA-FT12-11 — Conteúdo exibido.** Itens incluem tipo, resultado, severidade, momento, conta (id e e-mail atual ou “anonimizada”), ator, aplicação, sessão, IP/agente (quando ainda existem) e `details` permitidos. Ref.: RN-AUD-03, RF-28
- **CA-FT12-12 — Retenção de eventos.** Eventos com mais de 365 dias são removidos; IP/agente anulados após 90 dias; eventos mais novos permanecem. Ref.: RF-29, RN-RET-02, P-53, P-54
- **CA-FT12-13 — Retenção de contas pendentes.** Contas `PENDING_VERIFICATION` com mais de 7 dias são removidas com seus dados dependentes. Ref.: RN-RET-05
- **CA-FT12-14 — Retenção de sessões.** Sessões/refresh encerrados há mais de 30 dias são removidos; sessões ativas ou recentes permanecem. Ref.: RN-RET-03
- **CA-FT12-15 — Retenção de tokens e transações.** Tokens de uso único, transações e códigos sociais removidos 7 dias após expirar/consumir. Ref.: RN-RET-04
- **CA-FT12-16 — Outros alvos.** Outbox `SENT` > 3 dias e `FAILED` > 7 dias, contadores > 1 dia e chaves `REMOVED` > 30 dias são removidos. Ref.: RN-RET-01, P-41, P-55, P-56, P-59
- **CA-FT12-17 — Contas desativadas.** Permanecem até anonimização solicitada (não são purgadas automaticamente). Ref.: RN-RET-06
- **CA-FT12-18 — Idempotência e lotes.** Executar o job duas vezes seguidas não altera o resultado; remoções ocorrem em lotes (1000) e toleram execução concorrente de duas instâncias sem duplicar nem falhar. Ref.: RN-RET-01, RNF-14
- **CA-FT12-19 — Registro da execução.** Cada execução registra `RETENTION_PURGE_RUN` com contagens por alvo; falhas parciais são registradas e a próxima execução continua. Ref.: RN-RET-01
- **CA-FT12-20 — Parâmetros.** Todos os prazos vêm de configuração com os valores padrão P-50…P-59; valores fora da faixa impedem a subida. Ref.: parameters.md
- **CA-FT12-21 — Manutenção periódica.** Contadores de limite vencidos são limpos e envios travados são liberados; nada disso afeta usuários. Ref.: RF-29, UC-092
- **CA-FT12-22 — Desempenho da consulta.** Consulta com filtros usuais retorna em p95 ≤ 300 ms com 10 milhões de eventos. Ref.: RNF-02 (INFERRED), índices de `data-model.md`

## 5. Regras: RN-AUD-01…06, RN-RET-01…06, D-26, D-27, SEC-30 · Perfis: ADMIN (consulta); sistema (jobs).
## 6. Rastreabilidade: UC-090 → CA-FT12-08…11, 22 · UC-091 → CA-FT12-12…20 · UC-092 → CA-FT12-21 · registro de eventos (transversal) → CA-FT12-01…07.
## 7. Pontos a validar / divergências / LEGACY: nenhum / nenhuma / `NOT_APPLICABLE`.
## 8. Resultado esperado: trilha confiável e consultável, com retenção automática e privacidade preservada.
