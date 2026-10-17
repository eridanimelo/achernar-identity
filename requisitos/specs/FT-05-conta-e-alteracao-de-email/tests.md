# Test Spec — FT-05 Conta e alteração de e-mail
Cenários para execução futura; não representa execução. `APPROVED` (documental).

- **CT-FT05-01** Consulta retorna campos esperados e nada sensível. CA-FT05-01
- **CT-FT05-02** Sem Bearer; `sid` revogado; conta desativada; token sem audiência do Identity. CA-FT05-02
- **CT-FT05-03** Solicitação sem reautenticação. CA-FT05-03
- **CT-FT05-04** Neutralidade: e-mail livre/ocupado/igual ao atual (corpo e tempo). CA-FT05-04
- **CT-FT05-05** E-mails enviados conforme o caso (MailHog). CA-FT05-05
- **CT-FT05-06** Segunda solicitação invalida a primeira. CA-FT05-06
- **CT-FT05-07** 4ª solicitação no dia ⇒ 429. CA-FT05-07
- **CT-FT05-08** Confirmação completa (e-mail, sessões revogadas, tokens de recuperação invalidados, TPL-07, `sub` igual). CA-FT05-08
- **CT-FT05-09** Token inválido em todas as variantes. CA-FT05-09
- **CT-FT05-10** Endereço tomado entre pedido e confirmação. CA-FT05-10
- **CT-FT05-11** Confirmações simultâneas e dois usuários disputando o mesmo endereço. CA-FT05-11
- **CT-FT05-12** Conta social troca e-mail (reautenticação por provedor) e mantém vínculo. CA-FT05-12
- **CT-FT05-13** E-mail inválido/255 caracteres. CA-FT05-13
- **CT-FT05-14** Cadastro do endereço por terceiro durante pedido pendente. CA-FT05-14
- **CT-FT05-15** UI SCR-09/10/12 (estados, teclado, `axe`). CA-FT05-01, 03, 08
