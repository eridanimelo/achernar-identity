# ADR-003 — Repositories como interfaces Spring Data JPA

- **Status:** aceito
- **Data:** 2026-10-25
- **Escopo:** `users/repository`, `profiles/repository`, `auth/repository`
- **Feature:** UC-001 Identidade
- **Substitui:** IMP-01 (`jpa_hibernate_domain: false`) apenas para os
  repositories; `Audit`, `MailQueue` e `MailDelivery` continuam em JDBC

## Contexto

O backend usava classes de repository concretas com `NamedParameterJdbcTemplate`
e SQL montado em Java. A decisão humana solicitou que os repositories passem a
ser interfaces do Spring Data, mantendo o padrão de mercado
`interface Repository extends JpaRepository` e os contratos usados pelos
services.

Restrições preservadas:

- nenhum path, verbo, status HTTP, corpo JSON, mensagem, auditoria ou
  fronteira transacional poderia mudar;
- `config/technology.yaml` declarava `jpa_hibernate_domain: false` e a Spec
  registrava "sem ORM"; a decisão humana altera esse ponto para os
  repositories;
- o schema é criado e validado por Liquibase (`ddl-auto: validate`);
- `IdentityIT` e `ProfilesIT` são a rede de segurança comportamental.

## Decisão

### 1. Stack

- adicionar `spring-boot-starter-data-jpa` (Hibernate como provedor);
- `spring.jpa.open-in-view: false`;
- `spring.jpa.hibernate.ddl-auto: validate`;
- `config/technology.yaml` → `jpa_hibernate_domain: true`.

### 2. Repositories

| Feature | Interface | Entidade JPA |
| --- | --- | --- |
| users | `UserRepository extends JpaRepository<UserJpaEntity, UUID>` | `UserJpaEntity` (`app_user`) |
| profiles | `ProfileRepository extends JpaRepository<ProfileJpaEntity, UUID>` | `ProfileJpaEntity` (`access_profile`) |
| auth | `ActionRepository extends JpaRepository<ActionJpaEntity, UUID>` | `ActionJpaEntity` (`email_action`) |

- as entidades JPA são internas ao repository e não vazam para service, DTO ou
  API;
- os services continuam recebendo/retornando os records existentes
  (`UserEntity`, `UserView`, `ProfileView`, `Action`), mapeados nos métodos
  `default` das interfaces;
- toda consulta é declarada com `@Query`; não há dependência de métodos
  derivados por nome nem de `findById`/`findAll` herdados;
- escritas simples usam JPQL com `@Modifying(clearAutomatically = true)`;
- SQL específico do PostgreSQL permanece explícito em `@Query(nativeQuery =
  true)`: `INSERT ... ON CONFLICT DO NOTHING`, contagem de administradores,
  revogação por perfil e as leituras agregadas de usuário;
- leituras de usuário usam projeção Spring Data (`UserRepository.UserRow`) com
  `string_agg` das autoridades, preservando `profiles` (ativas),
  `assignedProfiles` (todas) e o `role` derivado;
- `ProfileJpaEntity.userCount` usa `@Formula` com a mesma subconsulta de
  contagem de vínculos do SQL original;
- o advisory lock transacional continua existindo via fragmento
  `UserAdvisoryLock`/`UserAdvisoryLockImpl` com `JdbcTemplate`, pois não é
  expressável em JPQL;
- `Audit`, `MailQueue` e `MailDelivery` permanecem com JDBC e participam da
  mesma transação gerenciada pelo `JpaTransactionManager`.

## Alternativas consideradas

1. **Spring Data JDBC (`CrudRepository`, sem Hibernate).** Rejeitada: manteria
   "sem ORM", mas operações multi-statement, advisory lock e projeções
   agregadas continuariam exigindo implementação própria, sem ganho real sobre
   a solução atual.
2. **Manter JDBC e extrair apenas interface + Impl.** Rejeitada: não atende ao
   pedido de repositories como interfaces Spring Data.
3. **Mapear relacionamentos `@ManyToMany` entre usuário e perfil.** Rejeitada:
   mudaria o carregamento (LAZY/N+1), traria risco de `LazyInitializationException`
   fora de transação e conflitaria com a decisão registrada de não criar
   relacionamentos de objetos. As associações continuam resolvidas por SQL.

## Consequências

Positivas:

- repositories viram contratos Spring Data com `JpaRepository`;
- consultas declarativas e testáveis, sem SQL espalhado em classes concretas;
- entidades JPA não contaminam a API nem os services;
- comportamento preservado: os 9 testes de integração continuam passando sem
  alteração de asserção.

Negativas / custos aceitos:

- Hibernate passa a fazer parte do runtime (startup e validação de schema);
- JPQL e SQL nativo convivem: o que depende de recurso do PostgreSQL continua
  nativo;
- `@Formula` e `@JdbcTypeCode(SqlTypes.CHAR)` são anotações específicas do
  Hibernate, necessárias para manter o schema atual (`CHAR(64)` em
  `email_action.token_hash`);
- escritas em massa exigem `clearAutomatically` para não devolver estado
  obsoleto do contexto de persistência.

## Preservação de comportamento

- `IdentityIT` (5) e `ProfilesIT` (4) executam sem alteração de asserção,
  cobrindo login, ciclo de identidade, convite, perfis, revogação de sessão,
  exclusão protegida por vínculo, advisory lock e consumo único de token;
- `AuthenticationTest` (5) continua passando com os repositories como mocks de
  interface;
- nenhum contrato HTTP, DTO, mensagem, auditoria ou transação foi alterado.

## Verificação

- `mvn -q -DskipTests compile`: sucesso;
- `mvn test`: 5 testes unitários, sucesso;
- `mvn -Pintegration verify` com `TEST_DATABASE_URL` em `identity_test`:
  9 testes de integração, sucesso.
