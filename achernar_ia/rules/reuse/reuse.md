---
name: rule-reuse-reuse
type: rule
description: "Reuso"
---

# Reuso

Antes de criar novo código, verificar se já existe implementação semanticamente compatível.

O Reuse Scan faz parte do `GATE-ANL` e MUST ocorrer antes de criar novo componente, service, utilitário, validator, DTO/model ou outra abstração relevante.

## Especialistas

Para implementação:

- Backend MUST usar o contrato `subagents/reuse/backend/AGENT.md`;
- Frontend MUST usar o contrato `subagents/reuse/frontend/AGENT.md`.

Quando o ambiente suportar subagentes, SHOULD delegar o scan ao especialista correspondente em contexto isolado.
Quando não suportar, MUST executar o mesmo contrato de forma temporária e incorporar à sessão principal somente a saída estruturada.

O Agent de implementação continua responsável pelo `GATE-ANL`; o Reuse Scanner fornece a evidência especializada de reuso.

## Localização

Antes de explorar o código-fonte diretamente, consultar `.agents/skills/reuse-index/SKILL.md`: gera e mantém um índice local (script, sem LLM, sem rede) dos símbolos declarados em backend/frontend, para reduzir a necessidade de abrir arquivos inteiros só para descobrir se algo já existe.

O índice é heurístico e só apoia a localização — MUST sempre abrir e ler o arquivo real do candidato antes de decidir reutilizar, compor ou estender. Quando o índice não cobrir a tecnologia/escopo ou não trouxer nada plausível, localizar por busca de símbolo, grep ou leitura seletiva do código-fonte.

## Decisões permitidas

- `REUSE`: usar o artefato existente sem alteração;
- `COMPOSE`: montar a solução combinando artefatos existentes;
- `EXTEND`: evoluir artefato existente após avaliar impacto nos consumidores;
- `CREATE`: criar novo artefato somente após busca suficiente e justificativa;
- `MIXED`: necessidades diferentes da mesma unidade receberam decisões diferentes.

MUST NOT reutilizar código apenas porque possui estrutura ou nome semelhante.
MUST NOT concluir `CREATE` apenas porque a busca pelo nome exato não retornou resultado.

## Busca semântica

Pesquisar conceitos e responsabilidades equivalentes quando aplicável.

Frontend: componentes, services, models, validators, pipes, directives, formatters, sanitizers, padrões de UI.

Backend: services, repositories, entities, DTOs, validators, sanitizers, normalizers, clients, exceptions, helpers/utilitários.

## Alteração de código compartilhado

Antes de modificar artefato compartilhado, MUST avaliar impacto nos consumidores existentes.

## Quando não escanear

Não há benefício em delegar quando a alteração já aponta explicitamente para
um artefato conhecido e não há decisão de reuso a investigar (ex.: corrigir
o texto de um label em um arquivo conhecido). Para criação de novo
componente, service, utilitário, validator, DTO, model ou outra abstração
relevante, o Reuse Scan MUST ocorrer.

## Extração

Criar abstração compartilhada quando:

- existir responsabilidade realmente comum;
- houver benefício claro de manutenção;
- o acoplamento resultante permanecer adequado.

MUST NOT criar abstrações especulativas.

## Reuso entre camadas

Backend e frontend executam scans independentes. Uma responsabilidade pode existir em ambas as camadas, mas implementação de uma camada MUST NOT ser tratada como código reutilizável pela outra.

Exemplo: um sanitizador Java pode ser reutilizado por vários consumidores backend. Se o frontend também precisar de normalização, o Frontend Reuse Scanner deve localizar ou justificar uma implementação frontend própria.

## Manual

Fluxo, exemplos e formato de uso: `docs/reuse-scan.md`.
