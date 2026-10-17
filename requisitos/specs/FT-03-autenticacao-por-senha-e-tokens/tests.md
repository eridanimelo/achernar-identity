# Test Spec — FT-03 Autenticação por senha e tokens

Cenários para execução futura; não representa execução. `APPROVED` (documental).

- **CT-FT03-01** Login BROWSER (cookie, sem refresh no corpo, flags do cookie). CA-FT03-01
- **CT-FT03-02** Login CONFIDENTIAL (refresh no corpo; Basic inválido ⇒ `INVALID_CLIENT`). CA-FT03-02, 06
- **CT-FT03-03** Respostas/tempos idênticos: inexistente × senha errada × bloqueado (200 amostras, ≤ 15 %). CA-FT03-03
- **CT-FT03-04** Bloqueio 5/15 min, escalonamento 15→30→60→120, liberação por tempo, TPL-08; recuperação de senha zera. CA-FT03-04
- **CT-FT03-05** Pendente e desativada com senha correta/errada (errada ⇒ `INVALID_CREDENTIALS`). CA-FT03-05
- **CT-FT03-06** Cliente desconhecido, inativo, origem não permitida. CA-FT03-06
- **CT-FT03-07** Audiências: padrão, subconjunto, fora da política, API desativada. CA-FT03-07
- **CT-FT03-08** Claims e cabeçalho do JWT validados contra JWKS; `roles` só com audiência do Identity; TTL 5/10/15 min. CA-FT03-08
- **CT-FT03-09** 11º login revoga a sessão mais antiga (`SESSION_LIMIT`). CA-FT03-09
- **CT-FT03-10** Rehash após mudança de parâmetros. CA-FT03-10
- **CT-FT03-11** Limite por IP e `Retry-After`. CA-FT03-11
- **CT-FT03-12** Cabeçalhos `no-store`, CORS com credenciais restrito. CA-FT03-12
- **CT-FT03-13** Refresh rotaciona; cookie renovado; validade = mín(P-02, teto). CA-FT03-13
- **CT-FT03-14** Reuso em 9 s ⇒ tolerado; em 11 s ⇒ revoga. CA-FT03-14, 15
- **CT-FT03-15** Reuso de token de duas gerações atrás ⇒ revoga. CA-FT03-15
- **CT-FT03-16** Falhas de refresh (todas as causas) ⇒ mesmo código. CA-FT03-16
- **CT-FT03-17** Refresh com subconjunto/ampliação de audiências. CA-FT03-17
- **CT-FT03-18** Relógio no teto absoluto. CA-FT03-18
- **CT-FT03-19** CSRF: sem Origin, Origin errado, sem cabeçalho, outro `client_id`, `text/plain`. CA-FT03-19
- **CT-FT03-20** 50 refresh simultâneos do mesmo token ⇒ 1 rotação, demais toleradas, 1 refresh corrente. CA-FT03-20 (concorrência)
- **CT-FT03-21** Logout válido/ inválido/ ausente ⇒ 204; cookie limpo; sessão revogada. CA-FT03-21
- **CT-FT03-22** Após logout: API de recurso sem introspecção aceita até `exp`; Identity API ⇒ `SESSION_REVOKED`; introspecção ⇒ `active=false`. CA-FT03-22
- **CT-FT03-23** Logs sem senha/token (sentinelas). CA-FT03-23
- **CT-FT03-24** Login seguido de operação sensível sem pedir reautenticação; após 5 min exige. CA-FT03-24
- **CT-FT03-25** UI SCR-06: estados, foco, teclado, `axe`, abas (Web Locks), expiração de sessão. CA-FT03-01, 03, 05

Rastreabilidade: CT-FT03-nn → CA indicado.
