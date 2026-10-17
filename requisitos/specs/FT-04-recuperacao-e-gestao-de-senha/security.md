# Security Spec — FT-04 Recuperação e gestão de senha
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT04-01 | Anti-enumeração na solicitação (resposta, status, tempo) | SEC-07 |
| SEC-FT04-02 | Token 256 bits, hash, uso único, 30 min, no fragmento; um ativo por conta | SEC-09 |
| SEC-FT04-03 | Revogação de todas as sessões após redefinição; demais após troca | RN-TOK-10 |
| SEC-FT04-04 | Reautenticação recente (5 min) para operações sensíveis; falhas contam no bloqueio | RN-PWD-08, SEC-05 |
| SEC-FT04-05 | Notificação ao titular após alteração/definição/redefinição | SEC-21 |
| SEC-FT04-06 | Link construído só de configuração; sem Host | SEC-22 |
| SEC-FT04-07 | Senha nunca em log/evento/e-mail | SEC-02, SEC-31 |

Riscos: RSK-FT04-01 tomada de conta por caixa de e-mail comprometida (mitiga: expira em 30 min, notifica, revoga sessões); RSK-FT04-02 bombardeio de e-mails (limites); RSK-FT04-03 força bruta de token (256 bits + limite P-31); RSK-FT04-04 reautenticação usada para adivinhar senha (compartilha P-30).
Rastreabilidade: SEC-FT04-01 → CA-FT04-01 · -02 → CA-FT04-03, 06, 08 · -03 → CA-FT04-05, 11 · -04 → CA-FT04-12, 14 · -05 → CA-FT04-05, 11, 16 · -06 → CA-FT04-19 · -07 → CA-FT04-18.
