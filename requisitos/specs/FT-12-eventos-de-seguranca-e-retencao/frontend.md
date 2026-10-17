# Frontend Spec — FT-12 Eventos de segurança e retenção
`NEW` · `APPROVED` (documental) · SCR-21 Eventos (`/admin/events`).

Filtros: período (seletor de data/hora, máx. 90 dias com validação), tipo (multisseleção agrupada), conta (UUID ou e-mail), aplicação, resultado, severidade; botão *Aplicar* e *Limpar*. Tabela: horário (local, com UTC no título), tipo, severidade (chip com texto), resultado, conta, aplicação, IP; linha expansível com `details`, sessão, agente e `request_id` (copiável). Paginação por *Carregar mais* (cursor). Realce de `ALERT`. Estados: carregando, vazio (“Nenhum evento no período”), erro com tentar novamente, `VALIDATION_ERROR` por filtro.
Acessibilidade: tabela semântica, expansão por teclado (`aria-expanded`), chips não dependem só de cor.
Rastreabilidade: SCR-21 → CA-FT12-08…11.
