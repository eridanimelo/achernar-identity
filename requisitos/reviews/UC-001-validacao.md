# Validação — UC-001 Identidade
Data: 24/10/2026. Origem NEW. Resultado: PASS para entrega local.

## Escopo entregue
Cadastro público, perfis USER/ADMIN, confirmação e reenvio de e-mail, login por e-mail/senha, recuperação e redefinição, conta própria, alteração de nome, saída e administração com busca/paginação, bloqueio/desbloqueio, atribuição de perfil e exclusão lógica com anonimização. Google é evolução futura, conforme pedido.

## Evidências executadas
- `mvn test`: 5 testes unitários, sem falhas ou skips.
- `TEST_DATABASE_URL=... mvn verify -Pintegration`: 5 testes integrados com PostgreSQL 18 e SMTP Mailpit, sem falhas ou skips; também executa novamente os 5 unitários.
- `npm test`: 6 testes em 3 serviços, todos aprovados.
- `npm run lint`: aprovado.
- `npm run build -- --configuration production`: aprovado; bundle inicial aproximadamente 294 kB.
- `npm run e2e`: 2 cenários Playwright aprovados no servidor de desenvolvimento e, após troca de ambiente, na versão Docker de produção com Nginx.
- Instalação final: zero vulnerabilidades reportadas; Angular Build 22.2.4 com overrides corrigidos para Vite 8.3.5 e Piscina 5.3.2.
- `docker compose ... build`: backend e frontend construídos. Stack completo iniciado; PostgreSQL e Mailpit saudáveis, backend e frontend ativos.
- Capturas desktop (1440 px) e móvel (390 px) inspecionadas visualmente; sem transbordamento horizontal no cenário móvel.
- `git diff --check`: sem erro. Segredos .env, chave local, node_modules, target e capturas de teste são ignorados.

## Comportamentos verificados
CT001/CT002: cadastro, limites, elevação indevida por campo role, login antes da confirmação e senha inválida.
CT003/CT010: links expirados, reutilizados, de finalidade incorreta e consumo concorrente (uma requisição aceita e uma rejeitada); e-mail duplicado sem duplicação da conta.
CT004: troca de senha, saída, bloqueio e alteração de perfil invalidam sessão anterior.
CT005/CT006: acesso administrativo negado a USER, proteção do próprio administrador, paginação limitada, busca e exclusão lógica.
CT007: entrega real via SMTP local, conteúdo de e-mail usado pelo navegador, fila esvaziada e conteúdo removido após entrega.
CT008: serviços frontend, formulários, estados de sucesso/erro, restauração por cookie, navegação, cancelamento e confirmação da exclusão, layout móvel e ausência de erros JavaScript no fluxo.
CT009: alteração sem CSRF negada; sessão adulterada negada; excesso de requisições retorna 429.

## Revisão técnica
SQL explícito e parametrizado no repository, projeções administrativas sem hash, ordenação estável, paginação limitada, transações de escrita na camada de serviço e NOT_SUPPORTED para consultas independentes. Sem ORM, relacionamentos bidirecionais ou Lombok. DTOs não retornam segredo. JWT RS256 assinado por chave persistida; validação de assinatura, emissor, audiência, expiração, estado e versão da conta. Cookies de sessão HttpOnly/SameSite; CSRF obrigatório. Links aleatórios de uso único armazenados como hash; e-mail usa fila transacional. Administração serializada e validada novamente no serviço. Frontend por feature com lazy loading, models e serviços, componentes smart/presentational, signals e arquivos separados. Diálogo administrativo com contenção de foco e Escape.

## Limites operacionais conhecidos
Execução local validada; não houve publicação. SMTP externo, HTTPS, domínio público e segredos de produção dependem do ambiente de implantação. Compose usa Mailpit e cookies sem Secure por ser HTTP local; configuração padrão do backend exige Secure e chave provisionada. Limites de tentativa são por processo/origem; usar gateway compartilhado ao escalar réplicas. Auditoria requer política de retenção no ambiente publicado. Sessão expira em 15 minutos e exige novo login; não há refresh token nesta entrega. Perfis são fixos USER/ADMIN. Exclusão é lógica, com anonimização da conta; referências de auditoria são preservadas. Provedor Google não foi implementado nesta versão.

## Gates
GATE-CTX PASS; GATE-ANL PASS (scan delegado: CREATE em ambas as camadas, fontes vazias); GATE-AUT PASS (autorização e decisões delegadas pelo humano); GATE-QLT PASS; GATE-CLS PASS. Pacote corporate/ui-ux ausente foi suprido pela skill ui-ux-design sem alterar corporate. Nenhum push executado.
