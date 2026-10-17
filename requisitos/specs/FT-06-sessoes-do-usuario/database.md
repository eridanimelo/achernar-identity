# Database Spec — FT-06 Sessões do usuário
`NEW` · `APPROVED` (documental).

### DATA-FT06-01 — Visão de sessões ativas
Consulta sobre Sessão (DATA-FT03-01) e Aplicação (DATA-FT09-01): ativa = não revogada e `expires_at` futuro. Campos exibidos derivados: IP aproximado (máscara), agente resumido.
| ID | Integridade | Ref. |
|---|---|---|
| INT-FT06-01 | Revogação idempotente (preserva o primeiro motivo e instante) | RN-SES-03 |
| INT-FT06-02 | “Encerrar todas” revoga em uma transação e registra contagem | RN-AUD-01 |
| INT-FT06-03 | Revogação de sessão não apaga refresh tokens (preservados para detecção de reuso até a purga P-52) | RN-RET-03 |
| INT-FT06-04 | Toda consulta filtra por conta do solicitante | RN-SES-04 |

Eventos: `SESSION_REVOKED`, `ALL_SESSIONS_REVOKED`. Rastreabilidade: DATA-FT06-01 → CA-FT06-01, 02, 08, 11.
