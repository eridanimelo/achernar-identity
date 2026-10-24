---
name: security
type: skill
description: "Security Skill"
---

# Security Skill

## Objetivo

Analisar segurança da feature e produzir/revisar `security.md` sem acoplamento tecnológico.

## Entradas

Consultar somente o necessário:

- `business.md`;
- `api.md`;
- `frontend.md`;
- `database.md`;
- contexto da feature;
- código, somente quando a solicitação for revisão de implementação.

## Processo de especificação

1. compreender objetivo e atores;
2. identificar superfícies de entrada;
3. identificar operações sensíveis;
4. determinar necessidade de autenticação;
5. determinar regras de autorização;
6. identificar dados sensíveis;
7. definir requisitos de validação;
8. avaliar auditoria;
9. avaliar riscos de integração;
10. registrar riscos e decisões pendentes;
11. produzir/revisar `security.md` usando `templates/security.md`.

## Estrutura mínima de `security.md`

```text
# Security Spec

## Aplicabilidade
## Autenticação
## Autorização
## Dados sensíveis
## Validação de entrada
## Auditoria
## Integrações
## Riscos
## Pontos a validar
## Rastreabilidade
```

Se um item não se aplicar:

`NOT_APPLICABLE`

Se uma decisão necessária estiver indefinida:

`REQUIRES_REVIEW`

## Processo de revisão de implementação

1. carregar a Spec;
2. localizar código relacionado;
3. verificar controles definidos;
4. identificar violações objetivas;
5. registrar findings;
6. classificar resultado.

## Restrições

MUST NOT:
- colocar tecnologia na Spec;
- inventar perfil ou permissão;
- alterar requisito para facilitar segurança;
- criar finding sem evidência;
- alterar outro pacote corporativo.
