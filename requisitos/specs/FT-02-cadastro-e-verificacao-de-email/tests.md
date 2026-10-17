# Test Spec — FT-02 Cadastro e verificação de e-mail

Define cenários para execução futura; não representa execução. Status da Spec: `APPROVED` (documental).

## Cenários
- **CT-FT02-01 — Cadastro feliz.** Dado app ativa/aberta; Quando e-mail e senha válidos; Então 202, conta pendente, outbox TPL-01, evento. Ref.: CA-FT02-01
- **CT-FT02-02 — Duplicado idêntico.** Dado e-mail em cada estado (pendente, ativo, desativado, anonimizado→livre); Então resposta byte a byte idêntica (exceto `trace_id`), nada alterado, TPL-05 enviado nos três primeiros; anonimizado cria conta nova. Ref.: CA-FT02-02
- **CT-FT02-03 — Tempo equivalente.** 200 amostras de cada; diferença da média ≤ 15 %. Ref.: CA-FT02-03, CT-RNF-16
- **CT-FT02-04 — Normalização.** Variações de caixa/espaços/Unicode ⇒ duplicado. Ref.: CA-FT02-04
- **CT-FT02-05 — Política de senha (limites).** 11, 12, 128, 129 caracteres; só espaços; lista comum; contendo e-mail. Ref.: CA-FT02-05
- **CT-FT02-06 — Cliente/origem/cadastro fechado.** Ref.: CA-FT02-06
- **CT-FT02-07 — Limites.** 10/h IP ⇒ 429; 3/h e-mail ⇒ 202 sem efeito; `Retry-After`. Ref.: CA-FT02-07
- **CT-FT02-08 — Verificar.** Ref.: CA-FT02-08
- **CT-FT02-09 — Token inválido.** Inexistente, expirado (relógio avançado 24 h+1 s), usado, invalidado, de outra finalidade ⇒ mesmo código. Ref.: CA-FT02-09
- **CT-FT02-10 — Concorrência no token.** 20 requisições simultâneas ⇒ 1 sucesso. Ref.: CA-FT02-10
- **CT-FT02-11 — Reenvio.** Limites 1/60 s, 5/h; invalida anterior; neutralidade. Ref.: CA-FT02-11
- **CT-FT02-12 — Login pendente.** (integração com FT-03) ⇒ `EMAIL_NOT_VERIFIED`, sem tokens. Ref.: CA-FT02-12
- **CT-FT02-13 — Purga.** Relógio +7 dias ⇒ conta removida, e-mail livre. Ref.: CA-FT02-13
- **CT-FT02-14 — Link do e-mail.** URL da app; token no fragmento; cabeçalho `Host` forjado não altera o link. Ref.: CA-FT02-14
- **CT-FT02-15 — SMTP fora do ar.** 202 mantido; retentativas P-40. Ref.: CA-FT02-15
- **CT-FT02-16 — Cadastros simultâneos do mesmo e-mail.** 10 paralelos ⇒ 1 conta, demais “aceito”. Ref.: CA-FT02-02, DM-12
- **CT-FT02-17 — UI.** Fluxo SCR-02→05→03 (MailHog), validações, estados, teclado, `axe` sem violações. Ref.: CA-FT02-01, 08, 09
- **CT-FT02-18 — Logs sem segredo.** Varredura de senha/token sentinela. Ref.: CA-FT02-01, CA-FT02-05; SEC-FT02-05, SEC-31

## Rastreabilidade
Cada CT-FT02-nn referencia o CA indicado; CT-FT02-16…18 cobrem SEC-FT02-01/03/05.
