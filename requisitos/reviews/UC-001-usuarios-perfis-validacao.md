# Validação — Usuários e Perfis completos
Data: 24/10/2026. Resultado: PASS para o escopo local ampliado. Substitui a limitação funcional a dois papéis fixos da primeira entrega; relatório anterior permanece como histórico.

## Escopo
Dez HUs em specs, mapa em UC-001-identidade/historias.md. CRUD administrativo completo de usuários por convite, cadastro público, confirmação, recuperação, conta própria, bloqueio, exclusão lógica; CRUD de perfis e associação múltipla. JWT contrato 2 com roles, chaves públicas, emissão explícita e consulta online de sessão.

## Evidências
- Backend: `mvn verify -Pintegration` com banco identity_test e SMTP Mailpit, 5 testes unitários e 9 integrados; todos passaram, sem skips.
- Frontend: `npm test`, 9 testes em quatro serviços; todos passaram.
- `npm run lint`: aprovado.
- Docker: backend e frontend compilados; frontend produzido com configuração production. Quatro containers ativos, PostgreSQL e Mailpit saudáveis.
- Playwright: três cenários aprovados em Docker: regressão cadastro/confirmação/recuperação/administração, validação móvel e CRUD de perfis/usuários com convite, múltiplos perfis, desativação/ativação e exclusão.
- Verificação visual: capturas de 390 px e desktop inspecionadas. Corrigido carregamento de CSS crítico incompatível com a CSP; testes agora verificam estilo efetivo dos campos e rejeitam violações de CSP no fluxo.
- Migração real 002: comparados registros antes/depois no banco local. Identificadores, hashes de senha, estados e confirmação preservados; papéis convertidos para vínculos e versões de sessão incrementadas. Schema inicial não foi alterado.
- `git diff --check`: aprovado. Nenhum push executado.

## Cobertura da ampliação
ProfilesIT valida criar, buscar/paginar/filtrar, detalhar, editar, ativar/desativar e excluir perfil; código duplicado/inválido; proteção de ADMIN/USER; bloqueio de exclusão de perfil vinculado. Vários perfis aparecem ordenados em roles; associações inexistentes, vazias e inativas são rejeitadas. Mudanças de vínculos e desativação invalidam sessão. Conta com apenas perfil customizado perde possibilidade de login quando ele é desativado e recupera acesso após ativação.

Convite administrativo ativa conta com senha escolhida pelo titular; prova reutilizada é rejeitada. Detalhe e edição de usuário funcionam; e-mail novo exige confirmação e revoga acesso anterior. E-mail duplicado retorna conflito. Navegador valida criação/edição de perfil, convite com múltiplos perfis, ativação, edição de usuário e operações de exclusão.

Consumidor HTTP independente usa JWKS publicado, valida assinatura, issuer e audience e converte roles em authorities. FINANCEIRO autoriza endpoint protegido; audiência incorreta e assinatura adulterada são negadas. Após revogação, validação local continua criptograficamente válida, enquanto consulta online retorna 401. Bearer permite alteração protegida sem CSRF de cookie; Bearer inválido junto a cookie válido não faz fallback e retorna 401.

## Revisão de implementação
Slice profiles com DTO/repository/service/controller; consultas SQL explícitas, parametrizadas e paginadas. Associação por IDs, sem relacionamentos de objetos ou Lombok. Leituras independentes com NOT_SUPPORTED; alterações administrativas serializadas pelo lock comum e transações de serviço. Código de perfil imutável e sistema protegido. Autoridade ADMIN deriva de perfil ativo. Contagem de administradores exige conta ativa e confirmada. Criação pública não recebe perfil privilegiado. Contrato legado role mantido como derivado para compatibilidade, sem ser fonte da coleção de perfis. Frontend preserva standalone, lazy loading, modelos/serviços por feature, smart/presentational e arquivos HTML/SCSS/TS separados. Opções de associação percorrem todas as páginas sem corte silencioso.

## Limites
Google é futuro. Ambiente publicado ainda requer SMTP real, HTTPS, segredos e configuração de issuer/audiences. Não foi publicado outro microserviço de negócio: foi entregue contrato, guia e teste de consumidor independente. Validação somente local do JWT não oferece revogação imediata; consulta online é necessária para essa garantia. Catálogo gerencia perfis; regras de autorização de cada serviço de negócio pertencem ao consumidor. Rotação automática de chaves e CRUD de permissões granulares não fazem parte desta entrega.

## Gates
CTX PASS; ANL PASS (scan especializado MIXED); AUT PASS (decisões e execução delegadas pelo humano e reafirmadas); QLT PASS; CLS PASS. Sem bloqueantes para o escopo solicitado.
