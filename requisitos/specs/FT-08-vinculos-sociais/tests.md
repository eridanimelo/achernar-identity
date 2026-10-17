# Test Spec — FT-08 Vínculos sociais
Cenários para execução futura (provedores simulados). `APPROVED` (documental).

- **CT-FT08-01** Intenção de vínculo: com/sem reautenticação, provedor desabilitado, `return_url` inválida. CA-FT08-01
- **CT-FT08-02** Vínculo concluído; e-mail do provedor diferente do da conta não interfere; TPL-10. CA-FT08-02, 05
- **CT-FT08-03** Identidade de outra conta; provedor duplicado ⇒ `already_linked`. CA-FT08-03
- **CT-FT08-04** Sessão revogada durante o fluxo. CA-FT08-04
- **CT-FT08-05** Desvincular com senha/outro vínculo. CA-FT08-06
- **CT-FT08-06** Último método ⇒ `LAST_ACCESS_METHOD`. CA-FT08-07
- **CT-FT08-07** Desvincular inexistente. CA-FT08-08
- **CT-FT08-08** Concorrência: dois desvínculos simultâneos; dois vínculos do mesmo provedor. CA-FT08-09
- **CT-FT08-09** Intenção de reautenticação social. CA-FT08-10
- **CT-FT08-10** Reautenticação válida abre janela de 5 min. CA-FT08-11
- **CT-FT08-11** Identidade diferente ⇒ `reauth_failed`. CA-FT08-12
- **CT-FT08-12** Conta desativada durante o fluxo. CA-FT08-13
- **CT-FT08-13** Limite 30/min. CA-FT08-14
- **CT-FT08-14** Lista de vínculos refletida em `GET /api/v1/account`. CA-FT08-15
- **CT-FT08-15** UI SCR-17 e SCR-11 (estados, teclado, `axe`). CA-FT08-01, 06, 07, 11
