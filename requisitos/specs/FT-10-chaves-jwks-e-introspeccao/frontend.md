# Frontend Spec — FT-10 Chaves, JWKS e introspecção
`NEW` · `APPROVED` (documental) · SCR-29 Chaves (`/admin/keys`); SCR-26 não se aplica. JWKS/metadados/introspecção não têm tela.

SCR-29: tabela de chaves (`kid` abreviado com *Copiar*, estado em chip, datas), destaque da ativa e da próxima agendada; ações *Rotacionar agora* (diálogo de confirmação com explicação do efeito) e *Remover* (para `PUBLISHED`/`RETIRED`; para a ativa, *Remoção emergencial* com motivo obrigatório, opção “revogar todas as sessões” e digitação do `kid` para confirmar). Reautenticação (SCR-11) quando `REAUTH_REQUIRED`.
Estados: carregando, erro, vazio (não ocorre), sucesso com anúncio; `CONFLICT` explicado.
Acessibilidade: chips com texto (não só cor), diálogos com foco preso e retorno de foco.
Rastreabilidade: SCR-29 → CA-FT10-10…13.
