# Frontend Spec — FT-11 Administração de contas
`NEW` · `APPROVED` (documental) · SCR-18 Contas, SCR-19 Detalhe, SCR-20 Diálogos de ação.

- **SCR-18 (`/admin/accounts`)**: filtros (texto, estado, perfil, método, período), tabela paginada ordenável (e-mail, estado em chip, perfil, métodos, criada em), estado vazio e erro. Linha navega ao detalhe.
- **SCR-19 (`/admin/accounts/:id`)**: dados da conta (sem segredos), vínculos, sessões ativas (contagem), linha do tempo (20 eventos) com link para SCR-21 filtrado; ações condicionais ao estado: *Desativar*, *Reativar*, *Conceder/Revogar administrador*, *Anonimizar*.
- **SCR-20 diálogos**: desativar (motivo opcional ≤ 200; texto explicando efeitos), reativar, perfil (explica consequências), anonimizar (aviso de irreversibilidade; digitar o e-mail atual; botão só habilita ao coincidir). Erros `LAST_ADMIN_PROTECTED` (“Há apenas um administrador ativo”), `REAUTH_REQUIRED` (abre SCR-11), `CONFLICT` (recarregar). Ações do próprio administrador logado ficam desabilitadas com explicação.
Acessibilidade: tabela semântica, diálogos modais com foco preso, confirmações em `aria-live`.
Rastreabilidade: SCR-18 → CA-FT11-01, 02 · SCR-19 → CA-FT11-03 · SCR-20 → CA-FT11-04, 06…08, 11, 12.
