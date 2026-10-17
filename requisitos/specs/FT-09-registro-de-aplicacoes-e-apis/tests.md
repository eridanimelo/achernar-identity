# Test Spec — FT-09 Registro de aplicações e APIs
Cenários para execução futura. `APPROVED` (documental).

- **CT-FT09-01** Cadastro BROWSER e CONFIDENTIAL; segredo exibido uma vez. CA-FT09-01
- **CT-FT09-02** `client_id`: formatos válidos/inválidos, duplicado, imutável. CA-FT09-02
- **CT-FT09-03** Origens: https, localhost em dev/prod, caminho, curinga, 21 itens. CA-FT09-03
- **CT-FT09-04** URLs: relativas, com fragmento, origem não registrada, curinga. CA-FT09-04
- **CT-FT09-05** Audiências: sem API e sem acesso ao Identity; sem padrão; API inexistente. CA-FT09-05
- **CT-FT09-06** TTL 299/300/900/901. CA-FT09-06
- **CT-FT09-07** Acesso ao Identity adiciona audiência no token. CA-FT09-07
- **CT-FT09-08** Listagem: paginação, busca, ordenação não permitida, sem segredos. CA-FT09-08
- **CT-FT09-09** PATCH: versão divergente, campo desconhecido, imutáveis. CA-FT09-09
- **CT-FT09-10** Desativar: `INVALID_CLIENT`, sessões revogadas, reativar não restaura. CA-FT09-10
- **CT-FT09-11** Aplicação do sistema protegida. CA-FT09-11
- **CT-FT09-12** Segredos: 2 ativos, terceiro recusado, último não revogável, BROWSER recusado. CA-FT09-12
- **CT-FT09-13** Segredo não recuperável; validação em tempo constante. CA-FT09-13
- **CT-FT09-14** Cadastro de API e credencial. CA-FT09-14
- **CT-FT09-15** Desativar API: omitida de tokens novos; introspecção ⇒ `INVALID_CLIENT`; token emitido vale até `exp`. CA-FT09-15
- **CT-FT09-16** Credenciais de introspecção. CA-FT09-16
- **CT-FT09-17** Política de emissão (tabela de interseções). CA-FT09-17
- **CT-FT09-18** Config pública: conteúdo mínimo, inativa ⇒ 404 igual a desconhecida. CA-FT09-18
- **CT-FT09-19** CORS dinâmico: origem nova passa sem reinício; removida deixa de passar. CA-FT09-19
- **CT-FT09-20** Limites 100/100. CA-FT09-20
- **CT-FT09-21** Usuário comum e anônimo nas rotas admin. CA-FT09-21
- **CT-FT09-22** Semeadura idempotente da aplicação da UI. CA-FT09-22
- **CT-FT09-23** Provedor não configurado globalmente não pode ser habilitado. CA-FT09-23
- **CT-FT09-24** Concorrência: rotações e cadastros simultâneos. CA-FT09-24
- **CT-FT09-25** Eventos de auditoria completos e sem segredos. CA-FT09-25
- **CT-FT09-26** UI SCR-24…28 (formulários, diálogo de segredo, conflito de versão, `axe`). CA-FT09-01, 09, 12, 14
