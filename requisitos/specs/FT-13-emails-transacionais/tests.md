# Test Spec — FT-13 E-mails transacionais
Cenários para execução futura (servidor SMTP de captura local). `APPROVED` (documental).

- **CT-FT13-01** Rollback da operação não gera e-mail; commit gera. CA-FT13-01
- **CT-FT13-02** SMTP parado: cadastro/recuperação respondem; ao voltar, mensagens são entregues. CA-FT13-02
- **CT-FT13-03** Cronograma de backoff com relógio controlado (6 tentativas). CA-FT13-03
- **CT-FT13-04** Erro permanente ⇒ `FAILED` imediato; esgotamento ⇒ evento `EMAIL_DELIVERY_FAILED`. CA-FT13-04
- **CT-FT13-05** Conteúdo cifrado no banco; apagado após envio; `last_error` sem dados pessoais. CA-FT13-05
- **CT-FT13-06** Duas instâncias do processo: sem duplicidade; queda libera em 2 min. CA-FT13-06
- **CT-FT13-07** Matriz de 11 modelos × gatilho × destinatário × validade; condicionais. CA-FT13-07, 13
- **CT-FT13-08** Idiomas pt-BR/en completos; TPL-05 por `Accept-Language`. CA-FT13-08
- **CT-FT13-09** Links: URL da app, fallback, fragmento, `Host` forjado. CA-FT13-09
- **CT-FT13-10** Varredura do corpo: sem senha, sem rastreadores, sem imagens remotas. CA-FT13-10
- **CT-FT13-11** Estrutura multipart, assunto ≤ 70, acessibilidade do HTML (contraste, `lang`, tamanho). CA-FT13-11
- **CT-FT13-12** Limites de reenvio e TPL-05/TPL-08. CA-FT13-12
- **CT-FT13-13** Configuração: `prod` sem TLS/credencial falha a subida; `dev` usa captura local; repositório sem credenciais. CA-FT13-14
- **CT-FT13-14** Métricas e mascaramento de destinatário em logs; readiness independe do SMTP. CA-FT13-15
- **CT-FT13-15** Reentrega duplicada: link continua de uso único. CA-FT13-16
