# Frontend Spec — FT-07 Login social
`NEW` · `APPROVED` (documental) · SCR-15 Botões sociais, SCR-16 Retorno do social (`/auth/callback`).

- **SCR-15**: botões “Continuar com Google/Facebook” exibidos conforme a configuração pública da aplicação; ao clicar gera `code_verifier` (guardado em `sessionStorage` da aba, com try/catch) e `state`, calcula S256 e navega ao `start`. Desabilitados durante a navegação; texto alternativo e foco visível; marcas respeitando as diretrizes dos provedores.
- **SCR-16**: lê `code`/`state`/`error` da query, valida o `state` com o guardado, remove a query da barra e chama a troca; estados: “Concluindo login…”, sucesso ⇒ navega à rota inicial, erros mapeados: `access_denied` (“Login cancelado”), `account_link_required` (“Já existe uma conta com este e-mail. Entre com sua senha e vincule o provedor em Segurança.”), `email_verification_required` (“Confirme seu e-mail para continuar”), `email_unavailable`, `account_disabled`, `temporarily_unavailable` (tentar novamente), `invalid_request`, `INVALID_GRANT`; cada erro com ação (voltar ao login).
Acessibilidade: mensagens em `role="alert"`; título único.
Rastreabilidade: SCR-15 → CA-FT07-01, 22 · SCR-16 → CA-FT07-04…12, 14, 15.
