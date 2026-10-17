---
name: rule-database-database
type: rule
description: "Banco de Dados"
---

# Banco de Dados

O modelo de dados deve representar corretamente o domínio.

## Normalização

O banco deve ser normalizado por padrão.

Deve-se evitar:
- duplicação desnecessária de dados;
- atributos multivalorados indevidos;
- dependências inconsistentes;
- armazenamento redundante sem justificativa.

Desnormalização MAY ser utilizada quando houver motivo técnico comprovado e documentado.

## Integridade

Utilizar mecanismos de integridade quando aplicáveis:
- chave primária;
- chave estrangeira;
- unicidade;
- obrigatoriedade;
- constraints.

Integridade crítica MUST NOT depender exclusivamente da interface.

## Índices

Índices devem ser criados com base em necessidade real de consulta, relacionamento ou restrição.

MUST NOT criar índices indiscriminadamente.

## Relacionamentos

Relacionamentos devem refletir o domínio e evitar acoplamento desnecessário entre entidades.

## Mudanças de schema

Mudanças estruturais devem ser versionadas pela ferramenta definida em `config/technology.yaml`.

Mudanças destrutivas exigem avaliação de impacto.
