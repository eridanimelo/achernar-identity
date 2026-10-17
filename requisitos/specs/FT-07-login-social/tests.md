# Test Spec — FT-07 Login social
Cenários para execução futura (provedores simulados por servidores de teste que emulam OIDC/Graph; sem credenciais reais). `APPROVED` (documental).

- **CT-FT07-01** Início válido por provedor (parâmetros, cookie). CA-FT07-01
- **CT-FT07-02** Início inválido: cliente, redirect, challenge, método, provedor desabilitado; nunca redireciona. CA-FT07-02
- **CT-FT07-03** Limite 30/min. CA-FT07-03
- **CT-FT07-04** Vínculo existente: ativa, desativada, pendente. CA-FT07-04
- **CT-FT07-05** Conta nova (Google verificado). CA-FT07-05
- **CT-FT07-06** Conta pendente (Facebook) + TPL-01. CA-FT07-06
- **CT-FT07-07** Sem e-mail. CA-FT07-07
- **CT-FT07-08** E-mail verificado coincidente com ativa/desativada ⇒ `account_link_required`, nada alterado. CA-FT07-08
- **CT-FT07-09** E-mail não verificado coincidente ⇒ mesma resposta que inexistente; TPL-05 enviado. CA-FT07-09
- **CT-FT07-10** Substituição de conta pendente. CA-FT07-10
- **CT-FT07-11** State: inexistente, reutilizado, expirado, cookie ausente/diferente. CA-FT07-11
- **CT-FT07-12** Cancelamento, timeout, ID token inválido (cada claim). CA-FT07-12
- **CT-FT07-13** Varredura de logs/banco/URLs por tokens do provedor sentinela. CA-FT07-13
- **CT-FT07-14** Troca válida (BROWSER e CONFIDENTIAL). CA-FT07-14
- **CT-FT07-15** Troca inválida em todas as variantes ⇒ `INVALID_GRANT`. CA-FT07-15
- **CT-FT07-16** Reuso do código revoga sessão do primeiro uso. CA-FT07-16
- **CT-FT07-17** 20 trocas simultâneas ⇒ 1 sessão. CA-FT07-17
- **CT-FT07-18** Conta desativada entre callback e troca. CA-FT07-18
- **CT-FT07-19** Limite da troca. CA-FT07-19
- **CT-FT07-20** Audiências conforme política. CA-FT07-20
- **CT-FT07-21** `state` do cliente de 500/501 caracteres. CA-FT07-21
- **CT-FT07-22** Provedor sem credenciais: botão ausente e erro. CA-FT07-22
- **CT-FT07-23** E2E UI SCR-15/16 com provedor simulado (sucesso e erros), `axe`. CA-FT07-04, 12, 14
