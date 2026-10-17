# Test Spec — FT-14 Interface base, tema e sessão no navegador
Cenários para execução futura (testes de componente e E2E em navegador). `APPROVED` (documental).

- **CT-FT14-01** Shell: marcos, *skip link*, `h1` único, foco no `h1` após navegação. CA-FT14-01
- **CT-FT14-02** Guardas: privada sem sessão, admin com usuário comum, rota inexistente. CA-FT14-02
- **CT-FT14-03** Setup requerido/encerrado redireciona corretamente. CA-FT14-03
- **CT-FT14-04** Inspeção de armazenamento e URL: nenhum token persistido. CA-FT14-04
- **CT-FT14-05** Recarregar página restaura sessão; sem cookie mostra login. CA-FT14-05
- **CT-FT14-06** Renovação a ~80 % e por 401 único; falha ⇒ `/session-expired`. CA-FT14-06
- **CT-FT14-07** Duas/três abas: 1 renovação efetiva, propagação, logout sincronizado; modo sem Web Locks. CA-FT14-07
- **CT-FT14-08** Interceptores: Bearer, `REAUTH_REQUIRED` com retomada, `SESSION_REVOKED`, `RATE_LIMITED`. CA-FT14-08
- **CT-FT14-09** Catálogos pt-BR/en completos (verificação automática de chaves), troca sem recarregar, `lang`. CA-FT14-09
- **CT-FT14-10** Tema: padrão do sistema, escolha persistida, sem *flash*. CA-FT14-10
- **CT-FT14-11** Armazenamento bloqueado/falhando não quebra a interface. CA-FT14-11
- **CT-FT14-12** Acessibilidade: `axe` em todas as telas nos dois temas, contraste, foco, teclado, alvos, movimento reduzido. CA-FT14-12
- **CT-FT14-13** Responsividade 320/768/1280/1920 px e zoom 200 %. CA-FT14-13
- **CT-FT14-14** Progresso, offline, 503, duplo envio impedido. CA-FT14-14
- **CT-FT14-15** Páginas de erro. CA-FT14-15
- **CT-FT14-16** Cabeçalhos e CSP sem violações no console; sem recursos de terceiros. CA-FT14-16
- **CT-FT14-17** Fragmentos removidos antes de navegar. CA-FT14-17
- **CT-FT14-18** Tokens de design e Material 3 aplicados (testes visuais). CA-FT14-18
- **CT-FT14-19** Orçamento de JS e LCP. CA-FT14-19
- **CT-FT14-20** Entrega estática com *fallback* de rota e cache correto. CA-FT14-20
