# Test Spec — FT-10 Chaves, JWKS e introspecção
Cenários para execução futura. `APPROVED` (documental).

- **CT-FT10-01** JWKS: conteúdo, cabeçalhos de cache, sem material privado, desempenho. CA-FT10-01
- **CT-FT10-02** `kid` do token ↔ JWKS ↔ thumbprint. CA-FT10-02
- **CT-FT10-03** Metadados corretos e sem `authorization_endpoint`. CA-FT10-03
- **CT-FT10-04** Primeira subida e subidas concorrentes (2 instâncias). CA-FT10-04, 09
- **CT-FT10-05** Invariante de uma chave ativa durante toda a rotação. CA-FT10-05
- **CT-FT10-06** Linha do tempo da rotação com relógio controlado (publicar, promover, aposentar, remover). CA-FT10-06
- **CT-FT10-07** Token da chave anterior válido até `exp` e verificável via JWKS durante P-12. CA-FT10-07
- **CT-FT10-08** Configuração inconsistente impede a subida. CA-FT10-08
- **CT-FT10-09** Admin lista chaves sem material privado; usuário comum recusado. CA-FT10-10
- **CT-FT10-10** Rotação manual. CA-FT10-11
- **CT-FT10-11** Remoção emergencial (com e sem revogação de sessões); a API de exemplo passa a rejeitar após refetch. CA-FT10-12
- **CT-FT10-12** Remoção da única ativa sem emergência. CA-FT10-13
- **CT-FT10-13** Introspecção ativa. CA-FT10-14
- **CT-FT10-14** Introspecção inativa (cada causa) sempre com o mesmo corpo. CA-FT10-15
- **CT-FT10-15** Credencial inválida/revogada/API desativada. CA-FT10-16
- **CT-FT10-16** Limite 600/min. CA-FT10-17
- **CT-FT10-17** Carga: 50 req/s, p95 ≤ 80 ms. CA-FT10-18
- **CT-FT10-18** Efeito imediato após revogação/desativação. CA-FT10-19
- **CT-FT10-19** Conteúdo mínimo e `no-store`. CA-FT10-20
- **CT-FT10-20** Interoperabilidade: API de exemplo Spring Security (monolítica e microsserviço) valida tokens e rejeita `aud`/`iss`/assinatura/`typ` errados. CA-FT10-21
- **CT-FT10-21** API do Identity recusa `sid` revogado e perfil rebaixado imediatamente. CA-FT10-22
- **CT-FT10-22** UI SCR-29 (diálogos, confirmação por digitação, `axe`). CA-FT10-10…12
