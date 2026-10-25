# ADR-002 — Service/ServiceImpl e pacotes por feature conforme o template Java

- **Status:** aceito
- **Data:** 2026-10-25
- **Escopo:** `backend/src/main/java/com/achernar/identity`
- **Feature:** UC-001 Identidade
- **Substitui parcialmente:** `ADR-001-vertical-slice.md` (layout de pacotes e
  decisão de eliminar interfaces de service)

## Contexto

O ADR-001 organizou o backend em vertical slices por feature e, na mesma
decisão, eliminou os pares `XService`/`XServiceImpl` e adotou
`controller/rest/`, `domain/model/`, `domain/repository/` e
`domain/exception/`.

Essa decisão divergiu de duas referências normativas do framework:

- `rules/backend/java-backend.md` §1 — exige, para funcionalidades com
  persistência, `Entity`, DTOs, interface `Service`, implementação
  `ServiceImpl` e `Repository`;
- `.agents/skills/backend/assets/java-backend.md` — define o layout
  `feature/controller`, `feature/dto`, `feature/entity`, `feature/repository`,
  `feature/service`, `feature/service/impl`, `feature/exception`.

A divergência foi levantada como decisão humana: alinhar o backend ao padrão
do framework e ao padrão de mercado (interface de serviço + implementação
isolada), mantendo a organização por feature já validada no ADR-001.

## Decisão

### 1. Layout de pacotes por feature

```text
com/achernar/identity/
├── account/
│   ├── controller/AccountController.java
│   ├── dto/{AccountResponse,RenameAccountRequest}.java
│   ├── service/{ReadAccountService,RenameAccountService}.java
│   └── service/impl/{ReadAccountServiceImpl,RenameAccountServiceImpl}.java
├── auth/
│   ├── controller/AuthController.java
│   ├── dto/                      # Request/Response + resultados de serviço
│   ├── entity/Action.java
│   ├── repository/ActionRepository.java
│   ├── service/                  # interfaces
│   └── service/impl/             # implementações
├── users/
│   ├── controller/UserController.java
│   ├── dto/                      # Request/Response, UserPage
│   ├── entity/{UserEntity,UserView,UserNames}.java
│   ├── exception/UserNotFoundException.java
│   ├── repository/UserRepository.java
│   └── service/ + service/impl/
├── profiles/
│   ├── controller/ProfileController.java
│   ├── dto/                      # Request/Response, ProfilePage
│   ├── entity/ProfileView.java
│   ├── exception/ProfileNotFoundException.java
│   ├── repository/ProfileRepository.java
│   └── service/ + service/impl/
├── shared/                       # audit, config, exception, mail, security
└── IdentityApplication.java
```

Regras:

- `controller/` contém apenas o controller da feature (sem subpacote `rest`);
- `dto/` contém tudo que atravessa fronteira: `*Request`, `*Response`,
  `*Page`, `*View` de saída e resultados de serviço (`LoginResult`,
  `IssuedToken`, `SessionView`);
- `entity/` contém o modelo do agregado mapeado de SQL — não há JPA no projeto
  (`jpa_hibernate_domain: false`, Spring JDBC);
- `repository/` contém o acesso a dados;
- `service/` contém somente interfaces;
- `service/impl/` contém as implementações, anotadas com `@Service` e
  responsáveis por `@Transactional`/`Propagation`;
- `exception/` contém exceções específicas da feature;
- `shared/` permanece com o que é realmente transversal.

### 2. Interface + implementação

Todo service de caso de uso e toda porta compartilhada entre features passa a
ter interface em `service/` e implementação em `service/impl/`:

| Feature | Interfaces com implementação |
| --- | --- |
| account | `ReadAccountService`, `RenameAccountService` |
| auth | `RegisterService`, `LoginService`, `IssueTokenService`, `LogoutService`, `SessionService`, `VerifyEmailService`, `ResendVerificationService`, `ForgotPasswordService`, `ResetPasswordService`, `ActivateAccountService`, `AuthActions` |
| users | `CreateUserService`, `GetUserService`, `ListUsersService`, `UpdateUserService`, `DeleteUserService`, `UserLookup`, `UserAccessService` |
| profiles | `CreateProfileService`, `GetProfileService`, `ListProfilesService`, `UpdateProfileService`, `DeleteProfileService`, `ProfileLookup`, `ProfileResolver` |

Detalhes:

- `AuthActions` permanece com `normalize`/`hash` como métodos `static` na
  interface, preservando as chamadas existentes;
- classes de infraestrutura sem variação de implementação continuam concretas:
  `shared/security/TokenService`, `shared/mail/MailQueue`,
  `shared/mail/MailDelivery`, `shared/audit/Audit`;
- anotações transacionais ficam na implementação, não na interface.

## Alternativas consideradas

1. **Manter o ADR-001 (services concretos, sem interface).** Rejeitada como
   decisão humana: contraria `rules/backend/java-backend.md` §1 e o template do
   framework.
2. **Manter `domain/model`, `domain/repository`, `domain/exception` e apenas
   adicionar `dto/` + `service/impl/`.** Rejeitada: preservaria dois esquemas
   de pacotes convivendo e um nível `domain/` que não agrega no projeto
   (Spring JDBC, sem agregados ricos).
3. **Interface + impl para toda e qualquer classe de serviço, inclusive
   infraestrutura.** Rejeitada: gera indireção sem variação de implementação
   para `TokenService`, `MailQueue` e `MailDelivery`.

## Consequências

Positivas:

- alinhamento integral com `rules/backend/java-backend.md` e com o template
  `.agents/skills/backend/assets/java-backend.md`;
- contrato de serviço explícito e testável por interface, com implementação
  substituível;
- leitura uniforme entre features: o mesmo conjunto de pacotes em todas;
- `ProfilePage`, `UserPage`, `LoginResult`, `IssuedToken` e `SessionView`
  deixam de morar em `service/`, separando contrato de dados de contrato de
  comportamento.

Negativas / custos aceitos:

- dobra o número de arquivos nos services (~26 interfaces + 26 implementações);
- exige manter interface e implementação em sincronia quando o contrato muda;
- o pacote `entity/` não corresponde a JPA; nome é mantido por alinhamento ao
  template e documentado aqui.

## Preservação de comportamento

Nenhum contrato foi alterado:

- paths, verbos, status HTTP e corpos JSON idênticos;
- transações, `Propagation.MANDATORY`/`NOT_SUPPORTED`, locks, auditoria e
  mensagens preservados na implementação;
- `AuthActions.normalize`/`hash` mantidos como métodos estáticos da interface.

## Verificação

- `mvn -q -DskipTests compile`: sucesso;
- `mvn test`: 5 testes unitários, sucesso;
- `mvn -Pintegration verify` com `TEST_DATABASE_URL` em `identity_test`:
  9 testes de integração (5 `IdentityIT` + 4 `ProfilesIT`), sucesso.
