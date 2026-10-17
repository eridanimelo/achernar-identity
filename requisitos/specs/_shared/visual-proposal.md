# Achernar Identity — Identidade visual

## Direção aprovada pelo usuário — 17/10/2026

O usuário forneceu a marca Achernar Observer e pediu substituir OBSERVER por IDENTITY, aplicar as cores da logo e modernizar a aplicação com ícones e animações. Na orientação seguinte, dispensou explicitamente suas Skills de UI/UX para esta alteração. Essa decisão humana substitui a marca provisória e a proposta visual anterior.

## Marca e paleta

Asset em uso: `frontend/public/brand/logo.png` (PNG com alpha, 1983×793), escolhido explicitamente pelo usuário. Preservada a transparência; o tema escuro não aplica placa de fundo e o tema claro mantém superfície marinha sob a marca para contraste do lettering branco. O arquivo `achernar-identity-logo.png` foi substituído nas referências e preservado em disco. O arquivo-fonte fornecido pelo usuário foi preservado. A edição anterior foi feita pela ferramenta integrada imagegen; a seleção atual usa diretamente o logo.png indicado pelo usuário, sem edição adicional.

Prompt final: “Precise text edit of the supplied image. Keep the original A symbol, orbit, star, blue/cyan/violet colors and ACHERNAR white lettering unchanged. Replace only OBSERVER with IDENTITY, thin uppercase widely spaced lettering, matching the existing violet-to-cyan text gradient and position. Text exactly ACHERNAR / IDENTITY. Clean perfectly solid crisp smooth letters: no distressed texture, no damage, no glitch, no noise, no holes. Uniform solid deep navy background #080b16. Crop the composition into a wide horizontal application logo with only a small margin around the complete emblem and text (about 4:1 content ratio). Preserve the supplied elegant logo style, do not redesign or add anything.”

Tokens em `frontend/src/styles/_tokens.scss`; integração Material em `_theme.scss`. Ações e links: azul (claro #2454D8, escuro #88B8FF); realces: violeta (#713CBB / #AC91FF); detalhes: ciano (#087995 / #64D6F5). Fundo claro #F4F6FC, fundo escuro #080B16; superfícies próprias para cada tema. Texto e estados usam cores semânticas, não o gradiente. Contraste calculado em `state/brand-contrast.md`, resolvido pelo workspace configurado.

## Layout e interação

- Desktop: marca na sidebar, cabeçalho com identificação do workspace, navegação com ícones e estado ativo tonal; conteúdo com hierarquia de títulos, filtros e tabelas em superfícies próprias.
- Conta: perfil destacado, estado com texto e ícone, dados em grade; atalhos de Segurança e Sessões com ícones e descrições.
- Login: painel de marca orbital e cartão de formulário com ação principal ampla.
- Celular: menu recolhível, cabeçalho compacto, dados e atalhos empilhados. Não muda dados, rotas, contratos nem permissões.
- Movimento: entrada por opacidade/deslocamento, transições de navegação e botões, órbita decorativa lenta no painel de acesso; tudo desligado com prefers-reduced-motion.

## Tipografia e componentes

Inter no corpo, Manrope nos títulos, JetBrains Mono em código; fontes e Material Icons auto-hospedados. Componentes, serviços e rotas existentes reutilizados. Ajustes apenas de apresentação; nenhum design system ou componente funcional duplicado.
