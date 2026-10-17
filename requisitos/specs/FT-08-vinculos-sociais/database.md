# Database Spec — FT-08 Vínculos sociais
`NEW` · `APPROVED` (documental). Modelo físico: `data-model.md §2.3`.

### DATA-FT08-01 — Vínculo social
Conta, provedor (`GOOGLE|FACEBOOK`), identificador do usuário no provedor (`provider_subject`), e-mail informado pelo provedor (apenas informativo), vinculado em, último login. Unicidade: (provedor, subject) e (conta, provedor).
Reutiliza: Transação social (DATA-FT07-01, com propósito LINK/REAUTH), Sessão (DATA-FT03-01, `last_reauth_at`).

| ID | Integridade | Ref. |
|---|---|---|
| INT-FT08-01 | Uma identidade social pertence a uma só conta; uma conta tem no máximo um vínculo por provedor | DM-03 |
| INT-FT08-02 | Desvincular verifica, na transação e com a conta travada, que resta outro método | DM-08 |
| INT-FT08-03 | O e-mail do provedor nunca é usado para decisões | RN-SOC-11 |
| INT-FT08-04 | Vínculo, evento e e-mail na mesma transação | RN-AUD-01 |

Eventos: `SOCIAL_LINKED`, `SOCIAL_UNLINKED`, `REAUTH_*`. Rastreabilidade: DATA-FT08-01 → CA-FT08-02, 03, 06, 07, 09.
