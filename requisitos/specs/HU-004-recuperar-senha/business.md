# Business Spec — HU-004 Recuperar senha
## 0. Identificação
ID: HU-004; origem: NEW; módulo: Usuários e Perfis; status APPROVED por decisão delegada em 24/10/2026.
## 1. Objetivo
Recuperar senha.
## 2. História / Necessidade
Como usuário, quero recuperar acesso por mensagem no e-mail confirmado.
## 3. Fluxo funcional
Executar a ação autorizada, validar entradas e estado, persistir efeitos quando aplicável e apresentar resultado sem expor credenciais.
## 4. Critérios de aceite
HU-004-CA001: Resposta genérica; link de 30 minutos e uso único; nova senha entre 12 e 128 caracteres; revogar sessões.
HU-004-CA002: entradas inválidas e acesso não autorizado são rejeitados, com erro compreensível.
## 5. Regras de negócio
HU-004-RN001 — Resposta genérica; link de 30 minutos e uso único; nova senha entre 12 e 128 caracteres; revogar sessões. Origem: pedido humano de usuários e perfis completos e delegação prévia; CONFIRMED.
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
Resposta genérica; link de 30 minutos e uso único; nova senha entre 12 e 128 caracteres; revogar sessões.
