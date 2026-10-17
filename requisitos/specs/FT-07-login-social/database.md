# Database Spec — FT-07 Login social
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.9, 2.10`.

### DATA-FT07-01 — Transação social
`state` (hash), provedor, propósito (`LOGIN|LINK|REAUTH`), aplicação, `redirect_uri`, `state` do cliente, `code_challenge` do cliente, audiências, `nonce`, verificador PKCE do provedor (cifrado), amarração ao navegador (hash), conta/sessão (LINK/REAUTH), criação, expiração (10 min), consumo.
### DATA-FT07-02 — Código de troca
Hash do código, conta, aplicação, provedor, audiências, `redirect_uri`, `code_challenge`, criação, expiração (60 s), consumo e sessão criada (para revogar em caso de reuso).
Reutiliza: Conta (DATA-FT02-01), Vínculo social (DATA-FT08-01), Sessão (DATA-FT03-01).

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT07-01 | Consumo único e atômico de transação e de código | DM-11 |
| INT-FT07-02 | Identidade social única na instalação | DM-03 |
| INT-FT07-03 | Criação de conta, vínculo, remoção de pendente substituída, evento e e-mail na mesma transação | RN-AUD-01 |
| INT-FT07-04 | Nenhum token do provedor é gravado (nem cifrado) | RN-SOC-14 |
| INT-FT07-05 | Código vincula `client_id`+`redirect_uri`+`code_challenge` | SEC-18 |

Retenção: P-51. Eventos: `SOCIAL_*`, `PENDING_ACCOUNT_SUPERSEDED`. Rastreabilidade: DATA-FT07-01 → CA-FT07-01, 11 · -02 → CA-FT07-14…18.
