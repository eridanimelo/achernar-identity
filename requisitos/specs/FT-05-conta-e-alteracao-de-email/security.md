# Security Spec — FT-05 Conta e alteração de e-mail
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT05-01 | Reautenticação recente para solicitar a troca | RN-PWD-08 |
| SEC-FT05-02 | Resposta neutra; nenhum e-mail ao titular de outra conta | SEC-07 |
| SEC-FT05-03 | Token 256 bits, hash, uso único, 1 h, fragmento de URL | SEC-09 |
| SEC-FT05-04 | Aviso ao endereço atual na solicitação e na conclusão | SEC-21 |
| SEC-FT05-05 | Revogação de todas as sessões e invalidação de tokens de recuperação | RN-ACC-15 |
| SEC-FT05-06 | Resposta de conta sem dados sensíveis; `no-store` | SEC-35 |
| SEC-FT05-07 | Sessão validada no banco a cada chamada | SEC-34 |

Riscos: RSK-FT05-01 sequestro de conta via sessão roubada (reautenticação + aviso ao endereço antigo + revogação); RSK-FT05-02 uso do fluxo para enumerar e-mails (neutralidade); RSK-FT05-03 corrida na unicidade (revalidação transacional).
Rastreabilidade: SEC-FT05-01 → CA-FT05-03 · -02 → CA-FT05-04, 05 · -03 → CA-FT05-09 · -04 → CA-FT05-05, 08 · -05 → CA-FT05-08 · -06 → CA-FT05-01 · -07 → CA-FT05-02.
