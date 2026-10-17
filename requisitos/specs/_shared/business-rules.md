# Achernar Identity — Regras de Negócio (RN-xx)

Formato: **ID · Regra · Origem · Situação**. Origem = prompt (§), decisão (D-xx). Situação: `CONFIRMED` (prompt) ou `INFERRED` (decisão da IA
sob autorização, registrada em `decisions.md`). Cada Spec referencia estes IDs.

## RN-ACC — Contas

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-ACC-01 | Estados da conta: `PENDING_VERIFICATION`, `ACTIVE`, `DISABLED`. Transições: PENDING→ACTIVE (verificação de e-mail ou login social com e-mail verificado pelo provedor); ACTIVE→DISABLED e DISABLED→ACTIVE (somente administrador); PENDING→removida (purga P-50). `anonymized_at` marca contas anonimizadas (permanecem `DISABLED`). | §3, D-11, D-12 | CONFIRMED |
| RN-ACC-02 | O e-mail é normalizado (D-14) e **único** na instalação (inclui contas pendentes e desativadas); pedidos pendentes de troca de e-mail **não** reservam o endereço (a unicidade é revalidada na confirmação). Contas anonimizadas liberam o endereço. | §4, D-14 | CONFIRMED |
| RN-ACC-03 | O ID da conta (`sub`) é estável, opaco, imutável e nunca reutilizado. O e-mail jamais é vínculo com sistemas de negócio. | §4 | CONFIRMED |
| RN-ACC-04 | Uma conta tem 0 ou 1 credencial de senha e 0 ou 1 vínculo por provedor social (Google, Facebook). | D-20 | INFERRED |
| RN-ACC-05 | Toda conta `ACTIVE` mantém ao menos um método de acesso (senha ou vínculo social); operações que removeriam o último são negadas (`LAST_ACCESS_METHOD`). | §3, D-21 | CONFIRMED |
| RN-ACC-06 | Cadastro com e-mail já existente (qualquer estado): resposta idêntica à de sucesso (202); nenhuma conta é criada/alterada; o titular recebe TPL-05. | §8, D-13 | CONFIRMED |
| RN-ACC-07 | Cadastro por e-mail/senha só é aceito se a aplicação (`client_id`) está ativa e `allow_registration=true`; a senha atende RN-PWD-01. | §5, D-46 | CONFIRMED |
| RN-ACC-08 | A conta guarda `locale` (`pt-BR`\|`en`) para idioma dos e-mails; origem: parâmetro do cadastro ou `Accept-Language`; padrão P-64. | D-37 | INFERRED |
| RN-ACC-09 | Conta `PENDING_VERIFICATION`: nenhum token é emitido (senha ou social); login com senha correta ⇒ `EMAIL_NOT_VERIFIED`; verificação pode ser reenviada (limites P-34); purga após P-50. | §3, D-11 | CONFIRMED |
| RN-ACC-10 | Verificar e-mail consome o token de verificação (uso único, 24 h), marca `email_verified_at`, muda a conta para `ACTIVE`. Não cria sessão. | D-11, D-17 | CONFIRMED |
| RN-ACC-11 | Token de verificação de outra finalidade, expirado, consumido ou invalidado ⇒ `TOKEN_INVALID_OR_EXPIRED`. Novo envio invalida o anterior. | D-17 | CONFIRMED |
| RN-ACC-12 | Conta `DISABLED`: sem login (senha ou social), sem refresh, sessões revogadas na desativação, tokens de uso único invalidados, recuperação de senha não reativa, introspecção `active=false`, e-mail continua reservado. | §3, D-12 | CONFIRMED |
| RN-ACC-13 | Anonimização: apenas conta `DISABLED` que não seja administrador ativo; irreversível; e-mail substituído por valor não entregável único, remove senha e vínculos sociais, revoga sessões, mantém `sub` e eventos (e-mail redigido). | §4, D-27 | INFERRED |
| RN-ACC-14 | Troca de e-mail: ver D-47 — reautenticação recente, confirmação no novo endereço, unicidade revalidada na confirmação, demais sessões revogadas, tokens de recuperação pendentes invalidados, notificação ao endereço antigo; no máx. uma solicitação pendente por conta (nova substitui a anterior). | §3, D-47 | CONFIRMED |
| RN-ACC-15 | Esclarecimento de D-47: a confirmação da troca de e-mail ocorre por link e não está vinculada a uma sessão; portanto **todas** as sessões da conta são revogadas (`EMAIL_CHANGED`) e o usuário entra de novo com o novo e-mail. | D-47 | INFERRED |

## RN-AUT — Autenticação por senha

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-AUT-01 | Login por senha só é concluído se: aplicação ativa; credenciais corretas; conta `ACTIVE`; não bloqueada por tentativas. | §3 | CONFIRMED |
| RN-AUT-02 | Credenciais inválidas, conta inexistente e bloqueio por tentativas produzem a **mesma** resposta `INVALID_CREDENTIALS`, com tempo equivalente (D-48). | §8, D-13 | CONFIRMED |
| RN-AUT-03 | Bloqueio: P-30 (5 falhas/15 min ⇒ 15 min, dobrando até 120 min); chave = e-mail normalizado (existindo ou não); limites por IP P-31. Recuperação de senha concluída zera o contador. | §8, D-16 | CONFIRMED |
| RN-AUT-04 | Toda chamada identifica a aplicação por `client_id`; `CONFIDENTIAL` também autentica com segredo (HTTP Basic). `BROWSER` exige `Origin` permitido nas rotas com cookie e nas de login. | §5, D-08 | CONFIRMED |
| RN-AUT-05 | Audiências do token: ver D-09/RN-APP-17. Pedido fora do permitido ⇒ `AUDIENCE_NOT_ALLOWED`. | §5, §7 | CONFIRMED |
| RN-AUT-06 | Cada login cria uma **nova sessão** (RN-SES-01) e a registra com `amr`, `auth_time`, IP e agente (truncados). Excedeu P-05 ⇒ revoga a menos recente (`SESSION_LIMIT`). | D-44 | INFERRED |
| RN-AUT-07 | Se o hash da senha usa parâmetros antigos, é regravado no login bem-sucedido. | D-15 | INFERRED |
| RN-AUT-08 | Respostas com tokens carregam `Cache-Control: no-store`. Refresh de `BROWSER` é entregue **somente** em cookie (nunca no corpo). | §8, D-07 | CONFIRMED |
| RN-AUT-09 | Logout encerra a sessão do refresh apresentado; é idempotente (sempre 204), limpa o cookie e não revela se o token era válido. | §3 | CONFIRMED |
| RN-AUT-10 | O login bem-sucedido conta como reautenticação (`last_reauth_at = auth_time`). | D-22 | INFERRED |
| RN-AUT-11 | O corpo do login nunca é logado; falhas registram `LOGIN_FAILED` sem o valor da senha. | §8 | CONFIRMED |

## RN-TOK — Tokens, chaves e validação

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-TOK-01 | Access token JWT RS256 com claims de D-05; validade P-01 (limitada pela aplicação); `typ=at+jwt`. | §7 | CONFIRMED |
| RN-TOK-02 | Refresh token opaco de 256 bits, guardado só como hash; **rotaciona a cada uso**; novo token herda a sessão. | §7 | CONFIRMED |
| RN-TOK-03 | Reutilização de refresh já rotado, fora da tolerância (P-04), revoga a sessão inteira, registra `REFRESH_REUSE_DETECTED` e responde `INVALID_REFRESH_TOKEN`. | §7 | CONFIRMED |
| RN-TOK-04 | Reutilização do **antecessor imediato** dentro de P-04: responde novo access token **sem** novo refresh e sem nova rotação; registra `REFRESH_REUSE_TOLERATED`. Qualquer outro token antigo ⇒ RN-TOK-03. | §7 | CONFIRMED |
| RN-TOK-05 | Validade do refresh = mín(agora + P-02, `session.created_at` + P-03). Ao atingir o teto absoluto, nova autenticação é exigida. | §7 | CONFIRMED |
| RN-TOK-06 | Refresh falha (`INVALID_REFRESH_TOKEN`) se sessão revogada/expirada, conta ≠ `ACTIVE` ou aplicação inativa. | §7 | CONFIRMED |
| RN-TOK-07 | O refresh pode pedir subconjunto das audiências da sessão; nunca ampliar. Audiência de API desativada é omitida. | §7 | CONFIRMED |
| RN-TOK-08 | Refresh só vale para o `client_id` que o emitiu (cookie por aplicação / cliente autenticado). | §5 | CONFIRMED |
| RN-TOK-09 | **Revogar refresh/sessão não invalida access tokens já emitidos**; eles valem até `exp` (≤ P-01). Para bloqueio imediato, APIs usam introspecção (RN-TOK-11). | §7 | CONFIRMED |
| RN-TOK-10 | Revogam sessão(ões): logout; revogação pelo titular; encerrar todas; desativação da conta (todas); redefinição de senha (todas); troca de senha (demais); troca de e-mail (demais); reutilização de refresh (a sessão); limite P-05 (a mais antiga); desativação de aplicação (todas da aplicação); anonimização (todas). | §7 | CONFIRMED |
| RN-TOK-11 | Introspecção (`POST /api/v1/tokens/introspect`) autenticada pela API: `active=true` somente se assinatura/`iss`/`exp` válidos **e** `aud` contém a audiência da API chamadora **e** sessão ativa **e** conta `ACTIVE` **e** aplicação ativa. Caso contrário `{active:false}` sem motivo. | §7 | CONFIRMED |
| RN-TOK-12 | Ciclo da chave: `PUBLISHED`→`ACTIVE`→`RETIRED`→`REMOVED` (D-25). Sempre há exatamente 1 chave `ACTIVE`. Rotação automática P-10; manual/emergencial por admin. | §7 | CONFIRMED |
| RN-TOK-13 | O JWKS lista chaves `PUBLISHED`, `ACTIVE` e `RETIRED` (apenas parte pública), `Cache-Control: public, max-age=P-14`. | §7 | CONFIRMED |
| RN-TOK-14 | API consumidora **deve** validar: assinatura (JWKS do emissor), `alg=RS256`, `iss` = emissor configurado, `aud` contém sua audiência, `exp`/`nbf` com tolerância P-06; deve ignorar tokens sem `sub`. Autorização de negócio é da API. | §7 | CONFIRMED |
| RN-TOK-15 | A API do próprio Identity valida o `sid` no banco em toda chamada (revogação imediata) e confere perfil/estado da conta no banco para operações administrativas. | §7, D-23 | CONFIRMED |

## RN-SES — Sessões do usuário

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-SES-01 | Sessão = (conta, aplicação, audiências, `amr`, criada em, último uso, expira em, revogada em/motivo). `sid` identifica-a no token. | §7 | CONFIRMED |
| RN-SES-02 | O titular lista apenas suas sessões ativas (aplicação, criada, último uso, IP aproximado, agente resumido, “esta sessão”). | §3 | CONFIRMED |
| RN-SES-03 | O titular revoga uma sessão ou todas (opção de manter a atual); “todas” exige reautenticação recente (D-22). Revogar a atual equivale a logout. | §3 | CONFIRMED |
| RN-SES-04 | Não é possível revogar sessões de outra conta pela API de conta; sessão inexistente/de outra conta ⇒ `NOT_FOUND`. | §3 | CONFIRMED |

## RN-PWD — Senhas e reautenticação

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-PWD-01 | Política: P-15 ≤ comprimento ≤ P-17 (caracteres Unicode, NFKC); rejeita lista de comuns, a parte local/domínio do e-mail (≥ 4 caracteres) e só espaços; sem regras de composição nem expiração. | §8, D-15 | CONFIRMED |
| RN-PWD-02 | Alterar senha (conta com senha): exige `current_password` correta e nova senha diferente da atual; atende RN-PWD-01. | §3 | CONFIRMED |
| RN-PWD-03 | Após alterar: revoga as demais sessões, mantém a atual, notifica por e-mail (TPL-06), evento `PASSWORD_CHANGED`. | §3 | CONFIRMED |
| RN-PWD-04 | Recuperação: solicitação sempre 202; só envia e-mail se a conta existe e está `ACTIVE` (pendente ⇒ reenvia verificação, TPL-01; desativada ⇒ nada; anonimizada ⇒ nada). Token 30 min, uso único. | §3, D-13 | CONFIRMED |
| RN-PWD-05 | Redefinir: valida token + política; grava hash; revoga **todas** as sessões; zera bloqueio; invalida outros tokens de recuperação/troca de e-mail da conta; notifica (TPL-06); **não** loga automaticamente. | §3 | CONFIRMED |
| RN-PWD-06 | Conta social sem senha pode usar a recuperação para definir a primeira senha (e-mail verificado comprova titularidade). | §3 | CONFIRMED |
| RN-PWD-07 | Definir senha local (UC-037): conta `ACTIVE` sem senha, reautenticação social ≤ P-07, política RN-PWD-01; demais sessões mantidas; notifica (TPL-06). | §3 | CONFIRMED |
| RN-PWD-08 | Reautenticação (UC-033/055): válida por P-07 e registrada em `session.last_reauth_at`; senha incorreta conta no bloqueio P-30; exigida em: alterar/definir senha, solicitar troca de e-mail, vincular/desvincular provedor, encerrar todas as sessões. | D-22 | INFERRED |

## RN-SOC — Login social

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-SOC-01 | Fluxos oficiais: Authorization Code + PKCE + `state` (+ `nonce` no OIDC do Google) executados pelo backend do Identity; segredo do provedor só no backend. | §6 | CONFIRMED |
| RN-SOC-02 | Início só com `client_id` ativo, provedor habilitado (global+aplicação), `redirect_uri` registrada (correspondência exata) e `code_challenge` (S256) do cliente. | §6 | CONFIRMED |
| RN-SOC-03 | Vínculo existente (`provider`,`provider_subject`) ⇒ login na conta vinculada (se `ACTIVE`; `DISABLED` ⇒ `account_disabled`; `PENDING_VERIFICATION` ⇒ `email_verification_required`). | §3 | CONFIRMED |
| RN-SOC-04 | Sem vínculo e sem conta de mesmo e-mail: cria conta + vínculo (e-mail verificado pelo provedor ⇒ `ACTIVE`; senão `PENDING_VERIFICATION` + verificação própria e `email_verification_required`). Sem e-mail ⇒ `email_unavailable`. | §3, D-20 | CONFIRMED |
| RN-SOC-05 | Sem vínculo, e-mail **verificado pelo provedor** e conta local `ACTIVE` ou `DISABLED` de mesmo e-mail: **não vincula**, não cria; retorna `account_link_required`. Com e-mail **não** verificado pelo provedor e conta existente de qualquer estado: nada é alterado, o titular recebe TPL-05 e o navegador recebe `email_verification_required`. | §3 | CONFIRMED |
| RN-SOC-06 | Sem vínculo e com conta local `PENDING_VERIFICATION` de mesmo e-mail e e-mail social verificado: a conta pendente é substituída (credencial descartada) e a conta social segue RN-SOC-04. | D-20 | INFERRED |
| RN-SOC-07 | Vincular (UC-053) exige sessão autenticada, reautenticação ≤ P-07 e conclusão do fluxo no provedor pelo mesmo usuário (transação com `account_id`); a identidade não pode estar vinculada a outra conta. | §3 | CONFIRMED |
| RN-SOC-08 | Nenhuma vinculação ocorre apenas por coincidência de e-mail em qualquer caso. | §3 | CONFIRMED |
| RN-SOC-09 | Desvincular exige reautenticação e que a conta mantenha outro método (RN-ACC-05). | §3 | CONFIRMED |
| RN-SOC-10 | Conta social sem senha: ver RN-PWD-06/07. | §3 | CONFIRMED |
| RN-SOC-11 | Mudança do e-mail no provedor não altera a conta; e-mail social divergente do e-mail da conta não é usado para vincular. | D-20 | INFERRED |
| RN-SOC-12 | Reautenticação social (`REAUTH`): só valida se a identidade devolvida for um vínculo existente **da própria conta** da sessão. | D-22 | INFERRED |
| RN-SOC-13 | Transação: `state` aleatório de 256 bits, 10 min, uso único atômico, amarrada ao navegador por cookie `Lax`; reuso, expiração ou cookie ausente ⇒ falha sem emitir nada. | §6 | CONFIRMED |
| RN-SOC-14 | Tokens do provedor jamais saem do backend nem aparecem em URL; o cliente recebe apenas `code` próprio (uso único, P-08) vinculado a `client_id`, `redirect_uri` e `code_challenge`; troca via `POST /auth/social/exchange` com `code_verifier`. Cancelamento/falha ⇒ redireciona com `error`. | §6 | CONFIRMED |

## RN-APP — Aplicações e APIs consumidoras

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-APP-01 | `client_id` único, imutável, `[a-z0-9-]{3,64}`. | §5 | CONFIRMED |
| RN-APP-02 | Tipos `BROWSER` (público) e `CONFIDENTIAL` (segredo). | D-08 | INFERRED |
| RN-APP-03 | Origens permitidas: `scheme://host[:port]` exatos, sem caminho/curinga; `https` (exceto `http://localhost`/`127.0.0.1` em `dev`). `BROWSER` exige ≥ 1 origem. | §5, §8 | CONFIRMED |
| RN-APP-04 | URLs de retorno (`redirect_uris`) e URLs de ação (`verify_email_url`, `reset_password_url`, `confirm_email_change_url`) são absolutas, sem fragmento nem curinga, com origem registrada na aplicação; comparação exata. | §5 | CONFIRMED |
| RN-APP-05 | `client_id`, CORS e `redirect_uri` **não provam** a autenticação de uma aplicação; só o segredo (`CONFIDENTIAL`) a prova. Aplicações `BROWSER` são tratadas como não confiáveis para segredos e têm privilégios limitados (sem introspecção, sem refresh no corpo). | §5, §8 | CONFIRMED |
| RN-APP-06 | Segredo: 256 bits aleatórios, exibido uma única vez, guardado como hash; até 2 ativos; rotação cria novo e permite revogar o antigo; nunca é devolvido por consulta. | §5, §8 | CONFIRMED |
| RN-APP-07 | Desativar aplicação: novos logins/refresh/exchange ⇒ `INVALID_CLIENT`; todas as suas sessões são revogadas; reativar não restaura sessões. | §5 | CONFIRMED |
| RN-APP-08 | A aplicação tem lista de APIs permitidas (≥ 1 se `identity_api_access=false`) e subconjunto **padrão**. | §5, §7 | CONFIRMED |
| RN-APP-09 | TTL do access pode ser sobrescrito dentro de P-01 (5–15 min). | §7 | CONFIRMED |
| RN-APP-10 | `identity_api_access=true` adiciona a audiência P-63 ao token (necessário para conta/sessões/admin). | D-46 | INFERRED |
| RN-APP-11 | Provedores sociais habilitados por aplicação (`google_enabled`,`facebook_enabled`), só se configurados globalmente. | D-45 | INFERRED |
| RN-APP-12 | `GET /auth/applications/{client_id}/config` expõe somente: nome, `allow_registration`, provedores habilitados, tipo; nada de segredos, origens ou audiências. Aplicação inativa/desconhecida ⇒ 404 genérico. | D-45 | INFERRED |
| RN-APP-13 | Aplicações não são excluídas (somente desativadas); toda alteração gera evento; limites P-74; alterações usam controle otimista. | §3 | INFERRED |
| RN-APP-14 | API consumidora: `audience` única e imutável (≤ 200 chars), nome, estado. | §5 | CONFIRMED |
| RN-APP-15 | API desativada: omitida de novos tokens; introspecção por ela ⇒ `INVALID_CLIENT`; tokens já emitidos valem até `exp`. | §5 | CONFIRMED |
| RN-APP-16 | A introspecção usa credencial da API (`audience` + segredo, HTTP Basic); regras de segredo como RN-APP-06. | §7 | CONFIRMED |
| RN-APP-17 | Política de emissão: `aud` = (audiências pedidas ∩ permitidas à aplicação ∩ APIs ativas) ∪ {P-63 se `identity_api_access`}; vazio ⇒ `AUDIENCE_NOT_ALLOWED`. | §7 | CONFIRMED |

## RN-ADM — Administração

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-ADM-01 | Funções administrativas exigem perfil `ADMIN` verificado no banco, conta `ACTIVE` e sessão ativa; claim `roles` não basta. | §8 | CONFIRMED |
| RN-ADM-02 | Desativar conta: revoga todas as sessões, invalida tokens de uso único, notifica o titular (TPL-09), registra motivo opcional e evento. | §3 | CONFIRMED |
| RN-ADM-03 | **Último administrador ativo** não pode ser desativado, rebaixado nem anonimizado (`LAST_ADMIN_PROTECTED`), inclusive sob concorrência. | D-23 | INFERRED |
| RN-ADM-04 | Admin não desativa a si mesmo; pode rebaixar a si mesmo se houver outro admin ativo. Conceder perfil exige conta-alvo `ACTIVE` com e-mail verificado. | D-23 | INFERRED |
| RN-ADM-05 | Reativar: `DISABLED`→`ACTIVE` (não anonimizada); sessões não retornam; notifica (TPL-09). | §3 | CONFIRMED |
| RN-ADM-06 | Anonimizar: ver RN-ACC-13; exige confirmação explícita (digitar o e-mail atual). | D-27 | INFERRED |
| RN-ADM-07 | Consulta de contas: paginação (P-57), filtros por texto de e-mail, estado, perfil, método de acesso e período; resposta sem hashes/segredos. | §3 | CONFIRMED |
| RN-ADM-08 | Toda ação administrativa registra evento com `actor_account_id`, alvo e resultado; administradores não personificam usuários nem leem senhas/tokens. | §3 | CONFIRMED |
| RN-ADM-09 | Desativar conta, conceder/revogar perfil administrador e anonimizar exigem reautenticação recente (P-07) do administrador, além das demais regras. | D-22 | INFERRED |

## RN-EML — E-mails

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-EML-01 | Mensagens definidas: TPL-01 verificação, TPL-02 recuperação, TPL-03 confirmação de troca (novo endereço), TPL-04 aviso de troca (antigo), TPL-05 cadastro duplicado, TPL-06 senha alterada/definida/redefinida, TPL-07 e-mail alterado, TPL-08 bloqueio por tentativas, TPL-09 conta desativada/reativada, TPL-10 vínculo social criado/removido, TPL-11 todas as sessões encerradas. | §9 | CONFIRMED |
| RN-EML-02 | Envio assíncrono por outbox gravada na mesma transação; falhas de SMTP não afetam a resposta da API (D-18). | §9 | CONFIRMED |
| RN-EML-03 | Limites de reenvio: P-33/P-34/P-35; excedidos ⇒ 202 silencioso (fluxos anti-enumeração) ou `429`. | §9 | CONFIRMED |
| RN-EML-04 | Links usam a URL de ação da aplicação de origem; sem ela, a UI do Identity (P-61); token no fragmento. | §9 | CONFIRMED |
| RN-EML-05 | E-mails não contêm senhas nem tokens além do link de ação; nenhum conteúdo de conta de terceiros. | §9 | CONFIRMED |
| RN-EML-06 | Idioma pelo `locale` da conta (TPL-05 pelo `Accept-Language`). | D-37 | INFERRED |
| RN-EML-07 | Tentativas e backoff P-40; esgotadas ⇒ `FAILED` + `EMAIL_DELIVERY_FAILED`; reemissão manual não é necessária (usuário pede novo link). | §9 | CONFIRMED |
| RN-EML-08 | Remetente, SMTP e URLs públicas são parâmetros; nenhuma credencial real no repositório. | §9 | CONFIRMED |

## RN-AUD / RN-RET — Auditoria e retenção

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-AUD-01 | Eventos do `event-catalog.md` são obrigatórios; eventos de mudança de estado são gravados na mesma transação. | §3 | CONFIRMED |
| RN-AUD-02 | Registros são imutáveis para a aplicação (sem UPDATE/DELETE exceto job de retenção e anulação de IP). | D-26 | INFERRED |
| RN-AUD-03 | Consulta administrativa: filtros por período, tipo, conta, aplicação, resultado, severidade; paginação por cursor; somente leitura. | §3 | CONFIRMED |
| RN-AUD-04 | `details` só aceita chaves da whitelist do catálogo; nunca segredos, tokens, senhas, `code`, `state`. | §8 | CONFIRMED |
| RN-AUD-05 | Cada requisição recebe `request_id`/`traceId` presente no evento e nos logs. | §8 | CONFIRMED |
| RN-AUD-06 | Eventos `ALERT` geram log `WARN` e métrica `achernar_security_alert_total{type}`. | D-35 | INFERRED |
| RN-RET-01 | Retenções P-50…P-56, P-59 aplicadas por job diário idempotente (03:00 UTC), em lotes, com evento `RETENTION_PURGE_RUN`. | §4 | CONFIRMED |
| RN-RET-02 | Eventos de segurança: 365 dias; IP/UA anulados após 90 dias. | §4 | CONFIRMED |
| RN-RET-03 | Sessões/refresh encerrados: 30 dias após o fim (necessário para detecção de reuso de tokens recentes). | §4 | CONFIRMED |
| RN-RET-04 | Tokens de uso único, transações e códigos sociais: 7 dias após expirar/consumir. | §4 | CONFIRMED |
| RN-RET-05 | Contas pendentes não verificadas: removidas após 7 dias (libera o e-mail). | §4 | CONFIRMED |
| RN-RET-06 | Dados de contas desativadas permanecem até anonimização solicitada pelo titular/controlador. | §4 | CONFIRMED |

## RN-SET — Primeiro administrador

| ID | Regra | Origem | Sit. |
|---|---|---|---|
| RN-SET-01 | O setup está disponível somente enquanto não existir administrador `ACTIVE`; depois, `404 SETUP_NOT_AVAILABLE`. | §3 | CONFIRMED |
| RN-SET-02 | Exige `bootstrap_token` igual ao segredo de implantação (comparação em tempo constante); limite P-38; falha ⇒ `SETUP_TOKEN_INVALID` + evento. | §3 | CONFIRMED |
| RN-SET-03 | Cria conta `ACTIVE`, e-mail verificado, senha conforme RN-PWD-01, perfil `ADMIN`, evento `BOOTSTRAP_ADMIN_CREATED`; não cria sessão. | §3 | CONFIRMED |
| RN-SET-04 | Se não há admin ativo e `ACHERNAR_BOOTSTRAP_TOKEN` não está definido: setup indisponível e a aplicação registra aviso na subida (em `prod`, falha de subida). | §3 | CONFIRMED |
| RN-SET-05 | Na primeira subida cria-se a aplicação `achernar-identity-ui` (D-46) a partir de P-61/P-62/P-63; nenhuma credencial fixa em código, imagem ou Compose. | §3 | CONFIRMED |
| RN-SET-06 | Se o e-mail informado já pertence a uma conta, o setup responde `409 CONFLICT` sem alterar nada (o chamador já provou o segredo de implantação; não há enumeração por anônimo). | §3 | INFERRED |
| RN-SET-07 | O setup não cria sessão nem tokens; o administrador entra pelo login normal da aplicação `achernar-identity-ui`. | §3 | INFERRED |
