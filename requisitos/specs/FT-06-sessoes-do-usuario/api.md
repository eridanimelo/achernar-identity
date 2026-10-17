# API Spec — FT-06 Sessões do usuário
`NEW` · `APPROVED` (documental). Todas: Bearer (audiência do Identity), `Cache-Control: no-store`.

### API-FT06-01 — Listar sessões
`GET /api/v1/sessions` · consulta. Sucesso `200`: `{ "items": [ { "id","client_id","application_name","created_at","last_used_at","ip_approx","user_agent_summary","amr","current" } ] }` (sem paginação; no máximo P-05 itens). Erros: 401. Rastreio: CA-FT06-01, 02, 08, 10, 11.
### API-FT06-02 — Revogar uma sessão
`DELETE /api/v1/sessions/{id}` · comando idempotente. Sucesso `204`. Erros: 404 `NOT_FOUND` (inexistente ou de outra conta), 401. Rastreio: CA-FT06-02…05, 09.
### API-FT06-03 — Encerrar todas
`DELETE /api/v1/sessions?keep_current=true|false` (padrão `false`) · comando; exige reautenticação. Sucesso `204`. Erros: 403 `REAUTH_REQUIRED`, 401. Rastreio: CA-FT06-06, 07, 09.

Rastreabilidade: API-FT06-01 → CA-FT06-01, 08 · -02 → CA-FT06-03, 04 · -03 → CA-FT06-06, 07. Pontos a validar: nenhum.
