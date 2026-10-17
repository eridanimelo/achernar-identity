# Database Spec — FT-12 Eventos de segurança e retenção
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.12, §4`.

### DATA-FT12-01 — Evento de segurança
Id (ordenável no tempo), momento, tipo (domínio do catálogo), severidade, resultado, conta e ator (**sem vínculo físico** para sobreviver a purgas), cliente, sessão, IP, agente (≤ 255), `request_id`, detalhes estruturados (somente chaves permitidas).
### DATA-FT12-02 — Estado de execução da retenção
Os resultados de cada execução são registrados como eventos `RETENTION_PURGE_RUN` (sem tabela própria); não há estado persistente adicional.

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT12-01 | Papel de aplicação sem alteração de eventos (exceto anulação de IP/agente) | SEC-30, RN-AUD-02 |
| INT-FT12-02 | Eventos de mudança de estado na mesma transação; falhas em transação independente | RN-AUD-01 |
| INT-FT12-03 | Índices por tempo, conta, tipo, cliente e alertas | RNF-02 |
| INT-FT12-04 | Purga em lotes com travas que ignoram linhas bloqueadas; idempotente | RN-RET-01 |
| INT-FT12-05 | Whitelist de chaves em `details` aplicada antes de gravar | RN-AUD-04 |
| INT-FT12-06 | Ao anonimizar a conta, o e-mail é substituído ao exibir/gravar eventos (nenhum e-mail em claro armazenado fora de `details` permitido) | RN-ACC-13 |

Retenção: ver CA-FT12-12…19. Rastreabilidade: DATA-FT12-01 → CA-FT12-01…05, 08…12.
