# API Spec — FT-12 Eventos de segurança e retenção
`NEW` · `APPROVED` (documental).

### API-FT12-01 — Consultar eventos de segurança
`GET /api/v1/admin/security-events` · Bearer + ADMIN · `no-store`.
Query: `from`, `to` (ISO-8601 UTC; intervalo ≤ 90 dias; padrão: últimas 24 h), `type` (repetível), `account` (UUID ou e-mail exato), `client_id`, `outcome` (`SUCCESS|FAILURE`), `severity` (`INFO|NOTICE|ALERT`), `limit` (1–100, padrão 50), `cursor`.
`200 {items:[{id,occurred_at,event_type,severity,outcome,account:{id,email|null},actor_account_id,client_id,session_id,ip,user_agent,request_id,details}],next_cursor}`.
Erros: 400 `VALIDATION_ERROR` (período > 90 dias, tipo desconhecido, cursor inválido); 401; 403 `FORBIDDEN`.
Rastreio: CA-FT12-08…11, 22.

Jobs (UC-091/092): internos, sem API pública; resultado observável por `RETENTION_PURGE_RUN` na API-FT12-01 e por métricas. Rastreio: CA-FT12-12…21.
Rastreabilidade: API-FT12-01 → CA-FT12-08…11. Pontos a validar: nenhum.
