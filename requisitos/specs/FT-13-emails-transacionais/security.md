# Security Spec — FT-13 E-mails transacionais
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT13-01 | Conteúdo pendente cifrado e apagado; erro sem dados pessoais | SEC-24 |
| SEC-FT13-02 | Links só de configuração, token no fragmento, sem `Host` | SEC-22, SEC-09 |
| SEC-FT13-03 | Notificações de segurança ao titular nos eventos críticos | SEC-21 |
| SEC-FT13-04 | Sem rastreadores, sem imagens remotas, sem senhas/tokens extras | RN-EML-05 |
| SEC-FT13-05 | SMTP com TLS e autenticação em `prod`; credenciais fora do repositório | RN-EML-08 |
| SEC-FT13-06 | Limites de reenvio contra abuso e e-mail bombing | RN-EML-03 |
| SEC-FT13-07 | SPF/DKIM/DMARC no domínio remetente (checklist de implantação) | SEC-38 |
| SEC-FT13-08 | Sem injeção de cabeçalhos: destinatário e assunto validados e sem quebras de linha | SEC-32 |

Riscos: RSK-FT13-01 vazamento do token por log/erro (redação); RSK-FT13-02 phishing imitando e-mails (remetente autenticado, texto padronizado); RSK-FT13-03 injeção em cabeçalhos (validação); RSK-FT13-04 e-mail bombing (limites).
Rastreabilidade: SEC-FT13-01 → CA-FT13-05 · -02 → CA-FT13-09 · -03 → CA-FT13-13 · -04 → CA-FT13-10 · -05 → CA-FT13-14 · -06 → CA-FT13-12 · -07 → CA-FT13-14 · -08 → CA-FT13-11.
