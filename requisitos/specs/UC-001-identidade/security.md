# Security Spec — UC-001 Identidade
NEW; APPROVED por delegação.
## Aplicabilidade
Identidade, credenciais e administração exigem controle em servidor.
## Autenticação
Senha protegida por função resistente à força bruta. E-mail confirmado e conta ativa obrigatórios. Sessão curta, validada com estado atual da conta em cada requisição; invalidada após alterações sensíveis. Mensagens genéricas evitam enumeração nos fluxos públicos. Tentativas de autenticação e envio limitadas por origem.
## Autorização
Somente administrador acessa administração. Usuário consulta e altera somente seu nome. Perfil enviado no cadastro é ignorado. Proteção contra administração da própria conta e remoção do último administrador ativo.
## Dados sensíveis
Não retornar nem registrar senha, provas de ação ou sessão. Credencial de sessão inacessível a scripts. Links de ação imprevisíveis, provas armazenadas protegidas, expiração e consumo único atômico. Não carregar recurso externo em página de ação. Requisições de alteração de estado exigem prova de origem. Ambiente publicado requer conexão segura.
## Validação
Limites e domínios conforme business.md; paginação limitada; comandos de consulta não podem ser alterados pelas entradas. Mensagens de erro não expõem implementação.
## Auditoria
Login e alterações sensíveis com ator, alvo, ação e data. Sem segredos.
## Integrações
E-mail configurado externamente. Proteger fila de mensagens por credenciais de banco. Remover corpo da mensagem depois do envio. Credenciais administrativas e de assinatura externas ao código.
## Riscos
Reenvio de mensagem em caso de falha após entrega é possível; uso único limita efeito. Limitação de requisições por processo requer gateway compartilhado antes de escalar réplicas.
## Pontos a validar
Sem bloqueantes para execução local; publicação exige segredos e transporte seguro.
## Rastreabilidade
CA001–CA006, RN001–RN009.

## Ampliação
Autoridade administrativa deriva da associação ativa ao perfil ADMIN, nunca de campo enviado pelo cliente. Cadastro público não aceita associação privilegiada. Usuários comuns não consultam CRUD administrativo de perfis. Alterações de perfis/vínculos invalidam sessões envolvidas. Mudança administrativa de e-mail exige nova confirmação e revoga acesso anterior. Convite é protegido, de uso único e 24 horas; ativação define senha sem expô-la ao administrador.
Consumidor valida assinatura, emissor, destinatário, expiração e lista de perfis. Credencial válida localmente pode ter sido revogada no emissor; consulta autenticada de sessão deve ser usada quando a política exigir revogação imediata, falhando fechada se indisponível. Serviço consumidor nunca recebe chave privada. Credenciais Bearer não devem ir para armazenamento persistente de navegador. Resposta de emissão não pode ser armazenada em cache. Navegador mantém proteção de origem; requisição Bearer explícita não pode fazer fallback para cookie ao apresentar credencial inválida.
