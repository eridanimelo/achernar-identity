---
name: rule-backend-java-backend
type: rule
description: "Padrões obrigatórios de backend Java"
---

# Backend Java

Fonte principal dos padrões obrigatórios de implementação Java de backend.
Agents, Skills e templates MUST referenciar este arquivo; MUST NOT duplicar
seu conteúdo.

Aplica-se quando `config/technology.yaml` indicar backend Java/Spring.
`Pessoa` nos exemplos é apenas nome didático: adaptar nomes, campos, DTOs e
consultas aos requisitos aprovados de cada funcionalidade. MUST NOT gerar
CRUDs, campos ou relacionamentos sem necessidade funcional.

Precedência preservada: decisão humana > Spec aprovada > `config/` >
`corporate/` > esta Rule > Skill > código existente. Conflito com contrato
existente MUST ser registrado como `REQUIRES_REVIEW`.

## 1. Estrutura

- Seguir o asset `skills/backend/assets/java-backend.md` (forma); a Spec é a
  fonte de comportamento.
- Para funcionalidades com persistência: Entity, DTOs, interface Service,
  implementação ServiceImpl e Repository.
- Controller delega ao Service; MUST NOT acessar Repository diretamente.
- Regras de negócio na camada de serviço (`rules/architecture/layers.md`).

## 2. Entidades e relacionamentos

- MUST NOT expor entidades JPA nos contratos da API.
- Configurar todo relacionamento com `fetch = FetchType.LAZY` explícito,
  quando o tipo permitir.
- MUST NOT criar mapeamentos bidirecionais.
- Carregar dados necessários de forma explícita (projeção ou fetch
  explícito), evitando N+1 e acesso a lazy após o encerramento do contexto.
- MUST NOT resolver carregamento mudando para EAGER.
- MUST NOT usar `@Data` em entidades nem `toString`/`equals`/`hashCode` que
  percorram relacionamentos. `open-in-view` MUST ser `false`.

## 3. DTOs

- Usar DTOs nos contratos de entrada e saída que representem dados do domínio.
- Separar DTOs de entrada e saída quando campos ou validações diferirem.
- Retornar somente campos exigidos pelo caso de uso; MUST NOT reproduzir a
  entidade inteira automaticamente.
- MUST NOT incluir entidades JPA em DTOs nem serializar grafos completos.
- MUST NOT retornar senhas, hashes, segredos ou campos sensíveis.

## 4. Conversão

- Mapeamento explícito em código Java.
- MUST NOT usar MapStruct, ModelMapper, BeanUtils ou cópia automática.
- Extrair métodos ou classes de conversão manual reutilizáveis somente
  quando houver repetição real.
- No mapeamento de entrada, atribuir somente campos que o caso de uso
  permita alterar.

## 5. Repositories e consultas

- Declarar explicitamente cada consulta usada, com `@Query` em JPQL ou SQL
  nativo; sobrescrever e declarar a consulta quando aplicável.
- MUST NOT depender de métodos derivados por nome ou herdados (`findById`,
  `findAll`) para as operações da funcionalidade esperando semântica
  específica; o `save` herdado é permitido para escrita dentro da transação
  do serviço.
- JPQL usa o nome da entidade (ex.: `SELECT p FROM Pessoa p`); SQL nativo
  usa nomes reais de tabelas e colunas.
- Usar parâmetros vinculados (`@Param`); MUST NOT concatenar entrada do
  usuário no SQL.
- Selecionar somente dados necessários; usar projeções (ex.: construtor de
  DTO) quando o caso exigir parte dos campos.
- Aplicar filtros, paginação e ordenação conforme o caso de uso.
- Remover consultas que a funcionalidade não usa.

## 6. Transações

- Consultas de serviço que dispensem transação:
  `@Transactional(propagation = Propagation.NOT_SUPPORTED)`.
- MUST NOT usar `@Transactional(readOnly = true)` como padrão para essas
  consultas.
- `NOT_SUPPORTED` suspende transação existente; MUST NOT usar esse método
  dentro de operação que precise enxergar escritas não confirmadas.
- Verificar a configuração transacional dos métodos de Repository chamados,
  para não abrir transação contrariando a decisão.
- Consultas que exijam consistência transacional, locks ou unidade de
  trabalho: registrar justificativa e aplicar a transação adequada.
- Escritas com limites transacionais definidos na camada de serviço
  (`@Transactional`).
- MUST NOT depender de autochamada entre métodos do mesmo bean para ativar
  `@Transactional`.

## 7. Exceptions

- Usar exceptions específicas de domínio/aplicação.
- Centralizar tradução para HTTP em tratamento global (`@RestControllerAdvice`,
  conforme asset); padronizar respostas sem expor stack traces ou detalhes
  internos.
- MUST NOT capturar exceptions para ignorá-las ou devolver sucesso falso.
- Preservar a causa original ao traduzir, quando aplicável.
- MUST NOT converter toda exception para 400 nem toda violação de banco em
  conflito de negócio. Erros de autenticação/autorização seguem a cadeia do
  Spring Security.

## 8. Métodos reutilizáveis

- Extrair métodos reutilizáveis quando houver lógica repetida ou
  responsabilidade clara; nomes coesos que expressem finalidade.
- MUST NOT criar utilitários genéricos nem abstrações sem necessidade
  demonstrada (`rules/quality/quality.md`, `rules/architecture/principles.md`).

## 9. Schema

- Liquibase cria e modifica o schema; Hibernate apenas valida
  (`ddl-auto: validate`). MUST NOT alterar changesets já aplicados.
