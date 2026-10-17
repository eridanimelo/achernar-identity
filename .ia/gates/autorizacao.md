---
name: gate-autorizacao
type: gate
description: "GATE-AUT — Gate de Autorização (Specification → Implementation)"
---

# GATE-AUT — Gate de Autorização (Specification → Implementation)

## Objetivo

Garantir que a implementação só inicia com Spec suficiente, escopo autorizado e
tecnologia alvo definida.

Concluir a Spec **não** autoriza implementação automática.

## Momento

Transição `SPECIFICATION → IMPLEMENTATION`, antes de qualquer alteração de código.

## Executor

O Agent de implementação (`backend`, `frontend`, `database`, ...) valida;
o orquestrador confirma a intenção explícita.

## Critérios

- [ ] Spec alvo identificada;
- [ ] Spec aprovada conforme regra do projeto;
- [ ] `business.md` suficiente;
- [ ] contratos necessários definidos quando aplicáveis;
- [ ] persistência definida quando aplicável;
- [ ] frontend definido quando aplicável;
- [ ] segurança analisada quando aplicável;
- [ ] testes/cenários de aceite definidos;
- [ ] dependências bloqueantes atendidas;
- [ ] Wave permite início ou houve decisão humana explícita;
- [ ] tecnologia alvo necessária explicitamente definida;
- [ ] decisões arquiteturais bloqueantes resolvidas;
- [ ] houve intenção explícita de implementar.

## Tecnologia ausente

Se a tecnologia alvo necessária não estiver definida em `config/technology.yaml`:

`REQUIRES_REVIEW`

MUST perguntar ao humano qual tecnologia usar.
MUST NOT selecionar tecnologia automaticamente.

## Evidência

- Spec alvo identificada (ID/slug) e artefatos aplicáveis presentes;
- `status.yaml` indicando aprovação da Spec conforme regra do projeto;
- tecnologia alvo necessária localizada em `config/technology.yaml`;
- registro da intenção explícita de implementar (solicitação do usuário).

## Resultado

- `PASS` — autorizado a implementar o escopo declarado;
- `REQUIRES_REVIEW` — decisão humana, tecnologia ou dependência pendente.

## Falha

1. registrar qual critério não foi atendido (Spec insuficiente, tecnologia
   ausente, dependência bloqueante ou Wave não liberada);
2. MUST NOT iniciar implementação com qualquer critério bloqueante pendente;
3. retornar `REQUIRES_REVIEW` e apontar exatamente o que falta para autorizar.

## Referências

- `rules/specification/spec-structure.md`
- `skills/specification/SKILL.md`
