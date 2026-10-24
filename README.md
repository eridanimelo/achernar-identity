# Achernar Identity
Serviço de usuários e perfis com Java 25, Spring Boot 4.1, Spring Security, JWT RS256, JDBC, PostgreSQL 18, Liquibase e Angular 22.

## Iniciar localmente
1. Copie `docker/.env.example` para `docker/.env`.
2. Preencha `DATABASE_PASSWORD`, `ADMIN_EMAIL` e `ADMIN_PASSWORD` (senha com 12 a 128 caracteres).
3. Execute `docker compose --env-file docker/.env -f docker/docker-compose.yml up --build -d` na raiz do projeto.
4. Abra http://localhost:4200. As mensagens de confirmação/recuperação aparecem no Mailpit em http://localhost:8025.

O administrador inicial é criado somente se o e-mail ainda não existir. Remova as credenciais de bootstrap do ambiente após provisionar o administrador. Não existe senha padrão.

## Desenvolvimento
Com PostgreSQL e Mailpit disponíveis, configure `DATABASE_PASSWORD`, `COOKIE_SECURE=false`, `JWT_GENERATE_KEY=true` e execute `mvn spring-boot:run` em `backend/`. Em `frontend/`, execute `npm ci` e `npm start`. O proxy encaminha `/api` para a porta 8080.

`mvn test` compila e roda testes unitários. Para testes integrados use um banco exclusivo e vazio: `TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/identity_test DATABASE_PASSWORD=... mvn verify -Pintegration`. Os testes integrados apagam os dados desse banco.

Frontend: `npm test -- --watch=false`, `npm run lint`, `npm run build -- --configuration production`. Veja `requisitos/specs/UC-001-identidade/` para contratos, decisões e evidências.

## Comportamento
Cadastro público recebe perfil USER. E-mail confirmado obrigatório antes de entrar. Links de confirmação duram 24 horas; recuperação dura 30 minutos. Tokens de ação são de uso único. JWT dura 15 minutos em cookie HttpOnly; sessão é restaurada ao recarregar. Logout, troca de senha, bloqueio e mudança de perfil revogam acessos anteriores. ADMIN inclui usuários por convite, consulta detalhes, edita nome/e-mail, associa múltiplos perfis, lista/busca, bloqueia/desbloqueia e exclui logicamente. Alterar e-mail exige nova confirmação. ADMIN também cadastra, consulta, edita, ativa/desativa e exclui perfis. ADMIN e USER são perfis de sistema protegidos. Perfil vinculado não pode ser excluído; desativação revoga sessões dos vinculados. Não pode alterar seu próprio acesso ou remover o último administrador ativo. Exclusão anonimiza a conta; registros de auditoria conservam identificadores.

Backend usa slices `auth`, `account`, `admin`, `profiles` e infraestrutura coesa de `users`, `mail`, `security`, `shared`. Consultas SQL explícitas, parâmetros vinculados, projeções e paginação; leitura sem necessidade de transação usa NOT_SUPPORTED. Sem Lombok ou relacionamentos bidirecionais. Frontend usa features lazy, signals, serviços por feature e componentes smart/presentational com HTML/SCSS/TS separados.

## Publicação
O compose é para uso local e contém Mailpit. Em ambiente publicado, configure SMTP real com autenticação/TLS, HTTPS, `COOKIE_SECURE=true`, `ACTION_URL` e `JWT_ISSUER` públicos e chave RSA previamente provisionada (`JWT_GENERATE_KEY=false`). Restrinja banco e arquivos de assinatura, faça backup e configure retenção da auditoria. A limitação de tentativas atual é por processo/IP; para réplicas, configure limite compartilhado no gateway. Não confie em cabeçalhos de proxy sem configurar proxies confiáveis. A fila de e-mail tenta novamente com intervalo crescente; monitore mensagens pendentes. Entrega de e-mail pode se repetir após falha entre SMTP e confirmação, mas links só funcionam uma vez. Google fica para evolução futura.

## Histórias e integração
O módulo possui dez HUs em `requisitos/specs/`, com mapa em `requisitos/specs/UC-001-identidade/historias.md`. A UC-001 consolida os contratos compartilhados do módulo.

Acesse **Usuários** para gerenciar contas e **Perfis** para manter o catálogo. Um usuário pode ter vários perfis; o token carrega os códigos ativos em `roles`. Para integrar outro microserviço, veja [consumir JWT](backend/docs/consumir-jwt.md). Em `docker/.env`, configure `JWT_ADDITIONAL_AUDIENCES` para os destinatários adicionais. A migração 002 converte o antigo papel único em associações e revoga sessões anteriores, preservando contas e credenciais.
