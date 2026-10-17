# Frontend Spec — FT-14 Interface base, tema e sessão no navegador
`NEW` · `APPROVED` (documental) · SCR-22 Shell, SCR-23 Erros, SCR-30 Preferências, SCR-31 Estados globais.

- **SCR-22 Shell**: *skip link*, cabeçalho (marca, navegação principal, menu do usuário com *Conta*, *Segurança*, *Sessões*, *Preferências*, *Sair*; itens de *Administração* apenas para ADMIN), `main`, rodapé. Navegação responsiva (menu recolhível em telas estreitas, operável por teclado). Layout de autenticação separado (cartão central) para rotas públicas.
- **SCR-23 Erros**: `/403` (sem permissão; ação *Voltar*), `/404` (não encontrado; ação *Ir para o início*), `/session-expired` (ação *Entrar novamente*, preserva destino).
- **SCR-30 Preferências**: tema (Sistema/Claro/Escuro) e idioma (pt-BR/en) com aplicação imediata e persistência tolerante a falhas.
- **SCR-31 Estados globais**: barra de progresso para requisições > 300 ms, banner offline, banner de serviço indisponível com *Tentar novamente*, notificações (`aria-live`) para sucesso/erro.
Comportamento de sessão (serviço): restauração por renovação silenciosa; renovação a 80 % do TTL; coordenação entre abas (Web Locks + canal de difusão); interceptor de requisições e de erros; limpeza de fragmento de URL; redirecionamentos por guarda; sem tokens em armazenamento persistente.
Tokens visuais: `--ach-*` (cores, tipografia, espaçamento, raio, elevação), temas claro/escuro, Material 3, ver `visual-proposal.md`.
Acessibilidade e responsividade conforme CA-FT14-12, 13.
Rastreabilidade: SCR-22 → CA-FT14-01, 02, 12, 13, 18 · SCR-23 → CA-FT14-15 · SCR-30 → CA-FT14-09…11 · SCR-31 → CA-FT14-14.
