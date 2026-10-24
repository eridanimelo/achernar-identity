---
name: docs-adding-skills
type: docs
description: "Como adicionar uma Skill"
---

# Como adicionar uma Skill

Skill = capacidade de execução reutilizável, no formato
[Agent Skills](https://agentskills.io/specification).

Adicionar uma Skill deve ser localizado na nova capacidade e, quando necessário,
no roteamento.

---

## Quando criar uma Skill

Crie quando houver um procedimento reutilizável com:

- objetivo próprio;
- entradas e saídas definidas;
- processo;
- critérios de parada/validação;
- contexto específico a carregar.

Não crie Skill para agrupar texto curto ou preferência pontual — use Rule ou documentação.
Não crie Skill para um critério de passagem — use `gates/`.

---

## Estrutura

```text
.agents/skills/<nome>/
├── SKILL.md       # obrigatório
├── assets/        # opcional: templates e recursos estáticos
├── references/    # opcional: documentação sob demanda
└── scripts/       # opcional: código executável
```

O nome do diretório é o `name` da Skill:

- minúsculas (`a-z`), números e hífen;
- sem hífen no início/fim, sem hífen duplo;
- igual ao diretório pai.

---

## SKILL.md

```markdown
---
name: <igual ao diretório>
description: <o que faz + quando usar, com palavras-chave de disparo>
---

# <Título>

## Quando usar
## Quando não usar
## Entradas
## Processo
## Saídas
## Referências
```

Regras:

1. `description` deve dizer **o que** faz e **quando** usar (é o gatilho de seleção);
2. manter o `SKILL.md` curto (referência: menos de 500 linhas / ~5.000 tokens);
3. mover detalhe para `references/` e templates para `assets/`;
4. referenciar arquivos por caminho relativo à raiz da Skill, uma camada de profundidade;
5. indicar **quando** carregar cada recurso, não apenas que ele existe;
6. uma Skill = uma unidade coerente de trabalho.

---

## Roteamento

Atualize o roteamento somente se a Skill representar uma nova intenção ou etapa:

- tabela de intenções em `AGENTS.md`;
- `AGENT.md` da intenção, que declara o contrato de leitura;
- `workflows/feature.md`, somente se houver transição nova.

Uma Skill de apoio não cria etapa nova.

---

## Limites entre camadas

Camadas e responsabilidade de cada uma: `docs/architecture.md`.

MUST NOT duplicar conteúdo entre elas.

---

## Checklist

- [ ] `name` igual ao diretório e válido pela especificação;
- [ ] `description` diz o que faz e quando usar;
- [ ] escopo e condições de uso explícitos;
- [ ] processo e saídas definidos;
- [ ] recursos em `assets/`/`references/`, referenciados com condição de carga;
- [ ] `SKILL.md` abaixo do limite de tamanho;
- [ ] sem duplicação com Rules/Gates/Agents;
- [ ] roteamento atualizado somente se necessário;
- [ ] Markdown e exemplos revisados.
