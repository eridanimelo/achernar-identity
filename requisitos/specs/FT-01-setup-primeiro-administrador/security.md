# Security Spec — FT-01 Setup do primeiro administrador

## 0. Identificação
- ID: FT-01 · Origem: `NEW` · Status: `APPROVED` (documental)

## 1. Aplicabilidade
Operação pública porém protegida por segredo de implantação; é o ponto de maior privilégio do produto. Riscos: tomada de controle por quem alcançar o endpoint antes do operador, força bruta do segredo, vazamento do segredo, criação repetida.

## 2. Autenticação
Sem autenticação de usuário. A autorização para criar vem da posse do segredo de implantação (comparação em tempo constante, SEC-33). Nenhuma sessão é criada.

## 3. Autorização
| Operação/Capacidade | Regra de acesso | Referência |
|---|---|---|
| Consultar estado | pública; resposta mínima | RN-SET-02 |
| Criar administrador | segredo correto **e** ausência de administrador ativo | RN-SET-01/02 |

## 4. Dados sensíveis
| Dado | Risco | Controle requerido | Referência |
|---|---|---|---|
| Segredo de implantação | vazamento/log | nunca em log, evento, resposta ou banco; fornecido por ambiente/segredo do orquestrador; recomendado remover após o setup | SEC-31, SEC-24 |
| Senha | exposição | resumo seguro; nunca registrada | SEC-02 |

## 5. Validação de entrada
| Entrada | Validação necessária | Motivo/Referência |
|---|---|---|
| segredo | 1–256 caracteres; comparação em tempo constante | SEC-33 |
| e-mail | formato e normalização | RN-ACC-02 |
| senha | política | RN-PWD-01 |

## 6. Auditoria
`BOOTSTRAP_ADMIN_CREATED` (alerta) e `SETUP_ATTEMPT_FAILED` (alerta); sem valores informados.

## 7. Integrações
`NOT_APPLICABLE`.

## 8. Riscos
| ID | Risco | Impacto | Controle esperado |
|---|---|---|---|
| RSK-FT01-01 | Atacante cria o admin antes do operador | crítico | segredo obrigatório e imprevisível; implantação deve criar o admin logo após subir; limite de tentativas |
| RSK-FT01-02 | Força bruta do segredo | alto | ≥ 32 caracteres aleatórios; limite 5/15 min/IP; evento de alerta |
| RSK-FT01-03 | Segredo permanece no ambiente | médio | documentar remoção; endpoint já fechado após admin existir |
| RSK-FT01-04 | Recuperação (setup reaberto) abusada | alto | reabre só sem administrador **ativo** (exige acesso privilegiado ao banco) |
| RSK-FT01-05 | Corrida cria dois admins | médio | serialização transacional |

Controles referenciados: SEC-31, SEC-32, SEC-33, SEC-05 (limites), SEC-35.

## 9. Pontos a validar
Nenhum.

## 10. Rastreabilidade
| Controle | Requisito/Regra/Critério |
|---|---|
| SEC-FT01-01 (resposta mínima) | CA-FT01-01 |
| SEC-FT01-02 (segredo em tempo constante) | CA-FT01-03 |
| SEC-FT01-03 (limite de tentativas) | CA-FT01-04 |
| SEC-FT01-04 (fechamento após uso/serialização) | CA-FT01-05, CA-FT01-11 |
