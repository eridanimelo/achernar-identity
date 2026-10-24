# Consumir o JWT de usuários e perfis

O serviço de identidade emite JWT RS256 com `claim_version=2`. Cada microserviço valida o token e aplica as suas próprias regras por perfil. Nunca compartilhar a chave privada.

## Contrato de emissão
`POST /api/auth/token` recebe `email` e `password`, com a mesma proteção CSRF do login. Primeiro faça `GET /api/csrf`, mantenha o cookie recebido e envie o token retornado no header `X-XSRF-TOKEN`. A resposta contém `accessToken`, `tokenType: Bearer` e `expiresIn: 900`. Não altera o cookie de sessão do navegador. Esse endpoint é um contrato próprio de autenticação, sem metadados OIDC.

Os próximos serviços recebem `Authorization: Bearer <accessToken>`. O frontend desta aplicação continua usando cookie HttpOnly; não persiste token Bearer em localStorage.

Payload ilustrativo:
```json
{
  "claim_version": 2,
  "iss": "http://localhost:8080",
  "sub": "identificador-do-usuario",
  "aud": ["achernar-identity", "finance-service"],
  "roles": ["FINANCEIRO", "USER"],
  "role": "USER",
  "version": 4,
  "iat": 1792860000,
  "exp": 1792860900,
  "jti": "identificador-da-credencial"
}
```
`roles` é a fonte dos perfis ativos, com códigos estáveis. `role` é somente compatibilidade com a primeira versão: ADMIN se possui o perfil ADMIN ativo e USER nos demais casos. Não use esse campo singular para autorização de negócio. `version` identifica revogação no emissor; a validação criptográfica local não consulta essa versão no banco.

## Destinatários e endereços
Adicione `JWT_ADDITIONAL_AUDIENCES=finance-service` em `docker/.env` e recrie o backend. Para vários serviços, separe os identificadores por vírgula. Cada consumidor deve exigir seu próprio identificador em `aud`.

`JWT_ISSUER` é o endereço estável público do emissor e precisa corresponder exatamente ao `iss` validado no consumidor. Dentro da mesma rede Docker, o consumidor pode buscar chaves em `http://backend:8080/.well-known/jwks.json`, mantendo o issuer público. Fora dessa rede, use o host acessível ao consumidor.

## Exemplo Spring Security
Dependência, usando a mesma família Spring Boot 4 do projeto:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```
Configuração ilustrativa do próximo microserviço:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080
          jwk-set-uri: http://localhost:8080/.well-known/jwks.json
          audiences: finance-service
```
Configure a conversão de perfis em autoridades:
```java
@Bean
SecurityFilterChain security(HttpSecurity http) throws Exception {
    var authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("ROLE_");
    var authentication = new JwtAuthenticationConverter();
    authentication.setJwtGrantedAuthoritiesConverter(authorities);
    return http
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(c -> c.disable()) // consumidor exclusivo de Bearer, sem autenticação por cookie
        .authorizeHttpRequests(a -> a
            .requestMatchers("/finance/**").hasRole("FINANCEIRO")
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(authentication)))
        .build();
}
```
Imports: `org.springframework.security.config.annotation.web.builders.HttpSecurity`, `org.springframework.security.config.http.SessionCreationPolicy`, `org.springframework.security.web.SecurityFilterChain`, `org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter` e `JwtAuthenticationConverter`, `org.springframework.context.annotation.Bean`.

## Revogação em outro microserviço
Há duas políticas de validação:

- **Local:** verifica RS256, `iss`, `aud`, `exp` e `roles`. Pode aceitar uma credencial revogada até sua expiração, aproximadamente 15 minutos. Disponibilidade independe da consulta de sessão, após obtenção/cache das chaves.
- **Online:** além da validação local, chama `GET /api/auth/session` com o mesmo Bearer. Resposta 200 inclui `active`, `sub`, `profiles` e `expiresAt`; 401 indica expiração/revogação/bloqueio/alteração de perfil. Para exigir revogação imediata, faça essa verificação em cada requisição e negue acesso se o emissor estiver indisponível. Não encaminhe somente o identificador do usuário.

Exemplo de validador adicional online, usando timeout de conexão/leitura configurado no RestClient do consumidor:
```java
OAuth2TokenValidator<Jwt> online = jwt -> {
    try {
        client.get().uri(sessionUrl)
            .headers(headers -> headers.setBearerAuth(jwt.getTokenValue()))
            .retrieve().toBodilessEntity();
        return OAuth2TokenValidatorResult.success();
    } catch (RestClientException failure) {
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "Sessão indisponível ou revogada", null));
    }
};
// Incluir junto aos validadores de issuer/tempo e audience do decoder:
// decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndTime, audience, online));
```
Não substituir os validadores de assinatura, tempo, emissor e destinatário por essa consulta. Não armazenar tokens em logs ou URLs. Configurar TLS em ambiente publicado. O conjunto JWKS publicado contém somente a chave pública da assinatura atual; rotação com sobreposição de chaves antigas requer evolução operacional específica.

## Evidência reproduzível
`ProfilesIT.multiProfileAssignmentRevocationAndIndependentConsumer` sobe um consumidor HTTP separado, valida JWT com a chave publicada, converte `roles` em autoridades e autoriza FINANCEIRO. Também rejeita audience incorreta e assinatura adulterada e demonstra que, após desativação do perfil, a validação local permanece válida enquanto a consulta de sessão retorna 401. Execute com banco exclusivo conforme README.

Referência oficial: [Spring Security — Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
