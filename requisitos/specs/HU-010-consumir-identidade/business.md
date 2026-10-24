# Business Spec — HU-010 Consumir identidade em outro serviço
## 0. Identificação
ID: HU-010; origem: NEW; módulo: Usuários e Perfis; status APPROVED por decisão delegada em 24/10/2026.
## 1. Objetivo
Consumir identidade em outro serviço.
## 2. História / Necessidade
Como serviço consumidor, quero validar a identidade e os perfis apresentados para autorizar minhas operações.
## 3. Fluxo funcional
Executar a ação autorizada, validar entradas e estado, persistir efeitos quando aplicável e apresentar resultado sem expor credenciais.
## 4. Critérios de aceite
HU-010-CA001: Contrato versionado de credencial com sujeito, emissor, destinatários, expiração, identificador e lista de perfis; publicação de chave pública; emissão explícita para cliente; consulta de sessão para checar revogação atual; serviço consumidor define suas próprias regras de negócio.
HU-010-CA002: entradas inválidas e acesso não autorizado são rejeitados, com erro compreensível.
## 5. Regras de negócio
HU-010-RN001 — Contrato versionado de credencial com sujeito, emissor, destinatários, expiração, identificador e lista de perfis; publicação de chave pública; emissão explícita para cliente; consulta de sessão para checar revogação atual; serviço consumidor define suas próprias regras de negócio. Origem: pedido humano de usuários e perfis completos e delegação prévia; CONFIRMED.
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
Contrato versionado de credencial com sujeito, emissor, destinatários, expiração, identificador e lista de perfis; publicação de chave pública; emissão explícita para cliente; consulta de sessão para checar revogação atual; serviço consumidor define suas próprias regras de negócio.
