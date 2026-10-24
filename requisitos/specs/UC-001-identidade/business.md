# Business Spec — UC-001 Identidade
## 0. Identificação
- Origem: NEW
- Módulo: Identidade
- Status: APPROVED por delegação humana em 24/10/2026.
## 1. Objetivo
Manter usuários e perfis e permitir acesso seguro por e-mail e senha.
## 2. Necessidade
Como usuário, quero cadastrar minha conta, confirmar meu e-mail e recuperar meu acesso. Como administrador, quero gerenciar contas e seus perfis.
## 3. Fluxo funcional
Cadastro público com nome, e-mail e senha; envio de confirmação; confirmação; login; consulta e alteração do próprio nome; saída. Recuperação solicita e-mail e permite definir nova senha através de link. Administrador lista, filtra, bloqueia, desbloqueia, altera perfil e exclui logicamente contas.
## 4. Critérios de aceite
CA001: cadastro válido gera conta não confirmada e entrega mensagem de confirmação.
CA002: somente conta confirmada e ativa com senha correta pode entrar.
CA003: confirmação e recuperação aceitam somente links válidos, não expirados e de uso único.
CA004: troca de senha, bloqueio, exclusão e mudança de perfil invalidam sessões existentes.
CA005: usuário comum não administra outras contas; administrador não pode bloquear, excluir ou alterar o próprio perfil, nem remover o último administrador ativo.
CA006: lista administrativa possui busca por nome/e-mail e paginação.
CA007: interface apresenta carregamento, erro, sucesso e ausência de resultados.
## 5. Regras de negócio
| ID | Regra | Origem | Situação |
|---|---|---|---|
| RN001 | Cadastro público recebe perfil USER; perfis iniciais de sistema USER e ADMIN; outros perfis cadastráveis por administrador. | Decisão delegada | CONFIRMED |
| RN002 | Nome entre 2 e 100 caracteres, e-mail único normalizado até 254, senha entre 12 e 128 caracteres. | Decisão delegada | CONFIRMED |
| RN003 | E-mail confirmado é obrigatório para login. | Decisão delegada | CONFIRMED |
| RN004 | Confirmação expira em 24 horas; recuperação em 30 minutos; uso único. | Decisão delegada | CONFIRMED |
| RN005 | Sessão expira em 15 minutos; saída revoga todas as sessões da conta. | Decisão delegada | CONFIRMED |
| RN006 | Exclusão lógica anonimiza nome/e-mail, impede acesso e remove links pendentes. | Decisão delegada | CONFIRMED |
| RN007 | Bloqueio impede acesso imediatamente; desbloqueio exige novo login. | Pedido humano e decisão delegada | CONFIRMED |
| RN008 | Somente administrador atribui perfis; proteção do próprio administrador e do último ativo. | Decisão delegada | CONFIRMED |
| RN009 | Cadastro duplicado, recuperação e reenvio retornam resposta genérica sem revelar existência da conta. | Decisão delegada | CONFIRMED |
| RN010 | Login Google pertence a versão futura; não há botão funcional nesta entrega. | Pedido humano | CONFIRMED |
## 6. Dados funcionais
Nome, e-mail, senha, perfil, estado ativo/bloqueado/excluído, confirmação de e-mail, identificação e datas de criação. Senha e credenciais de ação não são retornadas.
## 7. Ações funcionais
Cadastrar, confirmar e-mail, reenviar confirmação, entrar, sair, solicitar recuperação, redefinir senha, consultar/alterar nome próprio, listar usuários, bloquear/desbloquear, atribuir perfil e excluir.
## 8. Perfis e permissões
Visitante: cadastro, confirmação, login e recuperação. USER: conta própria e saída. ADMIN: capacidades de USER e administração de contas e perfis.
## 9. Rastreabilidade
Solicitação humana desta conversa e delegação explícita de decisões e implementação em 24/10/2026.
## 10. Pontos a validar
Nenhum bloqueante. Entrega futura: provedor social Google.
## 11. Divergências
Nenhuma funcional. Decisões técnicas são documentadas fora deste artefato.
## 12. LEGACY
NOT_APPLICABLE.
## 13. Resultado esperado
Aplicação de identidade utilizável com confirmação de e-mail e administração protegida.

## 14. Ampliação autorizada — Usuários e Perfis
A divisão funcional está em historias.md e nas dez HUs relacionadas. Usuário pode ter vários perfis. Perfil tem identificador, código único imutável, nome, descrição e estado ativo/inativo. CRUD de perfis exige ADMIN. Código usa 2–50 caracteres maiúsculos, dígitos e sublinhado, iniciando com letra. Perfis de sistema não podem ser alterados ou removidos. Perfil vinculado não pode ser excluído; pode ser desativado, revogando sessões dos vinculados. Associação aceita 1–20 perfis ativos existentes. Usuário sem perfil ativo não entra.
Administrador inclui usuário por convite de 24 horas, sem conhecer sua senha. Ativação define senha e confirma e-mail. Inclusão administrativa duplicada retorna conflito. Edição administrativa altera nome, e-mail e perfis; alteração de e-mail revoga sessões e requer confirmação no novo endereço. Senha não é editada pelo administrador. Proteção do próprio administrador se aplica a alterações de acesso. Exclusão lógica permanece. HUs de autenticação, e-mail e recuperação são mantidas.
Consumidores recebem credencial com múltiplos perfis e chave pública para validação. Validação local permite uso até expiração; revogação imediata em consumidor exige consulta online da sessão. Emissão para cliente é explícita e protegida por credenciais, sem alterar sessão de navegador.
