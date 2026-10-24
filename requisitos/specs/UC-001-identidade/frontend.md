# Frontend Spec — UC-001 Identidade
NEW; APPROVED por delegação.
## Fluxos e telas
Entrada com e-mail e senha e links para cadastro, recuperação e reenvio de confirmação. Cadastro com nome, e-mail e senha e orientação de tamanho. Confirmação e redefinição leem credencial do fragmento do link e exigem ação explícita. Página da conta exibe nome, e-mail e perfil e permite alterar nome e sair. Administração lista nome, e-mail, perfil e estado, busca, paginação, alteração de perfil, bloqueio/desbloqueio e exclusão com confirmação explícita.
## Estados
Botão de envio desabilitado durante carregamento; mensagens acessíveis de erro e sucesso; estado vazio e contagem de resultados; sessão expirada volta à entrada. Não exibir administração para USER.
## Acessibilidade e responsividade
Formulários com rótulos, preenchimento automático apropriado, foco visível, áreas de toque de pelo menos 44px, mensagens anunciadas, contraste e layout móvel sem rolagem horizontal. Tema claro/escuro acompanha sistema. Administração em cartões responsivos.
## Rastreabilidade
CA001–CA007. Sem pendências. Pacote corporate/ui-ux ausente no motor: documento definido pela skill ui-ux-design disponível; nenhuma política corporativa foi alterada.

## Ampliação
Navegação administrativa distingue Usuários e Perfis. Usuários: botão Novo usuário, formulário de convite com nome/e-mail/múltiplos perfis, consulta e edição com dados atuais, bloqueio/exclusão existentes; seleção inclui todos os perfis ativos paginados, busca por nome/código, sem limitar silenciosamente aos primeiros resultados. Perfis: lista paginada com busca e filtro de ativo/inativo, criação, detalhe/edição, ativação/desativação e exclusão confirmada. Exibir perfis de sistema e contagem de vínculos; operações proibidas desabilitadas com explicação. Estado e mensagens não dependem apenas de cor. A página de conta apresenta todos os perfis ativos. Ativação de convite define senha em tela própria.

## Identidade visual — logo fornecida pelo humano
Usar a logo original de frontend/src/assets/logo.png no cabeçalho e no painel de acesso, preservando transparência, proporção e conteúdo. Paleta derivada: azul elétrico, ciano e violeta, com superfícies legíveis em tema claro e escuro. Nome branco da marca sempre sobre superfície escura. Destaques e gradientes remetem à órbita do símbolo. Aplicar tokens globais e tema Material para estender a identidade a usuários, perfis, conta e formulários. Layout responsivo, foco visível, contraste e templates/estilos separados preservados.

## Ações com ícones
Usar um único conjunto de ícones vetoriais locais, com traços consistentes, integrado ao design system. Excluir usa lixeira, editar usa lápis, buscar usa lupa, bloquear/desbloquear usa cadeado, criação e navegação usam símbolos correspondentes. Manter rótulos textuais e ícones decorativos ocultos de leitores de tela. A área pública de autenticação exibe somente a marca no cabeçalho, sem link Entrar redundante. Navegação autenticada mantém Conta, Usuários e Perfis com ícones e indicação da página atual. Preservar funcionamento, nomes acessíveis e áreas de toque.
