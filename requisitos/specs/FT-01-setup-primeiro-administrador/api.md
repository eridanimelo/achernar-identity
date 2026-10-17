# API Spec — FT-01 Setup do primeiro administrador

## 0. Identificação
- ID: FT-01 · Nome: Setup do primeiro administrador · Origem: `NEW` · Status: `APPROVED` (documental)

## 1. Objetivo da API
Expor a consulta do estado do setup e a criação do primeiro administrador. Convenções comuns: `specs/_shared/api-conventions.md`.

## 2. Operações

### API-FT01-01 — Consultar estado do setup
**Objetivo:** informar à interface se deve exibir a tela de setup.
**Método/semântica:** consulta (idempotente, sem efeitos).
**Contrato HTTP:** `GET /setup/status`

#### Entrada
Sem parâmetros. Autenticação: nenhuma.

#### Saída de sucesso (200)
| Campo | Significado | Referência |
|---|---|---|
| `setup_required` | verdadeiro quando não existe administrador ativo | RN-SET-01 |
| `setup_available` | verdadeiro quando `setup_required` e o segredo de implantação está configurado | RN-SET-04 |

#### Erros esperados
| Situação | Resultado esperado | Referência |
|---|---|---|
| Limite de taxa (P-38) | `429 RATE_LIMITED` | RN-SET-02 |

#### Segurança
`security.md` (SEC-FT01-01). Resposta mínima; `Cache-Control: no-store`.

#### Rastreabilidade
CA-FT01-01, CA-FT01-05, CA-FT01-06.

### API-FT01-02 — Criar o primeiro administrador
**Objetivo:** criar a conta administradora inicial.
**Método/semântica:** comando (não idempotente; só um sucesso possível).
**Contrato HTTP:** `POST /setup/admin`

#### Entrada (JSON)
| Campo | Origem | Obrigatório | Restrição funcional | Referência |
|---|---|---|---|---|
| `bootstrap_token` | corpo | sim | string 1–256; comparação em tempo constante | RN-SET-02 |
| `email` | corpo | sim | endereço válido, ≤ 254 | RN-ACC-02 |
| `password` | corpo | sim | política RN-PWD-01 | RN-PWD-01 |
| `locale` | corpo | não | `pt-BR` \| `en` | RN-ACC-08 |

#### Saída de sucesso (201)
| Campo | Significado | Referência |
|---|---|---|
| `account_id` | identificador estável (`sub`) do administrador | RN-ACC-03 |
| `email` | e-mail como informado | RN-SET-03 |

Sem tokens (RN-SET-07). `Cache-Control: no-store`.

#### Erros esperados
| Situação | Resultado esperado | Referência |
|---|---|---|
| Campos inválidos | `400 VALIDATION_ERROR` com `errors[]` | RN-ACC-02 |
| Senha fora da política | `400 PASSWORD_POLICY_VIOLATION` | RN-PWD-01 |
| Segredo incorreto | `403 SETUP_TOKEN_INVALID` | RN-SET-02 |
| Setup já concluído / indisponível | `404 SETUP_NOT_AVAILABLE` | RN-SET-01 |
| E-mail já cadastrado | `409 CONFLICT` | RN-SET-06 |
| Excesso de tentativas | `429 RATE_LIMITED` + `Retry-After` | RN-SET-02 |

#### Segurança
SEC-FT01-02…04 em `security.md`; sem autenticação prévia.

#### Rastreabilidade
CA-FT01-02, 03, 04, 07, 08, 09, 11.

## 3. Filtros, paginação e ordenação
`NOT_APPLICABLE`.

## 4. Integrações
`NOT_APPLICABLE` (nenhum e-mail é enviado no setup; o e-mail do administrador é considerado verificado).

## 5. Pontos a validar
Nenhum.

## 6. Rastreabilidade geral
| Operação | Requisito/Regra/Critério |
|---|---|
| API-FT01-01 | RF-15; RN-SET-01, 04; CA-FT01-01, 05, 06 |
| API-FT01-02 | RF-15; RN-SET-02, 03, 06, 07; CA-FT01-02…04, 07…09, 11 |
