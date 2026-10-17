# Frontend Spec — FT-03 Autenticação por senha e tokens

`NEW` · `APPROVED` (documental) · SCR-06 Login (`/login`), SCR-16 (retorno social, em FT-07), serviço de sessão (FT-14).

## SCR-06 Login
Campos: *E-mail* (obrigatório, formato), *Senha* (obrigatória, sem regras de força, mostrar/ocultar, `autocomplete="current-password"`). Botões sociais (SCR-15) e links *Esqueci a senha* (SCR-07) e *Criar conta* (se permitido).
Estados: carregando configuração; enviando; `INVALID_CREDENTIALS` (mensagem única “E-mail ou senha inválidos”, foco no e-mail); `EMAIL_NOT_VERIFIED` (mensagem + ação *Reenviar verificação*); `ACCOUNT_DISABLED` (mensagem + contato); `RATE_LIMITED` (contagem regressiva); indisponível/offline.
Pós-sucesso: guarda o access token **só em memória**, agenda renovação antes do `exp` e navega à rota solicitada (ou `/account`).
Acessibilidade: erro anunciado (`role="alert"`), foco no primeiro campo inválido.

## Comportamento de sessão (cliente)
Renovação silenciosa antes do `exp` e ao carregar a página (cookie); `Web Locks` evita refresh simultâneo entre abas, `BroadcastChannel` propaga o novo access; resposta `refresh_rotated=false` não substitui o cookie; falha ⇒ `session-expired` (SCR-23). Logout chama a API, limpa memória e sincroniza abas.

Rastreabilidade: SCR-06 → CA-FT03-01, 03, 05, 11 · sessão → CA-FT03-13, 14, 20, 21.
