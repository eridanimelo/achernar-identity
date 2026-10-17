# API Spec — FT-13 E-mails transacionais
`NEW` · `APPROVED` (documental).

Não há endpoints próprios: o envio é assíncrono e interno. O comportamento observável por contrato é:
- **API-FT13-01 — Efeito colateral dos endpoints de origem.** Cadastro, reenvio, recuperação, troca de e-mail, desativação etc. respondem sem aguardar o SMTP (status e tempo independentes de entrega). Rastreio: CA-FT13-02.
- **API-FT13-02 — Contrato com o servidor SMTP.** SMTP padrão (RFC 5321) com STARTTLS/SMTPS e autenticação em `prod`. Rastreio: CA-FT13-14.
- **API-FT13-03 — Conteúdo dos links.** Formato `<URL de ação>#token=<token>` (fragmento); parâmetros adicionais não são anexados. Rastreio: CA-FT13-09.
- **API-FT13-04 — Observabilidade.** Métricas `achernar_mail_outbox_pending`, `achernar_mail_outbox_failed_total` e eventos `EMAIL_DELIVERY_FAILED` consultáveis em FT-12. Rastreio: CA-FT13-04, 15.

Rastreabilidade: API-FT13-01 → CA-FT13-01, 02 · -02 → CA-FT13-14 · -03 → CA-FT13-09 · -04 → CA-FT13-04, 15. Pontos a validar: nenhum.
