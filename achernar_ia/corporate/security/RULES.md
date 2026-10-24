---
name: corporate-security-rules
type: rule
description: "Security Rules"
---

# Security Rules

## Regra principal

A segurança deve ser especificada em termos de comportamento, risco e controle necessário.

MUST NOT colocar detalhes tecnológicos em `security.md`.

Exemplos permitidos:

- operação exige usuário autenticado;
- somente perfil Administrador pode excluir;
- usuário somente pode acessar recurso pertencente ao seu escopo;
- entrada deve respeitar tamanho e formato esperado;
- dados sensíveis não devem aparecer em logs;
- arquivo enviado deve ter tipo e tamanho validados.

Exemplos não permitidos na Spec:

- usar Spring Security;
- usar JWT;
- criar `SecurityFilterChain`;
- usar biblioteca X;
- usar annotation Y.

Essas decisões pertencem à implementação e `config/technology.yaml`.

## Autenticação

Quando aplicável, definir:

- se autenticação é necessária;
- contexto de identidade necessário;
- comportamento para acesso não autenticado.

MUST NOT assumir autenticação quando a feature explicitamente não exigir.

## Autorização

Quando aplicável:

- identificar operação protegida;
- identificar papel, perfil ou regra de acesso;
- considerar acesso por recurso e contexto, não apenas por tela;
- MUST NOT depender somente do frontend.

## Entrada

Entradas externas devem ser validadas conforme contexto.

Considerar:

- obrigatoriedade;
- formato;
- tamanho;
- faixa;
- lista permitida;
- referência válida;
- arquivo;
- conteúdo estruturado.

Validação e sanitização não são sinônimos.

## Dados

Quando houver dado sensível:

- reduzir exposição;
- evitar retorno desnecessário;
- evitar logs indevidos;
- definir necessidade de mascaramento/auditoria quando aplicável.

## Erros

Erros não devem expor:

- stack trace;
- segredo;
- estrutura interna desnecessária;
- informação sensível;
- detalhe que facilite exploração.

## Auditoria

Avaliar auditoria quando existir:

- alteração sensível;
- operação administrativa;
- exclusão relevante;
- mudança de permissão;
- acesso crítico;
- requisito regulatório.

## Integrações

Para integrações externas:

- identificar confiança;
- validar entradas/retornos relevantes;
- proteger credenciais;
- tratar falhas e respostas inesperadas.

## Review

Na revisão de implementação, verificar somente riscos aplicáveis ao escopo.

Findings devem conter:

- problema;
- impacto;
- evidência;
- recomendação;
- severidade.

MUST NOT gerar checklist genérico sem relação com a feature.
