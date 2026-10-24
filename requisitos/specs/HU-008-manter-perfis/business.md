# Business Spec — HU-008 Manter perfis
## 0. Identificação
ID: HU-008; origem: NEW; módulo: Usuários e Perfis; status APPROVED por decisão delegada em 24/10/2026.
## 1. Objetivo
Manter perfis.
## 2. História / Necessidade
Como administrador, quero cadastrar, listar, detalhar, editar, ativar/desativar e excluir perfis.
## 3. Fluxo funcional
Executar a ação autorizada, validar entradas e estado, persistir efeitos quando aplicável e apresentar resultado sem expor credenciais.
## 4. Critérios de aceite
HU-008-CA001: Código único imutável de 2–50 caracteres A-Z/0-9/_; nome 2–100; descrição até 500; perfis de sistema ADMIN/USER não podem ser renomeados, desativados ou excluídos; exclusão de perfil vinculado retorna conflito; desativação revoga sessões dos vinculados.
HU-008-CA002: entradas inválidas e acesso não autorizado são rejeitados, com erro compreensível.
## 5. Regras de negócio
HU-008-RN001 — Código único imutável de 2–50 caracteres A-Z/0-9/_; nome 2–100; descrição até 500; perfis de sistema ADMIN/USER não podem ser renomeados, desativados ou excluídos; exclusão de perfil vinculado retorna conflito; desativação revoga sessões dos vinculados. Origem: pedido humano de usuários e perfis completos e delegação prévia; CONFIRMED.
## 6. Dados funcionais
Conforme o domínio desta história e contratos compartilhados de UC-001-identidade; não retornar senha ou prova de ação.
## 7. Ações funcionais
Conforme história e critérios acima.
## 8. Perfis e permissões
Ator definido na história; operações administrativas exigem ADMIN no servidor.
## 9. Rastreabilidade
Solicitação humana de ampliação nesta conversa; contrato consolidado em UC-001-identidade.
## 10. Pontos a validar
Nenhum bloqueante; autenticação Google permanece futura.
## 11. Divergências
Substitui a limitação anterior a dois perfis fixos.
## 12. LEGACY
NOT_APPLICABLE — evolução de escopo NEW sem fonte legada obrigatória.
## 13. Resultado esperado
Código único imutável de 2–50 caracteres A-Z/0-9/_; nome 2–100; descrição até 500; perfis de sistema ADMIN/USER não podem ser renomeados, desativados ou excluídos; exclusão de perfil vinculado retorna conflito; desativação revoga sessões dos vinculados.
