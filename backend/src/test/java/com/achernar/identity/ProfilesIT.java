package com.achernar.identity;

import com.nimbusds.jose.util.JSONObjectUtils;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=${TEST_DATABASE_URL}", "app.generate-key=true",
        "app.key-file=./target/test-key-profiles.json", "app.secure-cookie=false", "app.mail-initial-delay=3600000",
        "app.admin-email=", "app.admin-password=", "app.additional-audiences=consumer-service" })
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".*identity_test.*")
class ProfilesIT {
    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate db;
    final String password = "SenhaInicial12345!";
    Client admin;

    String base() {
        return "http://localhost:" + port;
    }

    String json(Map<String, ?> map) {
        return JSONObjectUtils.toJSONString(new HashMap<>(map));
    }

    String field(HttpResponse<String> response, String field) throws Exception {
        return JSONObjectUtils.parse(response.body()).get(field).toString();
    }

    class Client {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

        HttpResponse<String> call(String method, String path, Object body) throws Exception {
            var builder = HttpRequest.newBuilder(URI.create(base() + path)).header("Content-Type", "application/json");
            if (!method.equals("GET")) {
                var csrf = http.send(HttpRequest.newBuilder(URI.create(base() + "/api/csrf")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                builder.header("X-XSRF-TOKEN", field(csrf, "token"));
            }
            String text = body == null ? "" : body instanceof String value ? value : json((Map<String, ?>) body);
            return http.send(builder.method(method, HttpRequest.BodyPublishers.ofString(text)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        String token(String email) throws Exception {
            var response = call("POST", "/api/auth/token", Map.of("email", email, "password", password));
            assertEquals(200, response.statusCode(), response.body());
            return field(response, "accessToken");
        }
    }

    HttpResponse<String> bearer(String token, String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base() + path))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    String profile(String code) throws Exception {
        var response = admin.call("POST", "/api/admin/profiles",
                Map.of("code", code, "name", "Perfil " + code, "description", "Descrição"));
        assertEquals(201, response.statusCode(), response.body());
        return field(response, "id");
    }

    String user(String email) throws Exception {
        var client = new Client();
        assertEquals(202, client.call("POST", "/api/auth/register",
                Map.of("name", "Pessoa Teste", "email", email, "password", password)).statusCode());
        db.update("UPDATE app_user SET verified=true WHERE email=?", email);
        return db.queryForObject("SELECT id::text FROM app_user WHERE email=?", String.class, email);
    }

    String actionToken(String email) {
        String body = db.queryForObject(
                "SELECT body FROM mail_outbox WHERE recipient=? ORDER BY next_attempt DESC LIMIT 1", String.class,
                email);
        var matcher = Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(body);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    @BeforeEach
    void setup() throws Exception {
        db.execute("TRUNCATE email_action,mail_outbox,security_audit_event,app_user CASCADE");
        db.execute("DELETE FROM access_profile WHERE NOT system");
        admin = new Client();
        String id = user("admin2@example.com");
        db.update(
                "INSERT INTO user_profile(user_id,profile_id) SELECT ?::uuid,id FROM access_profile WHERE code='ADMIN'",
                id);
        assertEquals(200,
                admin.call("POST", "/api/auth/login", Map.of("email", "admin2@example.com", "password", password))
                        .statusCode());
    }

    @Test
    void profileCrudAndProtectedSystemProfiles() throws Exception {
        String id = profile("FINANCEIRO");
        assertEquals(409, admin
                .call("POST", "/api/admin/profiles", Map.of("code", "FINANCEIRO", "name", "Outro", "description", ""))
                .statusCode());
        assertEquals(400, admin
                .call("POST", "/api/admin/profiles", Map.of("code", "bad-code", "name", "Outro", "description", ""))
                .statusCode());
        assertEquals(200, admin.call("GET", "/api/admin/profiles/" + id, null).statusCode());
        var list = admin.call("GET", "/api/admin/profiles?q=financeiro&size=1", null);
        assertEquals(200, list.statusCode());
        assertTrue(list.body().contains("FINANCEIRO"));
        assertEquals(200,
                admin.call("PATCH", "/api/admin/profiles/" + id,
                        Map.of("name", "Financeiro atualizado", "description", "Novo texto", "active", false))
                        .statusCode());
        var active = admin.call("GET", "/api/admin/profiles?active=true", null);
        assertFalse(active.body().contains("FINANCEIRO"));
        assertEquals(200, admin.call("PATCH", "/api/admin/profiles/" + id,
                Map.of("name", "Financeiro", "description", "", "active", true)).statusCode());
        assertEquals(409,
                admin.call("DELETE", "/api/admin/profiles/00000000-0000-0000-0000-000000000001", null).statusCode());
        assertEquals(409, admin.call("PATCH", "/api/admin/profiles/00000000-0000-0000-0000-000000000002",
                Map.of("name", "Outro", "description", "", "active", false)).statusCode());
        assertEquals(204, admin.call("DELETE", "/api/admin/profiles/" + id, null).statusCode());
        assertEquals(404, admin.call("GET", "/api/admin/profiles/" + id, null).statusCode());
    }

    @Test
    void multiProfileAssignmentRevocationAndIndependentConsumer() throws Exception {
        String profileId = profile("FINANCEIRO");
        String uid = user("consumer@example.com");
        var client = new Client();
        String before = client.token("consumer@example.com");
        assertEquals(403, bearer(before, "/api/admin/profiles").statusCode());
        assertEquals(200,
                admin.call("PATCH", "/api/admin/users/" + uid, Map.of("profiles", List.of("USER", "FINANCEIRO")))
                        .statusCode());
        assertEquals(401, bearer(before, "/api/auth/session").statusCode());
        String token = client.token("consumer@example.com");
        var decoder = NimbusJwtDecoder.withJwkSetUri(base() + "/.well-known/jwks.json").build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("http://localhost:8080"),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains("consumer-service"))));
        var jwt = decoder.decode(token);
        assertEquals(List.of("FINANCEIRO", "USER"), jwt.getClaimAsStringList("roles"));
        var converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        assertTrue(converter.convert(jwt).stream().anyMatch(a -> a.getAuthority().equals("ROLE_FINANCEIRO")));
        var consumer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            consumer.createContext("/reports", exchange -> {
                int status = 403;
                try {
                    var incoming = decoder.decode(exchange.getRequestHeaders().getFirst("Authorization").substring(7));
                    if (incoming.getClaimAsStringList("roles").contains("FINANCEIRO"))
                        status = 200;
                } catch (Exception ignored) {
                    status = 401;
                }
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            });
            consumer.start();
            var request = HttpRequest
                    .newBuilder(URI.create("http://127.0.0.1:" + consumer.getAddress().getPort() + "/reports"))
                    .header("Authorization", "Bearer " + token).GET().build();
            assertEquals(200,
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
        } finally {
            consumer.stop(0);
        }
        decoder.setJwtValidator(
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains("wrong-service")));
        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("http://localhost:8080"));
        assertThrows(JwtException.class,
                () -> decoder.decode(token.substring(0, token.lastIndexOf('.') + 1) + "tampered"));
        assertEquals(409, admin.call("DELETE", "/api/admin/profiles/" + profileId, null).statusCode());
        assertEquals(200, admin.call("PATCH", "/api/admin/profiles/" + profileId,
                Map.of("name", "Financeiro", "description", "", "active", false)).statusCode());
        assertNotNull(decoder.decode(token));
        assertEquals(401, bearer(token, "/api/auth/session").statusCode());
        String current = client.token("consumer@example.com");
        assertEquals(List.of("USER"), decoder.decode(current).getClaimAsStringList("roles"));
        assertEquals(400,
                admin.call("PATCH", "/api/admin/users/" + uid, Map.of("profiles", List.of("FINANCEIRO"))).statusCode());
        assertEquals(400, admin.call("PATCH", "/api/admin/users/" + uid, Map.of("profiles", List.of())).statusCode());
        assertEquals(400,
                admin.call("PATCH", "/api/admin/users/" + uid, Map.of("profiles", List.of("MISSING"))).statusCode());
    }

    @Test
    void adminInvitationAndEmailChangeRequireConfirmation() throws Exception {
        var created = admin.call("POST", "/api/admin/users",
                Map.of("name", "Convidado", "email", "invite@example.com", "profiles", List.of("USER")));
        assertEquals(201, created.statusCode(), created.body());
        String id = field(created, "id");
        String invite = actionToken("invite@example.com");
        var client = new Client();
        assertEquals(401,
                client.call("POST", "/api/auth/login", Map.of("email", "invite@example.com", "password", password))
                        .statusCode());
        assertEquals(200,
                client.call("POST", "/api/auth/activate", Map.of("token", invite, "password", password)).statusCode());
        assertEquals(400,
                client.call("POST", "/api/auth/activate", Map.of("token", invite, "password", password)).statusCode());
        String token = client.token("invite@example.com");
        assertEquals(200, admin.call("GET", "/api/admin/users/" + id, null).statusCode());
        assertEquals(200, admin
                .call("PATCH", "/api/admin/users/" + id, Map.of("name", "Nome editado", "email", "new@example.com"))
                .statusCode());
        assertEquals(401, bearer(token, "/api/me").statusCode());
        assertEquals(403,
                client.call("POST", "/api/auth/login", Map.of("email", "new@example.com", "password", password))
                        .statusCode());
        assertEquals(200,
                client.call("POST", "/api/auth/verify", Map.of("token", actionToken("new@example.com"))).statusCode());
        assertEquals(200, bearer(client.token("new@example.com"), "/api/me").statusCode());
        assertEquals(409,
                admin.call("POST", "/api/admin/users",
                        Map.of("name", "Duplicado", "email", "new@example.com", "profiles", List.of("USER")))
                        .statusCode());
    }

    @Test
    void profileOnlyAccountAndBearerCannotFallbackToCookie() throws Exception {
        String pid = profile("ANALISTA");
        String uid = user("analyst@example.com");
        assertEquals(200,
                admin.call("PATCH", "/api/admin/users/" + uid, Map.of("profiles", List.of("ANALISTA"))).statusCode());
        var client = new Client();
        String token = client.token("analyst@example.com");
        var rename = HttpRequest.newBuilder(URI.create(base() + "/api/me")).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json(Map.of("name", "Analista atualizado"))))
                .build();
        assertEquals(200, HttpClient.newHttpClient().send(rename, HttpResponse.BodyHandlers.ofString()).statusCode());
        var invalid = HttpRequest.newBuilder(URI.create(base() + "/api/me"))
                .header("Authorization", "Bearer invalid.jwt.value").GET().build();
        assertEquals(401, admin.http.send(invalid, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(200, admin.call("PATCH", "/api/admin/profiles/" + pid,
                Map.of("name", "Analista", "description", "", "active", false)).statusCode());
        assertEquals(401, bearer(token, "/api/me").statusCode());
        assertEquals(403,
                client.call("POST", "/api/auth/token", Map.of("email", "analyst@example.com", "password", password))
                        .statusCode());
        assertEquals(200, admin.call("PATCH", "/api/admin/profiles/" + pid,
                Map.of("name", "Analista", "description", "", "active", true)).statusCode());
        assertEquals(200, bearer(client.token("analyst@example.com"), "/api/auth/session").statusCode());
    }

}
