# Business Spec — FT-13 E-mails transacionais

## 0. Identificação
FT-13 (UC-095) · `NEW` · Módulo M9 E-mail · `APPROVED` (documental). Normativo: `emails.md`.

## 1. Objetivo
Entregar de forma confiável, assíncrona, acessível e bilíngue as mensagens do produto (TPL-01…TPL-11) sem que falhas de e-mail afetem as operações de autenticação e sem expor segredos.

## 2. História
**Como** titular **quero** receber links e avisos de segurança claros no meu idioma. **Como** operador **quero** que a indisponibilidade do SMTP não derrube cadastro e recuperação.

## 3. Fluxo (UC-095 — Enviar e-mails transacionais)
1. Uma operação de negócio grava, na mesma transação, a mensagem pendente (modelo, idioma, destinatário, dados de renderização protegidos).
2. Um processo em segundo plano recolhe mensagens vencidas em lotes, renderiza o modelo no idioma, monta o link a partir da configuração e envia por SMTP.
3. Sucesso: marca como enviada e apaga os dados de renderização. Falha temporária: agenda nova tentativa (1 min, 5 min, 15 min, 1 h, 6 h). Falha permanente ou tentativas esgotadas: marca como falha e registra evento de alerta.

## 4. Critérios de aceite
- **CA-FT13-01 — Mesma transação.** A mensagem é gravada na transação da operação; se a operação sofre rollback, nenhum e-mail é enviado. Ref.: RN-EML-02
- **CA-FT13-02 — API não espera o SMTP.** Com SMTP indisponível, cadastro, reenvio e recuperação respondem normalmente; as mensagens são entregues quando o SMTP volta. Ref.: RNF-17, D-18
- **CA-FT13-03 — Reentrega e backoff.** Falhas temporárias (4xx, timeout, conexão) são repetidas após 1 min, 5 min, 15 min, 1 h, 6 h (6 tentativas no total); sucesso interrompe o ciclo. Ref.: RN-EML-07, P-40
- **CA-FT13-04 — Falha definitiva.** Erro permanente de destinatário ⇒ `FAILED` imediato; tentativas esgotadas ⇒ `FAILED`, evento `EMAIL_DELIVERY_FAILED`, linha mantida 7 dias sem conteúdo. Ref.: RN-EML-07, P-41
- **CA-FT13-05 — Dados protegidos.** O conteúdo pendente (link/token) fica cifrado e é apagado ao enviar ou falhar; `last_error` nunca contém dados pessoais ou o token. Ref.: SEC-24, RN-EML-05
- **CA-FT13-06 — Concorrência.** Duas instâncias do processo nunca enviam a mesma mensagem ao mesmo tempo; um processo que cai libera a mensagem após 2 minutos. Ref.: RNF-14
- **CA-FT13-07 — Catálogo.** Existem os modelos TPL-01…TPL-11 com gatilhos, destinatários e validades de `emails.md §4`; mensagens condicionais só para contas em estado compatível. Ref.: RN-EML-01
- **CA-FT13-08 — Idioma.** Idioma pelo `locale` da conta (TPL-05 pelo `Accept-Language`/padrão); ambos os idiomas completos para os 11 modelos. Ref.: RN-EML-06, RNF-09
- **CA-FT13-09 — Links.** Link usa a URL de ação da aplicação de origem; sem ela, a UI do Identity; token apenas no fragmento; nunca derivado de `Host`. Ref.: RN-EML-04, SEC-22
- **CA-FT13-10 — Conteúdo seguro.** E-mails não contêm senha nem token além do link de ação, nem rastreadores ou imagens remotas; validade informada; “se você não solicitou, ignore”. Ref.: RN-EML-05
- **CA-FT13-11 — Formato e acessibilidade.** Multipart (texto + HTML) UTF-8, HTML semântico com `lang`, contraste AA, fonte ≥ 16 px, largura fluida ≤ 600 px, botão com texto e URL em texto puro; assunto ≤ 70 caracteres. Ref.: RNF-07
- **CA-FT13-12 — Limites de reenvio.** Respeita os limites P-33…P-35 e 1 TPL-05 por hora por destinatário; TPL-08 uma vez por bloqueio. Ref.: RN-EML-03
- **CA-FT13-13 — Notificações de segurança.** São enviados TPL-04/06/07/08/09/10/11 nos eventos definidos, ao endereço correto (TPL-04/07 ao antigo, TPL-03 ao novo). Ref.: RF-23
- **CA-FT13-14 — Configuração.** Remetente, SMTP, URLs e suporte vêm de parâmetros; em `prod` STARTTLS/SMTPS e autenticação são obrigatórios; sem credenciais no repositório; em `dev` usa o servidor de captura local. Ref.: RN-EML-08, P-42, P-43
- **CA-FT13-15 — Observabilidade.** Métricas de pendentes e falhas; readiness não depende do SMTP; logs sem destinatário completo em nível `INFO` (mascarado). Ref.: RNF-05, RNF-06
- **CA-FT13-16 — Idempotência para o usuário.** Reentrega (at-least-once) não causa dano: links são de uso único e novo envio invalida o anterior. Ref.: D-17

## 5. Regras: RN-EML-01…08, RN-AUD-01 · Perfil: sistema.
## 6. Rastreabilidade: UC-095 → CA-FT13-01…16 · RF-23 → CA-FT13-07, 13.
## 7. Pontos a validar: provedor SMTP de produção (A-02) é decisão do operador; o produto aceita qualquer SMTP. Divergências / LEGACY: nenhuma / `NOT_APPLICABLE`.
## 8. Resultado esperado: e-mails confiáveis e seguros sem acoplar a disponibilidade da API ao SMTP.
