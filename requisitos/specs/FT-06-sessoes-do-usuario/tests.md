# Test Spec — FT-06 Sessões do usuário
Cenários para execução futura; não representa execução. `APPROVED` (documental).

- **CT-FT06-01** Listagem correta, ordem, campos e “current”. CA-FT06-01
- **CT-FT06-02** Isolamento: id de outra conta e id inexistente produzem a mesma resposta. CA-FT06-02
- **CT-FT06-03** Revogar uma; refresh da sessão passa a falhar; idempotência. CA-FT06-03
- **CT-FT06-04** Revogar a atual. CA-FT06-04
- **CT-FT06-05** Efeito sobre access: Identity API recusa; introspecção `active=false`; API sem introspecção aceita até `exp`. CA-FT06-05
- **CT-FT06-06** Encerrar todas com/sem `keep_current`; TPL-11; evento com contagem. CA-FT06-06
- **CT-FT06-07** Sem reautenticação ⇒ `REAUTH_REQUIRED`, nada revogado. CA-FT06-07
- **CT-FT06-08** Sessões expiradas/revogadas não listadas. CA-FT06-08
- **CT-FT06-09** Revogações simultâneas; “todas” simultâneo a login. CA-FT06-09
- **CT-FT06-10** Sem Bearer; `sid` revogado. CA-FT06-10
- **CT-FT06-11** Máscara de IP (IPv4 /24, IPv6 /48) e resumo do agente. CA-FT06-11
- **CT-FT06-12** UI SCR-14 (estados, diálogos, teclado, `axe`). CA-FT06-01, 03, 06
