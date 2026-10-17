---
name: corporate-testing-rules
type: rule
description: "Test Specification Rules"
---

# Test Specification Rules

## Regra principal

`tests.md` é uma **especificação de testes**, não um relatório de execução.

Na fase de Specification:

- definir cenários;
- definir resultados esperados;
- garantir cobertura funcional;
- manter rastreabilidade.

MUST NOT:

- executar testes;
- registrar PASS/FAIL de execução;
- afirmar cobertura executada;
- definir framework/ferramenta;
- produzir evidência fictícia.

## Origem dos cenários

Os cenários devem ser derivados somente de artefatos aplicáveis e aprovados ou
suficientemente definidos:

- `business.md`;
- `api.md`;
- `database.md`, quando houver comportamento verificável relacionado a dados;
- `frontend.md`;
- `security.md`.

## Estrutura dos cenários

Cada cenário deve possuir:

- identificador;
- objetivo;
- pré-condição quando necessária;
- ação;
- resultado esperado;
- rastreabilidade.

Preferir, quando adequado:

- **Dado**
- **Quando**
- **Então**

## Cobertura

Considerar, quando aplicável:

- fluxo principal;
- fluxos alternativos;
- validações;
- erros;
- limites;
- autorização;
- comportamento da interface;
- contratos de API;
- regras críticas de dados.

## Estados

`DEFINED`:
cenário especificado.

`REQUIRES_REVIEW`:
comportamento necessário para especificar o cenário ainda está indefinido.

`NOT_APPLICABLE`:
categoria de teste não se aplica.

MUST NOT usar `PASS` ou `FAIL` para representar execução durante Specification.

## Execução

PASS/FAIL somente pode existir depois que houver implementação e execução real
ou evidência objetiva de verificação.
