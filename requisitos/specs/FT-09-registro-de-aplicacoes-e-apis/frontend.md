# Frontend Spec — FT-09 Registro de aplicações e APIs
`NEW` · `APPROVED` (documental) · SCR-24 Aplicações, SCR-25 Aplicação, SCR-26 Exibição única de segredo, SCR-27 APIs, SCR-28 API. Acesso: rotas `/admin/**` com guarda de perfil ADMIN.

- **SCR-24**: tabela paginada (nome, `client_id`, tipo, estado em chip), busca, botão *Nova aplicação*; vazio: “Cadastre a primeira aplicação”.
- **SCR-25**: formulário em seções (Identificação; Origens e retornos — listas editáveis com validação; URLs de ação; Comportamento — cadastro aberto, provedores, acesso à API do Identity, TTL; APIs permitidas — seletor com “padrão”; Segredos — lista com *Gerar novo* e *Revogar*). `client_id` e tipo somente leitura após criar. Conflito de versão ⇒ aviso “alterado por outro administrador” com *Recarregar*. Desativar abre confirmação que informa o efeito (sessões encerradas).
- **SCR-26**: diálogo modal exibindo o segredo uma vez (campo somente leitura, *Copiar* com anúncio “Copiado”), aviso “não será exibido novamente” e confirmação “Já guardei” obrigatória para fechar.
- **SCR-27/28**: análogos para APIs (`audience` somente leitura após criar; credenciais).
Validação cliente espelha CA-FT09-02…06. Estados: carregando, erro por campo (com `errors[]`), sucesso, `FORBIDDEN`. Acessibilidade: listas editáveis operáveis por teclado, rótulos explícitos, diálogos com foco preso.
Rastreabilidade: SCR-24 → CA-FT09-08 · SCR-25 → CA-FT09-01…07, 09…12, 23 · SCR-26 → CA-FT09-12, 13 · SCR-27/28 → CA-FT09-14…16.
