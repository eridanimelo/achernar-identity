# Achernar Identity — Mapa de Telas, Navegação, Estados e Validações

Normativo para o frontend do Identity (FT-14 e telas das demais FTs). Decisão D-31, D-37, D-46. Requisitos RF-25, RF-26, RNF-07, RNF-08, RNF-09.
As telas das **aplicações consumidoras** são responsabilidade delas; esta UI é a aplicação registrada `achernar-identity-ui` (BROWSER).

## 1. Mapa de navegação

```text
PÚBLICO (guest.guard)                                        AUTENTICADO (auth.guard)                     ADMIN (admin.guard)
/setup (SCR-01)  [só se setup necessário]                    /account (SCR-09)                            /admin/accounts (SCR-18) ─ /admin/accounts/:id (SCR-19, SCR-20)
/login (SCR-06) ─ social (SCR-15) ─ /auth/callback (SCR-16)  /account/security (SCR-10)                   /admin/applications (SCR-24) ─ /new, /:id (SCR-25, SCR-26)
/register (SCR-02) ─ /register/check-email (SCR-05)             ├ senha (UC-032) · definir senha (SCR-13)   /admin/apis (SCR-27) ─ /new, /:id (SCR-28, SCR-26)
/verify-email (SCR-03) ─ /resend-verification (SCR-04)          ├ e-mail (UC-035)                           /admin/keys (SCR-29)
/forgot-password (SCR-07) ─ /reset-password (SCR-08)            └ vínculos sociais (SCR-17)                 /admin/events (SCR-21)
/confirm-email-change (SCR-12)                               /account/sessions (SCR-14)
                                                             SCR-11 reautenticação (diálogo, sob demanda)
ERROS: /403, /404, /session-expired (SCR-23) · GLOBAL: SCR-22 shell · SCR-30 preferências · SCR-31 estados globais
```

Redirecionamentos: `/` ⇒ `/account` (autenticado) ou `/login`; `auth.guard` sem sessão ⇒ `/login?next=<rota>` (`next` validado: caminho relativo interno); `guest.guard` com sessão ⇒ `/account`;
admin sem perfil ⇒ `/403`; setup concluído ⇒ `/login`; setup necessário ⇒ todas as rotas públicas redirecionam a `/setup`.

Shell (SCR-22): cabeçalho com marca, navegação por papel, menu do usuário (Conta, Segurança, Sessões, Tema, Idioma, Sair); em ≥ 960 px menu lateral permanente (admin) e em < 960 px gaveta (`mat-sidenav`) com botão “Menu”; rodapé com versão.
Páginas de autenticação usam layout dividido (painel de marca + cartão do formulário) em ≥ 840 px e cartão único em telas menores.

## 2. Catálogo de telas

Legenda de estados: **C**arregamento · **V**azio · **S**ucesso · **E**rro · **B**loqueio (rate limit/permisssão). “Msg” = chave i18n.

| ID | Rota | UC | API | Campos e validações (cliente; o servidor revalida) | Estados e comportamento |
|---|---|---|---|---|---|
| SCR-01 Setup | `/setup` | UC-001 | `GET /setup/status`, `POST /setup/admin` | `bootstrap_token` (obrigatório, ≥ 32, campo senha mascarado); `email` (obrigatório, formato, ≤ 254); `password`/`confirmar` (política RN-PWD-01; iguais) | C: consulta status; E: `SETUP_TOKEN_INVALID` (msg genérica), `RATE_LIMITED` com contagem regressiva; S: aviso “Administrador criado” + botão Entrar; se status = concluído ⇒ redireciona |
| SCR-02 Cadastro | `/register` | UC-010 | `GET /auth/applications/{id}/config`, `POST /auth/register` | `email` (obrigatório, formato, ≤ 254, trim); `password` (≥ 12, ≤ 128; medidor; não reprova por composição); `confirmar` | C: botão com progresso; se `allow_registration=false` ⇒ estado “cadastro indisponível”; S ⇒ SCR-05 (sempre, mesmo duplicado); E: `PASSWORD_POLICY_VIOLATION` por campo, `RATE_LIMITED` |
| SCR-03 Verificar e-mail | `/verify-email#token=` | UC-011 | `POST /auth/email/verify` | token do fragmento (removido da barra via `history.replaceState`) | C: “Verificando…”; S: “E-mail confirmado” + Entrar; E: `TOKEN_INVALID_OR_EXPIRED` ⇒ link para SCR-04 |
| SCR-04 Reenviar verificação | `/resend-verification` | UC-012 | `POST /auth/email/resend-verification` | `email` | S: mensagem neutra “Se existir uma conta pendente, enviamos…”; B: contagem do intervalo mínimo (60 s) |
| SCR-05 Verifique seu e-mail | `/register/check-email` | UC-010 | — | — | informativa; ação “Reenviar” (SCR-04); lembrar de checar spam |
| SCR-06 Login | `/login` | UC-020 | `config`, `POST /auth/login`, social | `email`; `password` (obrigatório; sem regra de força); mostrar/ocultar senha | C; E: `INVALID_CREDENTIALS` (msg única), `EMAIL_NOT_VERIFIED` (oferece reenviar), `ACCOUNT_DISABLED`, `RATE_LIMITED` (mensagem + espera); S ⇒ `next` ou `/account`; botões sociais (SCR-15) conforme `config` |
| SCR-07 Esqueci a senha | `/forgot-password` | UC-030 | `POST /auth/password/forgot` | `email` | S: sempre mensagem neutra; B: `RATE_LIMITED` |
| SCR-08 Redefinir senha | `/reset-password#token=` | UC-031 | `POST /auth/password/reset` | `new_password`/`confirmar` (RN-PWD-01) | E: `TOKEN_INVALID_OR_EXPIRED` ⇒ link para SCR-07; S: “Senha redefinida; faça login” |
| SCR-09 Conta | `/account` | UC-034 | `GET /api/v1/account` | — (somente leitura): e-mail, estado, métodos de acesso, vínculos, perfil | C: skeleton; E: retentar |
| SCR-10 Segurança | `/account/security` | UC-032, UC-035, UC-037 | `POST …/password`, `POST …/email-change` | senha atual (obrigatória se tem senha); nova senha (política; ≠ atual); novo e-mail (formato; ≠ atual) | `REAUTH_REQUIRED` ⇒ SCR-11 e reenvia ação; S: snackbar + nota “outras sessões foram encerradas”; troca de e-mail: “Enviamos um link ao novo endereço” |
| SCR-11 Reautenticação (diálogo) | (diálogo) | UC-033, UC-055 | `POST …/reauth` ou intent social | `password` (se houver) ou botão do provedor vinculado | foco no campo; Esc cancela; E: `REAUTH_FAILED`; S: fecha e executa a ação pendente |
| SCR-12 Confirmar troca de e-mail | `/confirm-email-change#token=` | UC-036 | `POST /auth/email-change/confirm` | token do fragmento | S: “E-mail alterado”; E: token inválido |
| SCR-13 Definir senha local | seção de SCR-10 | UC-037 | `POST …/password` (sem `current_password`) | nova senha/confirmar | exibido só para contas sem senha; exige reautenticação social (SCR-11) |
| SCR-14 Minhas sessões | `/account/sessions` | UC-040…042 | `GET/DELETE /api/v1/sessions…` | — | lista: aplicação, criada, último uso, IP aproximado, agente resumido, selo “Esta sessão”; V: “Nenhuma outra sessão”; ações: Encerrar (confirmação), “Encerrar todas as outras”, “Encerrar todas” (reautenticação; desloga) |
| SCR-15 Botões sociais | componente | UC-050 | `config` | — | exibe Google/Facebook conforme `config`; navega para `start` com PKCE/`state` gerados; desabilitado durante navegação |
| SCR-16 Retorno do social | `/auth/callback` | UC-052 | `POST /auth/social/exchange` | query `code`, `state`/`error` | C: “Concluindo login…”; valida `state` salvo em `sessionStorage` efêmero (apenas `state`/`code_verifier` de ≤ 10 min — exceção documentada, sem tokens); E: mapeia `access_denied`, `account_link_required` (explica como vincular), `account_disabled`, `email_verification_required`, `email_unavailable`, `temporarily_unavailable` |
| SCR-17 Vínculos sociais | seção de SCR-10 | UC-053, UC-054 | `…/social-links…` | — | lista por provedor: vinculado/não; Vincular (reauth) / Desvincular (confirmação; bloqueado com explicação quando `LAST_ACCESS_METHOD`) |
| SCR-18 Contas | `/admin/accounts` | UC-080 | `GET /api/v1/admin/accounts` | filtros: texto (prefixo de e-mail), estado, perfil, método, período | tabela paginada (25/pg), ordenação permitida, V: “Nenhuma conta encontrada”, E; linha → SCR-19 |
| SCR-19 Detalhe da conta | `/admin/accounts/:id` | UC-080…084 | `GET …/{id}` + ações | — | dados (sem segredos), vínculos, contagem de sessões, linha do tempo de eventos recentes; ações conforme estado/perfil |
| SCR-20 Diálogos de ação | diálogos | UC-081…084 | `POST/PUT` | motivo (≤ 200, opcional); anonimizar: digitar o e-mail para confirmar | `LAST_ADMIN_PROTECTED` explicado; foco e Esc; S: snackbar |
| SCR-21 Eventos | `/admin/events` | UC-090 | `GET …/security-events` | filtros: período (máx. 90 dias por consulta), tipo (multi), conta (UUID/e-mail exato), aplicação, resultado, severidade | tabela com cursor “Carregar mais”; V; chips de severidade (não só cor); detalhe expansível (`details`) |
| SCR-22 Shell | todas | UC-100 | — | — | skip link “Ir para o conteúdo”, landmarks `banner/nav/main/contentinfo`, foco no `h1` a cada navegação |
| SCR-23 Erros | `/403` `/404` `/session-expired` | UC-100 | — | — | mensagem + ação (voltar/entrar); `session-expired` quando refresh falha |
| SCR-24 Aplicações | `/admin/applications` | UC-061 | `GET …/applications` | busca por nome/`client_id` | tabela, estado (chip), V: “Cadastre a primeira aplicação”; botão Nova |
| SCR-25 Aplicação | `/admin/applications/new|:id` | UC-060, 062, 063 | `POST/GET/PATCH …` | `client_id` (`^[a-z0-9-]{3,64}$`, imutável); nome (≤ 100); tipo; origens (lista; `https://host[:porta]` sem caminho; ≤ 20); redirects (absolutas, https, sem fragmento, origem registrada; ≤ 20); URLs de ação (3); APIs permitidas + padrão; flags; TTL (300–900 s) | edição com `version`; conflito 409 ⇒ recarregar; seção Segredos (CONFIDENTIAL): gerar/revogar com confirmação; desativar exige confirmação (“encerra todas as sessões”) |
| SCR-26 Exibição única de segredo | diálogo | UC-063, 067 | resposta do `POST` | — | mostra segredo uma vez, botão Copiar (anuncia “Copiado”), confirmação “Guardei em local seguro” obrigatória para fechar; não persiste |
| SCR-27 APIs | `/admin/apis` | UC-065 | `GET …/apis` | busca | tabela, V, botão Nova |
| SCR-28 API | `/admin/apis/new|:id` | UC-064, 066, 067 | `POST/GET/PATCH …` | `audience` (≤ 200, imutável, URL ou identificador `[A-Za-z0-9:/._-]`); nome | credencial de introspecção (gerar/revogar → SCR-26); desativar com confirmação |
| SCR-29 Chaves | `/admin/keys` | UC-072 | `GET/POST …/signing-keys…` | rotação manual: confirmação; remoção emergencial: motivo + opção “revogar todas as sessões” + digitar `kid` | tabela de estados (`PUBLISHED/ACTIVE/RETIRED/REMOVED`), datas, próxima rotação; alerta se chave ativa > P-10 |
| SCR-30 Preferências | menu | UC-100 | — | tema: Sistema/Claro/Escuro; idioma: pt-BR/en | persistidos em `localStorage` (try/catch; fallback silencioso) |
| SCR-31 Estados globais | global | UC-100 | — | — | barra de progresso global em requisições > 300 ms; banner offline; banner “serviço indisponível” (503) com retentar |

## 3. Padrões de estado e mensagens

| Estado | Padrão visual/comportamento |
|---|---|
| Carregando | `mat-progress-bar` (global) + *skeleton* em listas/detalhes; botões com spinner e `aria-busy`; nunca bloquear a página inteira |
| Vazio | ícone + título + texto + ação primária (ex.: “Nova aplicação”) |
| Sucesso | `MatSnackBar` (4 s, `role=status`) + estado inline quando a decisão é relevante |
| Erro de campo | texto abaixo do campo ligado por `aria-describedby`; `aria-invalid`; resumo de erros no topo do formulário (foco no primeiro erro após submissão) |
| Erro de operação | `mat-error`/banner `role=alert` com `trace_id` copiável; ação “Tentar novamente” |
| Bloqueio | mensagem + contagem regressiva a partir de `Retry-After`; botão desabilitado com explicação |
| Confirmação destrutiva | diálogo com título, consequência explícita e botão destrutivo distinto (nunca apenas cor) |

Textos nunca revelam existência de conta (D-13). Chaves i18n: `feature.tela.elemento` (`auth.login.title`); erros `error.<CODE>` para cada código do `error-catalog.md`.

## 4. Validação de formulários (cliente espelha servidor)

| Campo | Regra de cliente |
|---|---|
| e-mail | obrigatório; trim; ≤ 254; um `@`; sem espaços; (validação definitiva no servidor) |
| senha nova | 12–128 caracteres; sem regras de composição; exibe medidor e dicas (“use uma frase longa”); lista de comuns/contém e-mail só no servidor, erros mapeados por `errors[].code` |
| confirmar senha | igual à nova |
| token (fragmento) | presente; senão estado de erro com link de ajuda |
| `client_id` | `^[a-z0-9-]{3,64}$` |
| URL/origem | parser `URL`; esquema `https` (ou `http://localhost`/`127.0.0.1` em dev); sem curinga, sem fragmento; origens sem caminho |
| `audience` | ≤ 200 |
| motivo | ≤ 200 |

Autocompletar: `autocomplete="email"`, `"current-password"`, `"new-password"`, `"username"`; `inputmode="email"`; sem desativar colagem em campos de senha; `name`/`id` estáveis para gerenciadores de senha.

## 5. Acessibilidade e teclado (WCAG 2.2 AA)

| Item | Requisito |
|---|---|
| Teclado | todo o fluxo operável por teclado; ordem de tabulação lógica; sem armadilhas; atalhos opcionais não conflitam com leitores de tela |
| Foco | visível (anel 2 px, contraste ≥ 3:1, ver `visual-proposal.md`); gerenciado após navegação (h1), abertura/fechamento de diálogos (volta ao acionador), erros (primeiro campo inválido) |
| Semântica | landmarks; um `h1` por rota; tabelas com `<th scope>`; listas reais; botões ≠ links |
| Leitores de tela | `aria-live` polite para sucesso, assertive para erro; rótulos programáticos; ícones decorativos `aria-hidden`; estado de chips com texto |
| Alvo | ≥ 24×24 px (WCAG 2.5.8), preferir 44 px em toque |
| Movimento | respeita `prefers-reduced-motion`; sem animações essenciais |
| Cor | nunca única forma de informação (ícone + texto); contraste conforme `visual-proposal.md` |
| Tempo | mensagens de bloqueio com contagem; sessões expiram com aviso 60 s antes? **Não** há logout automático ocioso no cliente (renovação silenciosa); expiração real ⇒ SCR-23 com explicação |
| Autenticação acessível | sem CAPTCHA/teste cognitivo; permitir gerenciadores de senha e colar |
| Idiomas | `lang` dinâmico no `<html>` |

## 6. Responsividade

Breakpoints: `xs` < 600, `sm` 600–839, `md` 840–1199, `lg` 1200–1599, `xl` ≥ 1600. Layout fluido de 320 px; sem rolagem horizontal da página (tabelas largas rolam dentro do contêiner ou colapsam em cartões < 600 px);
cabeçalho compacto; formulários em coluna única; diálogos em tela cheia no `xs`.

## 7. Tratamento da sessão no cliente

- Estados de autenticação: `unknown` (inicialização/reidratação) → `authenticated` | `anonymous`.
- 401 de API ⇒ uma tentativa de refresh; falha ⇒ `anonymous` + SCR-23/`/login?next=`.
- `REAUTH_REQUIRED` ⇒ abre SCR-11; ao sucesso, reexecuta a requisição original uma vez.
- Logout propagado entre abas; evento de rede offline não derruba a sessão.
- Sem armazenamento de tokens (SEC-12). Única persistência local: tema, idioma e, **efêmero**, `state`/`code_verifier` do login social em `sessionStorage` (≤ 10 min, removidos no retorno).
