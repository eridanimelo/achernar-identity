# Business Spec — HU-009 Associar múltiplos perfis a usuários
## 0. Identificação
ID: HU-009; origem: NEW; módulo: Usuários e Perfis; status APPROVED por decisão delegada em 24/10/2026.
## 1. Objetivo
Associar múltiplos perfis a usuários.
## 2. História / Necessidade
Como administrador, quero associar um ou mais perfis para definir os acessos da conta.
## 3. Fluxo funcional
Executar a ação autorizada, validar entradas e estado, persistir efeitos quando aplicável e apresentar resultado sem expor credenciais.
## 4. Critérios de aceite
HU-009-CA001: Lista de 1–20 códigos existentes e ativos, sem duplicação; alteração revoga sessões; conta sem perfil ativo não autentica; proteger autoatribuição e remoção do último administrador.
HU-009-CA002: entradas inválidas e acesso não autorizado são rejeitados, com erro compreensível.
## 5. Regras de negócio
HU-009-RN001 — Lista de 1–20 códigos existentes e ativos, sem duplicação; alteração revoga sessões; conta sem perfil ativo não autentica; proteger autoatribuição e remoção do último administrador. Origem: pedido humano de usuários e perfis completos e delegação prévia; CONFIRMED.
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
Lista de 1–20 códigos existentes e ativos, sem duplicação; alteração revoga sessões; conta sem perfil ativo não autentica; proteger autoatribuição e remoção do último administrador.
