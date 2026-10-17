# Frontend Spec — FT-05 Conta e alteração de e-mail
`NEW` · `APPROVED` (documental) · SCR-09 Conta, SCR-10 (seção e-mail), SCR-12 Confirmar troca, SCR-11 (reautenticação).

- **SCR-09 Conta (`/account`)**: somente leitura — e-mail com selo “verificado”, estado, perfil, idioma, métodos de acesso (senha, Google, Facebook), data de criação; atalhos para Segurança e Sessões. Estados: skeleton; erro com *Tentar novamente*; 401 ⇒ fluxo de sessão expirada.
- **SCR-10 seção *Alterar e-mail***: campo *Novo e-mail* (obrigatório, formato, ≤ 254); abre SCR-11 se `REAUTH_REQUIRED`; sucesso mostra mensagem neutra “Se o endereço for válido, enviamos um link de confirmação ao novo e-mail”; `RATE_LIMITED` com aviso.
- **SCR-12 Confirmar troca (`/confirm-email-change#token=`)**: lê e limpa o fragmento, chama a confirmação; sucesso: “E-mail alterado. Entre novamente.” e botão para login (sessões encerradas); erro: `TOKEN_INVALID_OR_EXPIRED` com link para refazer o pedido.
Acessibilidade: `h1` por tela, mensagens em `aria-live`, foco gerenciado.
Rastreabilidade: SCR-09 → CA-FT05-01 · SCR-10 → CA-FT05-03, 04, 07, 13 · SCR-12 → CA-FT05-08, 09.
