# Test Spec — FT-12 Eventos de segurança e retenção
Cenários para execução futura. `APPROVED` (documental).

- **CT-FT12-01** Matriz catálogo × gatilho: cada evento é gerado e tem campos obrigatórios. CA-FT12-01
- **CT-FT12-02** Rollback remove evento de mudança; falha (login inválido) persiste mesmo recusando. CA-FT12-02
- **CT-FT12-03** Varredura de segredos sentinela e chaves não permitidas em `details`. CA-FT12-03
- **CT-FT12-04** Tentativa de UPDATE/DELETE pela conexão da aplicação falha (exceto IP/agente). CA-FT12-04
- **CT-FT12-05** `request_id` ↔ `trace_id` ↔ log. CA-FT12-05
- **CT-FT12-06** Evento `ALERT` ⇒ log WARN + métrica. CA-FT12-06
- **CT-FT12-07** Rajada de excessos de limite ⇒ 1 evento por chave/janela. CA-FT12-07
- **CT-FT12-08** Consulta: filtros, cursor, ordenação, limites de página e de período. CA-FT12-08
- **CT-FT12-09** Filtro por e-mail exato/ inexistente. CA-FT12-09
- **CT-FT12-10** Autorização da consulta. CA-FT12-10
- **CT-FT12-11** Conteúdo exibido; conta anonimizada mostra “anonimizada”. CA-FT12-11
- **CT-FT12-12** Retenção de eventos (364/366 dias; 89/91 dias para IP). CA-FT12-12
- **CT-FT12-13** Contas pendentes (6/8 dias). CA-FT12-13
- **CT-FT12-14** Sessões encerradas (29/31 dias) e ativas intactas. CA-FT12-14
- **CT-FT12-15** Tokens/transações/códigos (6/8 dias). CA-FT12-15
- **CT-FT12-16** Outbox, contadores e chaves removidas. CA-FT12-16
- **CT-FT12-17** Contas desativadas não purgadas. CA-FT12-17
- **CT-FT12-18** Duas execuções simultâneas e repetição idempotente; lotes. CA-FT12-18
- **CT-FT12-19** Registro de execução com contagens; falha parcial seguida de retomada. CA-FT12-19
- **CT-FT12-20** Parâmetros fora da faixa impedem a subida. CA-FT12-20
- **CT-FT12-21** Manutenção periódica de contadores e travas. CA-FT12-21
- **CT-FT12-22** Desempenho da consulta com 10 milhões de eventos. CA-FT12-22
- **CT-FT12-23** UI SCR-21 (filtros, expansão, `axe`). CA-FT12-08, 11
