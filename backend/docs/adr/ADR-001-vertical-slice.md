# ADR-001 — Backend organizado em vertical slices por feature

- **Status:** aceito; layout de pacotes e decisão de eliminar interfaces de
  service substituídos por `ADR-002-service-impl-e-pacotes.md`
- **Data:** 2026-10-25
- **Escopo:** `backend/src/main/java/com/achernar/identity`
- **Feature:** UC-001 Identidade

## Contexto

O backend nasceu organizado por capacidade técnica e por caso de uso
(`account/`, `admin/`, `auth/`, `profiles/`, `users/`, `security/`, `mail/`,
`shared/`, `bootstrap/`). Cada caso de uso tinha seu próprio controller e sua
própria interface de service, o que produzia três sintomas:

1. um endpoint por controller, ainda que pertencente à mesma feature;
2. interfaces `XService`/`XServiceImpl` sem segunda implementação, existindo
   apenas para satisfazer o par interface/implementação;
3. leitura e escrita da mesma feature dispersas em pacotes diferentes
   (`admin/`, `users/`, `profiles/`), dificultando localizar tudo o que uma
   feature possui.

A reorganização foi solicitada como mudança **estrutural**: nenhum contrato
HTTP, regra de negócio, transação, lock, auditoria ou mensagem poderia mudar.

## Decisão

Organizar o backend em **vertical slices por feature**. Cada feature contém
todas as suas camadas internas:

```text
com/achernar/identity/
├── account/
│   ├── controller/rest/
│   │   ├── AccountController.java
│   │   ├── RenameAccountRequest.java
│   │   └── AccountResponse.java
│   └── service/
│       ├── ReadAccountService.java
│       └── RenameAccountService.java
├── auth/
│   ├── controller/rest/           # AuthController + Request/Response
│   ├── domain/
│   │   ├── model/Action.java
│   │   └── repository/ActionRepository.java
│   └── service/                   # AuthActions + 1 service por caso de uso
├── users/
│   ├── controller/rest/           # UserController + Request/Response
│   ├── domain/
│   │   ├── exception/UserNotFoundException.java
│   │   ├── model/{UserEntity,UserNames,UserView}.java
│   │   └── repository/UserRepository.java
│   └── service/                   # 1 service por caso de uso + portas compartilhadas
├── profiles/
│   ├── controller/rest/           # ProfileController + Request/Response
│   ├── domain/
│   │   ├── exception/ProfileNotFoundException.java
│   │   ├── model/ProfileView.java
│   │   └── repository/ProfileRepository.java
│   └── service/
├── shared/
│   ├── audit/Audit.java
│   ├── config/{SecurityConfig,RequestLimits,AdminBootstrap}.java
│   ├── exception/{ApiErrors,ApiException}.java
│   ├── mail/{MailQueue,MailDelivery}.java
│   └── security/{TokenService,PublicEndpoints}.java
└── IdentityApplication.java       # raiz do pacote
```

Regras da estrutura:

- **um controller por feature**, não por caso de uso; os handlers apenas
  traduzem HTTP ↔ service;
- **um service por caso de uso**, com nome de ação (`RegisterService`,
  `LoginService`, `ResetPasswordService`, `UpdateUserService`, ...);
- `domain/model` guarda os modelos e a leitura tipada (`UserEntity`,
  `UserView`, `ProfileView`, `Action`);
- `domain/repository` guarda o acesso a dados com SQL explícito;
- `domain/exception` guarda a exceção da feature, tratada em
  `shared/exception/ApiErrors` com o mesmo envelope `{code,message}`;
- DTOs HTTP (`*Request`, `*Response`) ficam em `controller/rest`, para que o
  repository não dependa de REST;
- `shared` só recebe o que é realmente transversal.

Portas compartilhadas dentro da feature, sem interface/implementação:

| Porta | Responsabilidade |
| --- | --- |
| `auth/service/AuthActions` | emissão e consumo de links de e-mail, normalização de e-mail e hash de token |
| `auth/service/LoginService` | autenticação de credenciais, reutilizada por `/login` e `/token` |
| `users/service/UserLookup` | leitura de usuário para validação de sessão |
| `users/service/UserAccessService` | `lockAdmin`, `lockTarget`, `protect`, `revokeProfile` |
| `profiles/service/ProfileLookup` | perfil obrigatório (404) |
| `profiles/service/ProfileResolver` | resolução de códigos de perfil em IDs |

Interfaces `XService`/`XServiceImpl` foram eliminadas: sem variação de
implementação, a interface era cerimônia e duplicava o contrato.

## Consequências

Positivas:

- uma feature é lida em um único diretório, das rotas ao SQL;
- adicionar um caso de uso significa adicionar um service e um handler, sem
  criar pacote de caso de uso nem interface;
- o limite entre features fica explícito: `users` e `profiles` expõem portas
  de serviço, não seus repositories;
- `shared` deixa de acumular classes de negócio (`Audit`, `ApiException`).

Negativas / custos aceitos:

- `AuthController` e `UserController` concentram mais dependências que os
  antigos controllers de caso único;
- novos arquivos por feature (`*Response`, `*Page`) fazem a tradução explícita
  entre modelo de domínio e JSON;
- exceções específicas de feature exigem handler em `ApiErrors`.

## Preservação de comportamento

A refatoração não alterou:

- paths, verbos HTTP, códigos de status e corpos JSON (validados pelos
  `IdentityIT` e `ProfilesIT` existentes);
- cookie `IDENTITY_SESSION` (HttpOnly, SameSite=Strict, `secure` por
  configuração), CSRF e validação JWT/JWKS;
- fronteiras transacionais e `Propagation.MANDATORY` das portas chamadas
  dentro de transação, `NOT_SUPPORTED` nas leituras independentes e os locks
  (`FOR UPDATE`, advisory lock administrativo);
- mensagens de erro e auditoria (`REGISTER`, `LOGIN`, `RENAME`,
  `ADMIN_CREATE`, `ADMIN_CHANGE`, `ADMIN_DELETE`, `PROFILE_*`, ...);
- o frontend Angular, que consome exatamente os mesmos endpoints e campos.

## Verificação

- `mvn -q -DskipTests compile`: sucesso;
- `mvn test`: 5 testes unitários, sucesso;
- `mvn -Pintegration verify` com `TEST_DATABASE_URL` em `identity_test`:
  9 testes de integração, sucesso;
- frontend: `npm run lint`, `npm test` (9 testes) e `npm run build` sem
  alteração de código e sem falhas.
