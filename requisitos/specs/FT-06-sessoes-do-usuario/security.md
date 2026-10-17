# Security Spec — FT-06 Sessões do usuário
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT06-01 | Isolamento por conta; id alheio = inexistente | RN-SES-04 |
| SEC-FT06-02 | Reautenticação para encerrar todas | RN-PWD-08 |
| SEC-FT06-03 | Sessão validada no banco a cada chamada | SEC-34 |
| SEC-FT06-04 | Minimização de IP/agente exibidos | RNF-15 |
| SEC-FT06-05 | Notificação por e-mail do encerramento global | SEC-21 |
| SEC-FT06-06 | Idempotência e atomicidade da revogação | RNF-20 |

Riscos: RSK-FT06-01 enumeração de ids de sessão (UUID + `NOT_FOUND` uniforme); RSK-FT06-02 sessão roubada encerra as demais (reautenticação exigida; atual mantida só por opção); RSK-FT06-03 dados de localização excessivos (máscara).
Rastreabilidade: SEC-FT06-01 → CA-FT06-02 · -02 → CA-FT06-07 · -03 → CA-FT06-10 · -04 → CA-FT06-11 · -05 → CA-FT06-06 · -06 → CA-FT06-09.
