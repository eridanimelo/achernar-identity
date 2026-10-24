# Database Spec — UC-001 Identidade
Origem NEW; status APPROVED por delegação.
## Objetivo e estruturas lógicas
Conta: identificação, nome, e-mail único normalizado, credencial protegida, conjunto de perfis associados, estado ACTIVE/BLOCKED/DELETED, confirmação, versão de sessão e criação.
Ação de e-mail: identificador, conta de origem, finalidade VERIFY/RESET, prova protegida única, expiração e data de consumo. Conta possui várias ações; ação pertence a uma conta.
Fila de mensagens: destinatário, assunto, conteúdo, tentativas, próxima tentativa e data de envio. Persistida junto à ação para evitar perda entre gravação e envio.
Evento de auditoria: identificação, ator opcional, alvo opcional, ação e data; não contém credenciais.
## Integridade e unicidade
E-mail único inclusive concorrência. Token de ação único, consumo atômico com alteração de conta; contas excluídas não podem autenticar. Administração serializa decisões de último administrador. Datas obrigatórias de criação e validade. Registros de ação referenciam conta existente.
## Histórico e auditoria
Registrar cadastro, confirmação, redefinição, login e operações administrativas. Exclusão anonimiza identidade na conta; histórico conserva somente identificadores e ação.
## Dados transitórios
Credencial de sessão expira em 15 minutos; credencial de ação em claro somente na mensagem destinada ao usuário.
## Rastreabilidade
RN001–RN009; sem pendências. LEGACY: NOT_APPLICABLE.

## Ampliação
Perfil: id, código único imutável, nome, descrição, ativo, indicador de sistema, data de criação. Associação usuário/perfil: par único com referência às duas estruturas; muitos-para-muitos funcional, sem exigência de referências de objeto. Migrar cada papel anterior para associação equivalente, preservar identificadores e credenciais e revogar sessões anteriores à mudança de contrato. Perfil de sistema ADMIN/USER protegido. Exclusão física de perfil somente sem vínculo; banco bloqueia referências pendentes. Desativar perfil revoga versão de sessão de todas as contas vinculadas; código permanece estável. Inclusão/edição administrativa de perfil e atribuição a usuários usam limite consistente para evitar corridas. Ação de e-mail acrescenta finalidade INVITE. Conta continua com exclusão lógica.
