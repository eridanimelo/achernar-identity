# Database Spec — FT-14 Interface base, tema e sessão no navegador
`NEW` · `APPROVED` (documental).

`NOT_APPLICABLE` no servidor: esta feature não cria estruturas de banco.

### DATA-FT14-01 — Preferências locais no navegador
Tema (`system|light|dark`) e idioma (`pt-BR|en`) guardados no armazenamento local do navegador, por origem, sem dados pessoais; leitura/escrita tolerantes a falha. Não são enviadas ao servidor (o idioma de e-mails é o da conta — FT-02/FT-05).
### DATA-FT14-02 — Estado de sessão em memória
Access token, instante de expiração, identificador da sessão e perfil da conta em memória da aba; nunca persistidos (SEC-12).

Rastreabilidade: DATA-FT14-01 → CA-FT14-09…11 · DATA-FT14-02 → CA-FT14-04…06.
