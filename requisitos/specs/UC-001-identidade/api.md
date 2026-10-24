# API Spec — UC-001 Identidade
## Identificação
NEW; APPROVED por delegação humana.
## Operações
Todas as operações usam /api. Corpos e respostas são objetos JSON.
| Método e caminho | Entrada | Sucesso | Acesso |
|---|---|---|---|
| GET /csrf | nenhuma | 200, token e headerName | público |
| POST /auth/register | name, email, password | 202, message genérica | público |
| POST /auth/login | email, password | 200, usuário e cookie de sessão | público |
| POST /auth/verify | token | 200, message | público |
| POST /auth/resend | email | 202, message genérica | público |
| POST /auth/forgot-password | email | 202, message genérica | público |
| POST /auth/reset-password | token, password | 200, message | público |
| POST /auth/logout | nenhuma | 204 e remoção da sessão | autenticado |
| GET /me | nenhuma | 200, usuário | autenticado |
| PATCH /me | name | 200, usuário | autenticado |
| GET /admin/users | q, page, size | 200, items, page, size, total, totalPages | ADMIN |
| PATCH /admin/users/{id} | name, email, profiles e/ou status; role legado opcional | 200, usuário | ADMIN |
| DELETE /admin/users/{id} | nenhuma | 204 | ADMIN |
Usuário de saída: id, name, email, role legado, profiles, assignedProfiles, status, verified, createdAt. Campos obrigatórios nas entradas indicadas; PATCH administrativo aceita name, email, profiles e status ACTIVE/BLOCKED; role USER/ADMIN é compatibilidade opcional, não combinável com profiles. Exige ao menos um campo. Identificador válido obrigatório.
## Paginação
page começa em 0; size padrão 20, mínimo 1, máximo 100; q até 100 caracteres; ordem estável por criação decrescente e identificador. Busca literal sem diferenciação de caixa por nome/e-mail. Excluídos omitidos.
## Erros
400 entrada inválida ou link inválido/expirado; 401 credencial ou sessão inválida; 403 acesso negado ou confirmação pendente; 404 recurso inexistente; 409 proteção administrativa; 429 excesso de tentativas; 500 erro interno genérico. Corpo: code, message. Links de ação só são consumidos por POST explícito.
## Integrações e segurança
Entrega de e-mail com repetição automática. Sessão protegida contra leitura por scripts; proteção de requisições que alteram estado. Ver security.md.
## Rastreabilidade
CA001–CA007, RN001–RN010 de business.md. Sem pendências.

## Ampliação — contrato de usuários e perfis
| Método e caminho | Entrada | Saída | Acesso |
|---|---|---|---|
| GET /admin/profiles | q, page, size, active opcional | página de perfis | ADMIN |
| POST /admin/profiles | code, name, description | 201 perfil | ADMIN |
| GET /admin/profiles/{id} | identificador | perfil | ADMIN |
| PATCH /admin/profiles/{id} | name, description, active obrigatórios | perfil | ADMIN |
| DELETE /admin/profiles/{id} | identificador | 204; 409 se sistema/vinculado | ADMIN |
| POST /admin/users | name, email, profiles (códigos) | 201 usuário não confirmado e convite enviado | ADMIN |
| GET /admin/users/{id} | identificador | usuário | ADMIN |
| PATCH /admin/users/{id} | name, email, profiles e/ou status | usuário | ADMIN |
| POST /auth/activate | token, password | mensagem de sucesso | público |
| POST /auth/token | email, password | accessToken, tokenType=Bearer, expiresIn=900 | público com credenciais |
| GET /auth/session | credencial de acesso | sub, profiles, active=true, expiresAt | autenticado |
| GET /.well-known/jwks.json | nenhuma | conjunto de chaves públicas | público; fora do prefixo /api |
Perfil: id, code, name, description, active, system, userCount, createdAt. Código não é editável. Usuário acrescenta profiles (ativos) e assignedProfiles (todos os vínculos), arrays de códigos ordenados. Campo role permanece compatibilidade derivada (ADMIN se possui ADMIN ativo, USER nos demais casos), mas não representa o conjunto de acessos. Entrada role antiga fica disponível por compatibilidade e não deve ser combinada com profiles.
POST /auth/token não usa cookie de autenticação; exige e-mail/senha e prova de origem, igual ao login. Consumidor envia Authorization: Bearer <accessToken>. Cookie permanece para navegador. Token inclui contrato claim_version=2, sub, iss, aud, exp, iat, jti, roles (array de perfis ativos), version (versão de sessão) e role legado. Nunca incluir dados pessoais ou senha. Destinatários adicionais configurados externamente.
Listagens usam os limites existentes. Filtro active em perfis é opcional. Erros 400 para campos/associações inválidos; 404 ausente; 409 código/e-mail duplicado, perfil protegido ou vinculado; demais códigos existentes preservados.
