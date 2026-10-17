# Test Spec — FT-04 Recuperação e gestão de senha
Cenários para execução futura; não representa execução. `APPROVED` (documental).

- **CT-FT04-01** Neutralidade/tempo para 5 estados de e-mail. CA-FT04-01
- **CT-FT04-02** E-mail enviado só quando cabível (ativa/pendente/outros). CA-FT04-02
- **CT-FT04-03** Segunda solicitação invalida o primeiro token. CA-FT04-03
- **CT-FT04-04** Limites 3/h e-mail, 10/h IP. CA-FT04-04
- **CT-FT04-05** Redefinição completa (sessões revogadas, bloqueio zerado, tokens invalidados, TPL-06, sem sessão criada). CA-FT04-05
- **CT-FT04-06** Token inexistente/expirado (30 min+1 s)/usado/invalidado/outro tipo. CA-FT04-06
- **CT-FT04-07** Política violada não consome token (nova tentativa válida passa). CA-FT04-07
- **CT-FT04-08** 20 redefinições simultâneas ⇒ 1 sucesso. CA-FT04-08
- **CT-FT04-09** Desativação invalida token; recuperação não reativa. CA-FT04-09
- **CT-FT04-10** Conta social sem senha define senha por recuperação; vínculo mantido. CA-FT04-10
- **CT-FT04-11** Alterar senha: demais sessões revogadas, atual mantida. CA-FT04-11
- **CT-FT04-12** Sem reautenticação ⇒ `REAUTH_REQUIRED`; senha atual errada ⇒ `REAUTH_FAILED` e conta no bloqueio. CA-FT04-12
- **CT-FT04-13** Nova senha igual à atual. CA-FT04-13
- **CT-FT04-14** Reautenticar: sucesso, falha, limite 10/15 min, janela expira em 5 min. CA-FT04-14
- **CT-FT04-15** Conta só social em reautenticação por senha. CA-FT04-15
- **CT-FT04-16** Definir senha local (com/sem reautenticação, conta com senha). CA-FT04-16
- **CT-FT04-17** Sem Bearer / `sid` revogado. CA-FT04-17
- **CT-FT04-18** Logs e eventos sem senha/token; hash Argon2id com parâmetros vigentes. CA-FT04-18
- **CT-FT04-19** Link do e-mail com URL da app e fragmento; `Host` forjado ignorado. CA-FT04-19
- **CT-FT04-20** UI SCR-07/08/10/11/13 (estados, foco, teclado, `axe`). CA-FT04-01, 05, 11, 14, 16
