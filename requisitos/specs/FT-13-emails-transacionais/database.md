# Database Spec — FT-13 E-mails transacionais
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.13`.

### DATA-FT13-01 — Mensagem pendente (caixa de saída)
Id (ordenável), modelo (TPL-xx), idioma, destinatário, conta e aplicação de origem (opcionais), conteúdo de renderização **cifrado** (apagado ao enviar/falhar), estado (`PENDING|SENT|FAILED`), tentativas, próxima tentativa, reserva (até), último erro (≤ 300, sem dados pessoais), criação, envio.

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT13-01 | Gravação na mesma transação da operação de negócio | RN-EML-02 |
| INT-FT13-02 | Coleta por reserva exclusiva com ignorar-bloqueadas; reserva expira em 2 min | RNF-14 |
| INT-FT13-03 | Conteúdo cifrado com a chave-mestra e apagado após uso | SEC-24 |
| INT-FT13-04 | Estados e transições fechados; `attempts` ≤ 6 | P-40 |
| INT-FT13-05 | Índice de mensagens vencidas e de limpeza | RNF-02 |

Retenção: `SENT` 3 dias (P-55), `FAILED` 7 dias (P-41). Rastreabilidade: DATA-FT13-01 → CA-FT13-01, 03…06.
