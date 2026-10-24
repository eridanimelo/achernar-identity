---
name: docs-reuse-scan
type: docs
description: "Manual — Reuse Scan com Subagentes Frontend e Backend"
---

# Manual — Reuse Scan com Subagentes Frontend e Backend

## Objetivo

O Reuse Scan existe para impedir que a IA crie uma segunda solução quando o projeto já possui código que pode ser reutilizado, composto ou estendido.

A regra central é:

```text
Antes de CREATE, provar que REUSE / COMPOSE / EXTEND não atende.
```

O framework possui dois especialistas independentes:

- `subagents/reuse/backend/AGENT.md` — Reuse Scan de backend;
- `subagents/reuse/frontend/AGENT.md` — Reuse Scan de frontend.

Eles são subagentes especializados quando o harness suporta delegação. O Agent principal continua responsável pela implementação; o Reuse Scanner apenas investiga e devolve uma decisão estruturada.

---

## Por que separar Frontend e Backend

Frontend e backend possuem artefatos, contratos e critérios de compatibilidade diferentes.

No frontend, o scanner entende reuso em termos de componentes, inputs/outputs, formulários, services, models, validators, pipes, directives, estados de UI e acessibilidade.

No backend, o scanner entende services, repositories, entities, DTOs, validators, sanitizers, clients, exceptions, helpers e regras equivalentes.

Separar os scanners reduz contexto e evita que uma investigação frontend carregue backend, ou vice-versa.

---

## Fluxo geral

```text
Spec aprovada
     │
     ↓
Backend Agent ou Frontend Agent
     │
     ├── entende a necessidade
     │
     ├── GATE-ANL
     │      │
     │      └── delega Reuse Scan
     │              │
     │              ├── Reuse Index (rápido, local, sem LLM)
     │              ├── busca por símbolo/texto
     │              ├── abre somente candidatos plausíveis
     │              ├── compara semântica/contrato
     │              └── REUSE / COMPOSE / EXTEND / CREATE
     │
     ←──── resultado estruturado pequeno
     │
     ├── planeja respeitando a decisão
     ├── implementa
     ├── testa
     └── GATE-QLT
```

O subagente não precisa devolver todos os arquivos lidos. Ele devolve somente candidatos relevantes, decisão, justificativa, impacto e orientação.

---

## As quatro decisões

### REUSE

O artefato existente atende sem precisar ser alterado.

Exemplo: usar um `EnderecoComponent` existente em outra tela.

### COMPOSE

A solução deve ser montada usando artefatos existentes, sem fundi-los nem duplicá-los.

Exemplo: uma tela usa `EnderecoComponent` + `TelefoneComponent` para formar um cadastro maior.

### EXTEND

Existe um artefato correto, mas ele precisa de uma evolução compatível. Antes de alterar código compartilhado, o scanner deve avaliar consumidores e risco de regressão.

Exemplo: `EnderecoComponent` atende quase tudo, mas precisa aceitar um novo input opcional que não quebra os consumidores existentes.

### CREATE

Nenhum candidato semanticamente compatível atende. A criação nova precisa ter justificativa baseada na busca executada.

`CREATE` não é a decisão padrão; é a decisão depois de eliminar candidatos reais.

---

# Reuse Scan Frontend

## Exemplo: Endereço reutilizável

Imagine que a primeira feature seja Cadastro de Cliente e a aplicação ainda não possua componente de endereço.

O scanner pesquisa e não encontra equivalente. A decisão pode ser:

```yaml
reuse_scan:
  layer: frontend
  decision: CREATE
  candidates: []
  create:
    - artifact: EnderecoComponent
      reason: "Não existe componente/formulário de endereço compatível no frontend."
  guidance:
    - "Criar componente reutilizável porque endereço é uma responsabilidade coesa e há uso compartilhável justificado pelo produto."
```

O Frontend Agent cria, por exemplo:

```text
shared/
└── endereco/
    ├── endereco.component.ts
    ├── endereco.component.html
    └── endereco.component.spec.ts
```

Depois surge Cadastro de Fornecedor.

Antes de escrever novamente CEP, logradouro, número, complemento, bairro, cidade e UF, o Frontend Agent chama o Frontend Reuse Scanner.

O scanner encontra `EnderecoComponent`, lê seu contrato e devolve:

```yaml
reuse_scan:
  layer: frontend
  decision: REUSE
  candidates:
    - path: src/app/shared/endereco/endereco.component.ts
      symbol: EnderecoComponent
      decision: REUSE
      reason: "O componente já representa o formulário de endereço exigido pela nova tela."
      impact: "Somente novo consumidor."
  create: []
  guidance:
    - "Reutilizar EnderecoComponent na tela de Fornecedor."
    - "Não recriar campos/validações de endereço no formulário de Fornecedor."
```

O resultado arquitetural esperado é:

```text
ClienteComponent ──────┐
                       ├──> EnderecoComponent
FornecedorComponent ───┤
                       │
EmpresaComponent ───────┘
```

Uma correção futura de validação de CEP fica centralizada no componente compartilhado, desde que essa responsabilidade pertença ao componente.

## O que o Frontend Scanner deve verificar

Antes de reutilizar um componente, verificar pelo menos quando aplicável:

- responsabilidade semântica;
- inputs e outputs;
- estrutura do formulário;
- validators;
- estados disabled/readonly/loading/error;
- integração com API quando houver;
- acessibilidade;
- compatibilidade com o design system;
- consumidores existentes quando houver alteração compartilhada.

Não transformar toda repetição visual em componente compartilhado. Reuso precisa representar uma responsabilidade real.

---

# Reuse Scan Backend

## Exemplo: Sanitizador de String

Imagine que uma feature anterior tenha criado:

```text
StringSanitizer
```

com a responsabilidade de normalizar entrada textual conforme o padrão do projeto.

Uma nova feature precisa sanitizar o nome de um fornecedor. O Backend Agent não deve criar automaticamente:

```text
FornecedorStringUtils
TextCleaner
InputSanitizer
```

Primeiro ele delega ao Backend Reuse Scanner.

O scanner procura por conceitos equivalentes, encontra `StringSanitizer`, abre o código real e compara o contrato.

Se for compatível:

```yaml
reuse_scan:
  layer: backend
  decision: REUSE
  candidates:
    - path: src/main/java/.../StringSanitizer.java
      symbol: StringSanitizer
      decision: REUSE
      reason: "O utilitário existente já executa a sanitização exigida."
      impact: "Somente novo consumidor."
  create: []
  guidance:
    - "Reutilizar StringSanitizer."
    - "Não criar novo utilitário de sanitização."
```

O resultado esperado é:

```text
ClienteService ────────┐
                       ├──> StringSanitizer
FornecedorService ─────┤
                       │
UsuarioService ─────────┘
```

Se a nova regra exigir comportamento adicional, o scanner decide se é seguro `EXTEND` ou se a responsabilidade é diferente e deve ser `CREATE`.

---

## Reuso entre Backend e Frontend

Backend e frontend fazem Reuse Scan separadamente.

Uma necessidade conceitual pode existir nos dois lados — por exemplo, normalização de texto — mas **código Java do backend não é reutilizado diretamente pelo frontend TypeScript**, nem o contrário.

O correto é:

```text
necessidade: sanitização
        │
        ├── Backend Reuse Scanner
        │      └── procura implementação backend compatível
        │
        └── Frontend Reuse Scanner
               └── procura implementação frontend compatível
```

Se existir `StringSanitizer` no backend e a mesma responsabilidade também for necessária no frontend, o Frontend Scanner deve procurar um equivalente frontend. Se não houver, pode justificar `CREATE` no frontend. A regra de negócio/contrato pode ser equivalente; a implementação é própria de cada camada.

Também é importante não confiar no frontend para segurança: sanitização/validação necessária para proteger o backend deve continuar existindo no backend, mesmo que haja tratamento equivalente na interface.

---

## Reuse Index x Reuse Scanner

São coisas diferentes e complementares.

```text
Reuse Index
   ↓
"onde pode existir algo parecido?"
   ↓
lista de candidatos
   ↓
Reuse Scanner
   ↓
"este candidato realmente atende?"
   ↓
REUSE / COMPOSE / EXTEND / CREATE
```

O `.agents/skills/reuse-index` é local, determinístico, sem rede e sem LLM. Ele reduz custo de localização.

O Reuse Scanner lê apenas candidatos plausíveis e toma a decisão semântica.

---

## Como o Agent principal deve chamar o subagente

### Backend

Entrada mínima sugerida:

```text
Execute o Backend Reuse Scan para esta necessidade:
- Spec: UC-XXX
- necessidade: sanitizar e normalizar o nome antes da persistência
- artefatos esperados: validator/helper/service existente

Retorne somente o YAML definido em subagents/reuse/backend/AGENT.md.
```

### Frontend

```text
Execute o Frontend Reuse Scan para esta necessidade:
- Spec: UC-YYY
- necessidade: capturar e validar endereço no cadastro
- elementos esperados: componente/formulário/validator existente

Retorne somente o YAML definido em subagents/reuse/frontend/AGENT.md.
```

O Agent principal recebe o resultado e continua a implementação. Ele não precisa receber o contexto bruto usado na investigação.

---

## Quando NÃO chamar um subagente

Não há benefício em delegar quando a alteração já aponta explicitamente para um artefato conhecido e não há decisão de reuso a investigar, por exemplo:

```text
"Corrija o texto do label X em Arquivo Y."
```

Para criação de novo componente, service, utilitário, validator, DTO, model ou abstração, o Reuse Scan deve ocorrer.

---

## Relação com os Gates

O Reuse Scan faz parte do `GATE-ANL`.

O Gate só passa quando existe evidência suficiente de que:

1. o código relevante foi localizado;
2. candidatos plausíveis foram avaliados;
3. a decisão foi registrada;
4. `CREATE` foi justificado quando necessário;
5. impacto foi avaliado para alterações compartilhadas.

Sem isso, retornar `REQUIRES_REVIEW` em vez de criar estrutura duplicada.

---

## Regra prática

```text
Localizar barato.
Ler pouco.
Comparar semanticamente.
Reutilizar quando fizer sentido.
Estender com análise de impacto.
Criar somente com justificativa.
```
