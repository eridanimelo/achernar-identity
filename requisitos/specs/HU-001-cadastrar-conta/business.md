# Business Spec — HU-001 Cadastrar conta pública
## 0. Identificação
ID: HU-001; origem: NEW; módulo: Usuários e Perfis; status APPROVED por decisão delegada em 24/10/2026.
## 1. Objetivo
Cadastrar conta pública.
## 2. História / Necessidade
Como visitante, quero criar conta com nome, e-mail e senha para acessar os serviços.
## 3. Fluxo funcional
Executar a ação autorizada, validar entradas e estado, persistir efeitos quando aplicável e apresentar resultado sem expor credenciais.
## 4. Critérios de aceite
HU-001-CA001: Cadastro atribui somente USER; e-mail único normalizado; confirmação obrigatória; nenhuma elevação de perfil por entrada pública.
HU-001-CA002: entradas inválidas e acesso não autorizado são rejeitados, com erro compreensível.
## 5. Regras de negócio
HU-001-RN001 — Cadastro atribui somente USER; e-mail único normalizado; confirmação obrigatória; nenhuma elevação de perfil por entrada pública. Origem: pedido humano de usuários e perfis completos e delegação prévia; CONFIRMED.
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
Cadastro atribui somente USER; e-mail único normalizado; confirmação obrigatória; nenhuma elevação de perfil por entrada pública.
