# Test Spec — UC-001 Identidade
NEW; APPROVED. Especificação de cenários, não relatório de execução.
## Cenários
CT001: cadastrar válido, rejeitar formato inválido e não permitir elevação de perfil; CA001/RN001/RN002.
CT002: negar login antes de confirmação, com senha errada e conta bloqueada; permitir após confirmação; CA002.
CT003: confirmar e recuperar com prova válida; negar reutilização, expiração e finalidade incorreta; CA003/RN004.
CT004: redefinição, saída, bloqueio, exclusão e mudança de perfil revogam sessão anterior; CA004.
CT005: negar administração por USER e visitante; proteger próprio administrador e último ativo; CA005.
CT006: busca literal, paginação limitada e ordem estável; excluídos ausentes; CA006.
CT007: enviar mensagens pela fila, repetir falhas, não retornar segredos; CA001/CA003.
CT008: formulário valida campos, apresenta erro/sucesso/carregamento; navegação protegida e sessão restaurada; CA007.
CT009: negar alteração sem prova de origem, credencial adulterada e tentativa excessiva; security.md.
CT010: persistência aplica integridade, e-mail único e consumo atômico; database.md.
## UI/UX
Verificar fluxo completo em navegador, tamanhos móvel e desktop, rótulos, foco e estados vazios.
## Pontos a validar
Nenhuma decisão funcional pendente.

## Ampliação
CT011 CRUD de perfil: criar/listar/buscar/detalhar/editar/desativar/ativar/excluir; código duplicado e dados inválidos rejeitados; sistema protegido; perfil vinculado não excluído.
CT012 Vários perfis: atribuir códigos ativos, rejeitar inexistentes/inativos/vazios, retornar perfis em token; revogar sessão após atribuição/desativação; restaurar acesso com novo login.
CT013 Usuário administrativo: convite enviado, uso único e senha definida; detalhe/edição de nome/e-mail; novo e-mail exige confirmação; duplicação rejeitada.
CT014 Consumidor independente: validar assinatura via chave pública publicada, emissor, destinatário e expiração; converter roles em autoridades; rejeitar token adulterado; demonstrar que revogação online retorna 401 enquanto assinatura local permanece válida.
CT015 Interface de perfis e usuários: criar/editar/atribuir/listar/buscar/alternar estado/excluir com confirmação; formulário de convite e ativação; regressão dos fluxos anteriores; móvel.
CT016 Migração: manter contas existentes e transformar papel antigo em associação; revogar contrato antigo sem apagar dados.
