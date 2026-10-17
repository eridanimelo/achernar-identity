# Frontend Spec — FT-04 Recuperação e gestão de senha
`NEW` · `APPROVED` (documental) · SCR-07, SCR-08, SCR-10 (seção senha), SCR-11, SCR-13.

- **SCR-07 Esqueci a senha (`/forgot-password`)**: campo e-mail; sempre mensagem neutra “Se existir uma conta, enviaremos instruções”; `RATE_LIMITED` com contagem regressiva.
- **SCR-08 Redefinir senha (`/reset-password#token=`)**: token do fragmento removido da barra; *Nova senha* + *Confirmar* (política, medidor); sucesso ⇒ “Senha alterada” e link ao login; `TOKEN_INVALID_OR_EXPIRED` ⇒ mensagem e link para nova solicitação; política violada mantém o formulário.
- **SCR-10 Segurança (`/account/security`)** seção *Alterar senha*: *Senha atual*, *Nova*, *Confirmar*; exibida apenas se a conta tem senha; sucesso informa que as outras sessões foram encerradas.
- **SCR-13 Definir senha local**: exibida só para conta sem senha; dispara reautenticação social (SCR-11) se necessário; campos *Nova*/*Confirmar*.
- **SCR-11 Reautenticação (diálogo)**: pede a senha (ou botão do provedor vinculado); foco no diálogo, `Esc` fecha; falha mostra erro no campo; após sucesso repete a ação original automaticamente.
Estados comuns: carregando, enviando, erro por campo, sucesso, indisponível. Acessibilidade: diálogos modais com retorno de foco; medidor com texto.
Rastreabilidade: SCR-07 → CA-FT04-01, 04 · SCR-08 → CA-FT04-05…07 · SCR-10 → CA-FT04-11…13 · SCR-11 → CA-FT04-14, 15 · SCR-13 → CA-FT04-16.
