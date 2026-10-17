# Frontend Spec — FT-01 Setup do primeiro administrador

## 0. Identificação
- ID: FT-01 · Origem: `NEW` · Status: `APPROVED` (documental) · Telas: SCR-01

## 1. Objetivo de interface
Guiar o operador na criação do primeiro administrador, de forma clara e segura, sem expor segredos.

## 2. Tela SCR-01 — Setup do primeiro administrador
**Rota:** `/setup` (acesso condicionado ao estado do setup) · **Layout:** autenticação (cartão central, painel de marca) · **Idiomas:** pt-BR/en.

| Elemento | Comportamento |
|---|---|
| Título e texto de apoio | “Configure o primeiro administrador” + explicação de que o segredo de implantação foi definido no ambiente |
| Campo *Segredo de implantação* | obrigatório; campo mascarado com botão mostrar/ocultar; não reaproveitado em autopreenchimento (`autocomplete="off"`); valor nunca persistido |
| Campo *E-mail* | obrigatório; formato; ≤ 254; `autocomplete="username"` |
| Campo *Senha* e *Confirmar senha* | política exibida em texto (mín. 12, até 128; frase longa recomendada); medidor; coincidência; `autocomplete="new-password"` |
| Botão *Criar administrador* | desabilitado enquanto inválido/enviando; spinner e `aria-busy` |

**Estados**
- Carregando: consulta de estado ao abrir; barra de progresso.
- Setup não necessário/indisponível: se `setup_required=false` ⇒ redireciona ao login; se `setup_required=true` e `setup_available=false` ⇒ página explicativa (“Defina o segredo de implantação no ambiente”) sem formulário.
- Sucesso: mensagem “Administrador criado” e botão *Ir para o login*; formulário limpo.
- Erro: segredo inválido (mensagem genérica, foco no campo segredo); campos inválidos (mensagem por campo); conflito de e-mail; limite de tentativas (aviso com contagem regressiva do `Retry-After`).
- Offline/indisponível: banner e botão *Tentar novamente*.

**Navegação:** entrada automática de qualquer rota pública quando `setup_required=true`; saída para `/login` após sucesso.
**Acessibilidade:** `h1` único; erros ligados aos campos por `aria-describedby`; resumo de erros com foco; contagem regressiva anunciada com `aria-live="polite"`; tudo operável por teclado.
**Validações de cliente:** ver `ui-map.md §4`; o servidor é a autoridade.
**Mensagens (chaves):** `setup.title`, `setup.token.label`, `setup.error.token`, `setup.success`, `error.SETUP_TOKEN_INVALID`, `error.RATE_LIMITED`, `error.CONFLICT`.

## 3. Rastreabilidade
| Tela | Critérios |
|---|---|
| SCR-01 | CA-FT01-01, 02, 03, 04, 05, 06, 07, 09 |

## 4. Pontos a validar
Nenhum.
