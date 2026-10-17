# Database Spec — FT-01 Setup do primeiro administrador

## 0. Identificação
- ID: FT-01 · Nome: Setup do primeiro administrador · Origem: `NEW` · Status: `APPROVED` (documental)

## 1. Objetivo de persistência
O setup **não cria estrutura própria**: grava uma conta (estrutura definida em FT-02), um evento (FT-12) e, na primeira inicialização, a aplicação da interface (FT-09). O estado “setup necessário” é **derivado** da existência de administrador ativo.

## 2. Estruturas lógicas

### DATA-FT01-01 — Estado do setup (derivado)
**Objetivo:** decidir disponibilidade do setup.
| Campo/Dado | Significado | Obrigatório | Restrição funcional | Referência |
|---|---|---|---|---|
| existência de administrador ativo | resultado de consulta sobre contas | — | calculado, não armazenado | RN-SET-01 |
| segredo de implantação configurado | informado pelo ambiente | — | não armazenado no banco | RN-SET-04 |

### Estruturas utilizadas (definidas em outras Specs)
| Estrutura | Definida em | Uso no setup |
|---|---|---|
| Conta (DATA-FT02-01) | FT-02 | cria 1 conta (perfil administrador, ativa, e-mail verificado) |
| Credencial de senha (DATA-FT02-02) | FT-02 | grava o resumo seguro da senha |
| Evento de segurança (DATA-FT12-01) | FT-12 | `BOOTSTRAP_ADMIN_CREATED`, `SETUP_ATTEMPT_FAILED` |
| Aplicação consumidora (DATA-FT09-01) | FT-09 | semeadura da aplicação da interface |
| Contador de limite (DATA-FT03-03) | FT-03 | limite P-38 |

## 3. Relacionamentos
| Origem | Relação | Destino | Cardinalidade | Regra/Justificativa |
|---|---|---|---|---|
| Conta | possui | Credencial de senha | 1 : 0..1 | RN-ACC-04 |

## 4. Integridade
| ID | Regra de integridade | Referência |
|---|---|---|
| INT-FT01-01 | A criação do administrador e do evento ocorre de forma atômica | RN-AUD-01 |
| INT-FT01-02 | Duas criações simultâneas não resultam em dois administradores pelo setup (a verificação “sem administrador ativo” e a criação são serializadas) | RN-SET-01, RNF-20 |

## 5. Unicidade
| Dado/Conjunto | Regra | Referência |
|---|---|---|
| e-mail normalizado | único na instalação | RN-ACC-02 |

## 6. Histórico e auditoria
Evento `BOOTSTRAP_ADMIN_CREATED` (sucesso) e `SETUP_ATTEMPT_FAILED` (falha, sem o valor informado).

## 7. Dados derivados ou transitórios
Estado do setup (derivado). Nenhum dado transitório.

## 8. LEGACY — correspondências conhecidas
`NOT_APPLICABLE`.

## 9. Pontos a validar
Nenhum.

## 10. Rastreabilidade
| Estrutura/Regra | Requisito/Regra/Critério |
|---|---|
| DATA-FT01-01 | RN-SET-01, RN-SET-04; CA-FT01-01, 05, 06, 08 |
| INT-FT01-02 | CA-FT01-11 |
