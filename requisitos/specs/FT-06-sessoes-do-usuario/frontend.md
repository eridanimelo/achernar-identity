# Frontend Spec — FT-06 Sessões do usuário
`NEW` · `APPROVED` (documental) · SCR-14 Minhas sessões (`/account/sessions`), SCR-11.

Lista (tabela em telas largas, cartões em estreitas): aplicação, criada, último uso, local aproximado, agente, método (chip), selo “Esta sessão”. Ações: *Encerrar* por linha (diálogo de confirmação); *Encerrar todas as outras* e *Encerrar todas (inclusive esta)* (confirmação + reautenticação SCR-11 quando `REAUTH_REQUIRED`).
Estados: carregando (skeleton), vazio (não ocorre — sempre há a atual), erro com tentar novamente, sucesso com anúncio `aria-live` (“Sessão encerrada”), revogar a atual ⇒ sai e vai ao login.
Acessibilidade: tabela com cabeçalhos, botões com rótulos contextuais (“Encerrar sessão em Chrome, Windows”), foco devolvido à lista.
Rastreabilidade: SCR-14 → CA-FT06-01, 03, 04, 06, 07.
