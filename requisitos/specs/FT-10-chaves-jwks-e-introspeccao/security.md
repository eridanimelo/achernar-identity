# Security Spec — FT-10 Chaves, JWKS e introspecção
`NEW` · `APPROVED` (documental).

| ID | Controle | Ref. |
|---|---|---|
| SEC-FT10-01 | Somente RS256; `alg`/`typ`/`kid` verificados; RSA 3072 | SEC-25, D-04 |
| SEC-FT10-02 | Chave privada cifrada (AES-256-GCM) e jamais exposta | SEC-24 |
| SEC-FT10-03 | JWKS apenas com parte pública, cacheável, sem credenciais | RN-TOK-13 |
| SEC-FT10-04 | Janela de rotação suficiente para validade do token + skew + cache | P-12 |
| SEC-FT10-05 | Remoção emergencial com revogação opcional de sessões | RN-TOK-12 |
| SEC-FT10-06 | Introspecção autenticada por credencial da API; respostas sem motivo; limites | SEC-29, SEC-34 |
| SEC-FT10-07 | Sem confiança em claims de perfil para administração | SEC-27 |
| SEC-FT10-08 | Rotação manual e remoção exigem ADMIN e reautenticação | RN-PWD-08 |

Riscos: RSK-FT10-01 comprometimento da chave (remoção emergencial); RSK-FT10-02 chave-mestra vazada (separação de segredos; rotação documentada em `deployment-operations.md`); RSK-FT10-03 APIs com cache antigo do JWKS (max-age curto; refetch em `kid` desconhecido); RSK-FT10-04 introspecção como oráculo (autenticação + limite + sem motivo).
Rastreabilidade: SEC-FT10-01 → CA-FT10-02 · -02 → CA-FT10-10 · -03 → CA-FT10-01 · -04 → CA-FT10-07, 08 · -05 → CA-FT10-12 · -06 → CA-FT10-15…17 · -07 → CA-FT10-22 · -08 → CA-FT10-11.
