---
name: rule-architecture-layers
type: rule
description: "Camadas e Dependências"
---

# Camadas e Dependências

A aplicação deve manter separação clara entre responsabilidades.

Quando o projeto utilizar arquitetura MVC ou equivalente, considerar conceitualmente:
- entrada/interface;
- aplicação/serviço;
- domínio;
- persistência/infraestrutura.

## Controller / Entrada

A camada de entrada deve:
- receber a requisição;
- validar dados estruturais;
- delegar execução;
- retornar resposta.

MUST NOT conter regra de negócio relevante.
MUST NOT acessar persistência diretamente.

## Service

Services coordenam casos de uso e regras de aplicação.

Um Service deve acessar diretamente apenas o Repository pertencente ao seu próprio domínio/agregado.

Exemplo permitido:

`ClienteService` → `ClienteRepository`

MUST NOT:

`ClienteService` → `PedidoRepository`

Quando informação de outro domínio for necessária, utilizar o Service responsável por aquele domínio.

Exemplo:

`ClienteService` → `PedidoService` → `PedidoRepository`

Isso evita acoplamento indevido entre domínio e persistência.

## Repository

Repository deve:
- representar acesso à persistência;
- permanecer associado ao domínio/agregado responsável;
- não conter regras de negócio.

## Dependências

MUST NOT criar dependências circulares.

MUST NOT quebrar separação de camadas apenas para reduzir quantidade de código.
