# Frontend Spec — FT-02 Cadastro e verificação de e-mail

## 0. Identificação
FT-02 · `NEW` · `APPROVED` (documental) · Telas SCR-02, SCR-03, SCR-04, SCR-05.

## 1. SCR-02 Cadastro (`/register`)
Campos: *E-mail* (obrigatório, formato, ≤ 254, trim, `autocomplete="username"`); *Senha* e *Confirmar senha* (política com texto de apoio, medidor, mostrar/ocultar, `autocomplete="new-password"`); *Idioma* implícito do seletor global. Botões sociais (SCR-15) conforme configuração pública. Link “Já tenho conta”.
Estados: carregando configuração; cadastro desabilitado (mensagem e link ao login); enviando (botão ocupado); erro por campo; `RATE_LIMITED` com contagem regressiva; sucesso ⇒ navega para SCR-05.
Acessibilidade: erros associados a campos, resumo com foco, medidor com texto e `aria-live`.

## 2. SCR-05 Verifique seu e-mail (`/register/check-email`)
Informativa: “Se o endereço puder ser cadastrado, enviamos um link” (neutra); orientação para verificar spam; botão *Reenviar* (SCR-04) com espera de 60 s.

## 3. SCR-03 Verificar e-mail (`/verify-email#token=`)
Lê o token do fragmento, remove-o da barra (`history.replaceState`), chama a verificação automaticamente. Estados: carregando; sucesso (“E-mail confirmado”, botão *Entrar*); erro `TOKEN_INVALID_OR_EXPIRED` com botão *Reenviar verificação*; falha de rede com *Tentar novamente*.

## 4. SCR-04 Reenviar verificação (`/resend-verification`)
Campo e-mail; resposta sempre neutra; `RATE_LIMITED` por IP com aviso.

Mensagens: chaves `register.*`, `verify.*`, `error.REGISTRATION_DISABLED`, `error.PASSWORD_POLICY_VIOLATION.*`. i18n pt-BR/en (RNF-09).

## 5. Rastreabilidade
SCR-02 → CA-FT02-01, 02, 05, 06, 07 · SCR-03 → CA-FT02-08, 09 · SCR-04 → CA-FT02-11 · SCR-05 → CA-FT02-01, 11.
