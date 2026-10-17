# Business Spec — FT-14 Interface base, tema e sessão no navegador

## 0. Identificação
FT-14 (UC-100) · `NEW` · Módulo M10 Frontend base · `APPROVED` (documental). Normativo: `ui-map.md`, `visual-proposal.md`.

## 1. Objetivo
Prover a base comum da interface do Identity: estrutura da página (shell), navegação protegida por perfil, idioma, tema claro/escuro, estados globais, páginas de erro e o comportamento de sessão no navegador (token em memória, renovação silenciosa, várias abas), com acessibilidade WCAG 2.2 AA e responsividade.

## 2. História
**Como** usuário **quero** uma interface acessível, no meu idioma e tema, que me mantenha autenticado com segurança. **Como** equipe do projeto **quero** reutilizar o comportamento de sessão como referência para minhas próprias aplicações.

## 3. Fluxo (UC-100 — Prover shell, tema, i18n e sessão)
1. Ao abrir qualquer rota, a interface carrega a configuração da aplicação do Identity, aplica idioma e tema (preferência salva, senão a do sistema) e tenta restaurar a sessão por renovação silenciosa.
2. Rotas públicas (login, cadastro, recuperação, verificação) e privadas (conta, sessões, administração) são separadas por guardas; rotas administrativas exigem perfil administrador.
3. Se `setup_required`, a interface leva ao setup (FT-01).
4. Durante o uso, o access token é renovado antes de expirar; abas se coordenam; logout/expiração leva a páginas de estado.
5. Falhas de rede, indisponibilidade e permissões ausentes têm estados e mensagens padronizados.

## 4. Critérios de aceite
- **CA-FT14-01 — Shell.** Todas as telas compartilham cabeçalho, navegação, área principal e rodapé com *skip link* “Ir para o conteúdo”, marcos `banner/nav/main/contentinfo`, um `h1` por tela e foco movido ao `h1` a cada navegação. Ref.: RF-25, RNF-07
- **CA-FT14-02 — Rotas e guardas.** Rotas privadas sem sessão levam ao login preservando o destino; rotas `/admin/**` com perfil ≠ ADMIN levam a `/403`; rota inexistente leva a `/404`. Ref.: RF-25
- **CA-FT14-03 — Setup.** Com `setup_required=true` toda rota pública redireciona a `/setup`; após concluído, `/setup` redireciona ao login. Ref.: RF-15
- **CA-FT14-04 — Access token em memória.** O access token existe apenas em memória; nada de tokens em `localStorage`, `sessionStorage`, IndexedDB, URL ou cookies legíveis por script (verificável por inspeção do armazenamento). Ref.: SEC-12
- **CA-FT14-05 — Restauração de sessão.** Ao recarregar a página, a interface restaura a sessão pela renovação silenciosa (cookie do refresh); sem cookie válido mostra o login sem erro visível. Ref.: RN-AUT-08, D-07
- **CA-FT14-06 — Renovação proativa.** O token é renovado a ~80 % do TTL ou uma única vez ao receber 401; falha de renovação leva a `/session-expired` com ação “Entrar novamente”. Ref.: token-lifecycle §5
- **CA-FT14-07 — Várias abas.** Renovações simultâneas em abas diferentes resultam em uma única chamada de renovação efetiva (Web Locks) e propagam o novo token por canal entre abas; sem suporte a Web Locks aplica atraso aleatório; logout numa aba encerra as demais. Ref.: token-lifecycle §5, RN-TOK-04
- **CA-FT14-08 — Interceptação.** Chamadas a `/api/v1/**` levam `Authorization: Bearer`; `REAUTH_REQUIRED` abre o diálogo de reautenticação e repete a operação; `SESSION_REVOKED` leva a `/session-expired`; `RATE_LIMITED` mostra tempo de espera. Ref.: RN-PWD-08
- **CA-FT14-09 — Idiomas.** pt-BR e en completos (nenhuma chave faltante; erros por `code`); escolha persistida; padrão do navegador/ambiente; troca sem recarregar; `lang` do documento atualizado; datas e números localizados. Ref.: RNF-09, D-37
- **CA-FT14-10 — Tema.** Claro, escuro e sistema; padrão = sistema; escolha persistida e aplicada sem *flash* ao carregar; todos os componentes legíveis nos dois temas. Ref.: RF-26
- **CA-FT14-11 — Persistência segura.** Preferências guardadas em `localStorage` dentro de `try/catch`; falha de armazenamento não quebra a interface (usa o padrão). Ref.: RF-26
- **CA-FT14-12 — Acessibilidade.** WCAG 2.2 AA: contraste ≥ 4.5:1 (texto) e 3:1 (componentes), foco visível, alvos ≥ 24×24 px, navegação 100 % por teclado, rótulos e erros associados, anúncios por regiões vivas, sem armadilhas de foco, respeito a `prefers-reduced-motion`; verificação com `axe` sem violações críticas ou sérias. Ref.: RNF-07
- **CA-FT14-13 — Responsividade.** Layout funcional de 320 a 1920 px sem rolagem horizontal; tabelas se adaptam (cartões em telas estreitas); zoom de 200 % sem perda de funcionalidade. Ref.: RNF-08
- **CA-FT14-14 — Estados globais.** Barra de progresso em requisições > 300 ms; banner offline; banner de serviço indisponível (503) com *Tentar novamente*; formulários mostram estado de envio e impedem duplo envio. Ref.: SCR-31
- **CA-FT14-15 — Páginas de erro.** `/403`, `/404` e `/session-expired` com mensagem, foco e ação adequada; sem detalhes técnicos. Ref.: SCR-23
- **CA-FT14-16 — Segurança da página.** CSP restritiva (`default-src 'self'; script-src 'self'; …`), sem scripts inline, sem recursos de terceiros; fontes locais; `X-Frame-Options`/`frame-ancestors 'none'`. Ref.: SEC-23, RNF-19
- **CA-FT14-17 — Fragmentos sensíveis.** Tokens recebidos no fragmento (verificação, recuperação, troca de e-mail) são lidos e removidos da barra de endereço antes de qualquer outra navegação. Ref.: SEC-09
- **CA-FT14-18 — Identidade visual.** Tokens de design `--ach-*`, Material 3, tipografia e espaçamento conforme `visual-proposal.md`; ícones com texto alternativo. Ref.: D-38
- **CA-FT14-19 — Desempenho.** Carregamento inicial ≤ 200 KB gzip de JS crítico por rota pública (carregamento preguiçoso por área); LCP ≤ 2,5 s em conexão 4G simulada. Ref.: RNF-02 (INFERRED)
- **CA-FT14-20 — Implantação da interface.** A interface é servida como aplicação estática sob `P-61` (`/app`) com *fallback* de rota; a aplicação `achernar-identity-ui` está registrada (FT-09) e funciona em `dev` e `prod`. Ref.: D-46, D-31

## 5. Regras: RN-AUT-08, RN-TOK-04/05, SEC-12, SEC-23, RNF-07/08/09/19, D-31, D-37, D-38.
## 6. Perfis: visitante, usuário, administrador.
## 7. Rastreabilidade: UC-100 → CA-FT14-01…20 · RF-25 → CA-FT14-01, 02, 08 · RF-26 → CA-FT14-10, 11.
## 8. Pontos a validar / divergências / LEGACY: nenhum / nenhuma (Tailwind descartado em favor de Material: DEV-0x em `decisions.md`) / `NOT_APPLICABLE`.
## 9. Resultado esperado: interface consistente, acessível e segura, com sessão previsível em múltiplas abas.
