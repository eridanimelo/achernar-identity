package com.achernar.identity;

import com.achernar.identity.mail.MailDelivery;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=${TEST_DATABASE_URL}", "app.generate-key=true", "app.key-file=./target/test-key.json",
        "app.secure-cookie=false", "app.mail-initial-delay=3600000", "app.admin-email=", "app.admin-password=" })
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".*identity_test.*")
class IdentityIT {
    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate db;
    @Autowired
    MailDelivery delivery;
    final String password = "SenhaInicial12345!";

    class Client {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

        HttpResponse<String> call(String method, String path, String json, boolean csrf) throws Exception {
            String token = "";
            if (csrf) {
                var response = call("GET", "/api/csrf", null, false);
                assertEquals(200, response.statusCode(), response.body());
                token = extract(response.body(), "\"token\":\"([^\"]+)\"");
            }
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type",
                    "application/json");
            if (csrf)
                builder.header("X-XSRF-TOKEN", token);
            return http.send(
                    builder.method(method,
                            json == null ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofString(json))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, String json) throws Exception {
            return call("POST", path, json, true);
        }
    }

    String extract(String text, String regex) {
        var matcher = Pattern.compile(regex).matcher(text);
        assertTrue(matcher.find(), text);
        return matcher.group(1);
    }

    String token(String email) {
        return extract(
                db.queryForObject("SELECT body FROM mail_outbox WHERE recipient=? ORDER BY next_attempt DESC LIMIT 1",
                        String.class, email),
                "#token=([A-Za-z0-9_-]+)");
    }

    String id(String email) {
        return db.queryForObject("SELECT id::text FROM app_user WHERE email=?", String.class, email);
    }

    String credentials(String email, String pass) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + pass + "\"}";
    }

    void register(Client client, String email) throws Exception {
        assertEquals(202, client.post("/api/auth/register", "{\"name\":\"Pessoa Teste\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\",\"role\":\"ADMIN\"}").statusCode());
    }

    @BeforeEach
    void clear() {
        db.execute("TRUNCATE email_action,mail_outbox,security_audit_event,app_user CASCADE");
        db.execute("DELETE FROM access_profile WHERE NOT system");
    }

    @Test
    void completeIdentityLifecycle() throws Exception {
        var user = new Client();
        var visitor = new Client();
        var admin = new Client();
        assertEquals(403, visitor.call("POST", "/api/auth/register", "{}", false).statusCode());
        assertEquals(401, visitor.call("GET", "/api/me", null, false).statusCode());
        assertEquals(400, user.post("/api/auth/register", "{\"name\":\"A\",\"email\":\"bad\",\"password\":\"tiny\"}")
                .statusCode());
        register(user, "user@example.com");
        assertEquals("USER", db.queryForObject(
                "SELECT p.code FROM app_user u JOIN user_profile up ON up.user_id=u.id JOIN access_profile p ON p.id=up.profile_id WHERE u.email='user@example.com'",
                String.class));
        assertEquals(403, user.post("/api/auth/login", credentials("user@example.com", password)).statusCode());
        String verify = token("user@example.com");
        assertEquals(400, user
                .post("/api/auth/reset-password", "{\"token\":\"" + verify + "\",\"password\":\"OutraSenha123456!\"}")
                .statusCode());
        assertEquals(200, user.post("/api/auth/verify", "{\"token\":\"" + verify + "\"}").statusCode());
        assertEquals(400, user.post("/api/auth/verify", "{\"token\":\"" + verify + "\"}").statusCode());
        assertEquals(401, user.post("/api/auth/login", credentials("user@example.com", "senha errada")).statusCode());
        var login = user.post("/api/auth/login", credentials("user@example.com", password));
        assertEquals(200, login.statusCode(), login.body());
        assertTrue(login.headers().firstValue("Set-Cookie").orElseThrow().contains("HttpOnly"));
        assertFalse(login.body().contains("password"));
        assertEquals(200, user.call("GET", "/api/me", null, false).statusCode());
        assertEquals(403, user.call("GET", "/api/admin/users", null, false).statusCode());
        assertEquals(200, user.call("PATCH", "/api/me", "{\"name\":\"Nome Novo\"}", true).statusCode());
        assertEquals(400, user.call("PATCH", "/api/me", "{\"name\":\" a \"}", true).statusCode());
        assertEquals(202, user.post("/api/auth/forgot-password", "{\"email\":\"user@example.com\"}").statusCode());
        String reset = token("user@example.com");
        assertEquals(200, user
                .post("/api/auth/reset-password", "{\"token\":\"" + reset + "\",\"password\":\"OutraSenha123456!\"}")
                .statusCode());
        assertEquals(401, user.call("GET", "/api/me", null, false).statusCode());
        assertEquals(400, user
                .post("/api/auth/reset-password", "{\"token\":\"" + reset + "\",\"password\":\"OutraSenha123456!\"}")
                .statusCode());
        assertEquals(200,
                user.post("/api/auth/login", credentials("user@example.com", "OutraSenha123456!")).statusCode());
        register(admin, "admin@example.com");
        db.update("UPDATE app_user SET verified=true WHERE email='admin@example.com'");
        db.update(
                "INSERT INTO user_profile(user_id,profile_id) SELECT u.id,p.id FROM app_user u CROSS JOIN access_profile p WHERE u.email='admin@example.com' AND p.code='ADMIN'");
        assertEquals(200, admin.post("/api/auth/login", credentials("admin@example.com", password)).statusCode());
        assertEquals(200, admin.call("GET", "/api/admin/users?q=Nome&page=0&size=1", null, false).statusCode());
        assertEquals(400, admin.call("GET", "/api/admin/users?size=101", null, false).statusCode());
        assertEquals(409, admin.call("DELETE", "/api/admin/users/" + id("admin@example.com"), null, true).statusCode());
        String target = id("user@example.com");
        assertEquals(200,
                admin.call("PATCH", "/api/admin/users/" + target, "{\"status\":\"BLOCKED\"}", true).statusCode());
        assertEquals(401, user.call("GET", "/api/me", null, false).statusCode());
        assertEquals(401,
                user.post("/api/auth/login", credentials("user@example.com", "OutraSenha123456!")).statusCode());
        assertEquals(200,
                admin.call("PATCH", "/api/admin/users/" + target, "{\"status\":\"ACTIVE\"}", true).statusCode());
        assertEquals(200,
                user.post("/api/auth/login", credentials("user@example.com", "OutraSenha123456!")).statusCode());
        assertEquals(200, admin.call("PATCH", "/api/admin/users/" + target, "{\"role\":\"ADMIN\"}", true).statusCode());
        assertEquals(401, user.call("GET", "/api/me", null, false).statusCode());
        assertEquals(204, admin.call("DELETE", "/api/admin/users/" + target, null, true).statusCode());
        assertEquals("DELETED",
                db.queryForObject("SELECT status FROM app_user WHERE id=?::uuid", String.class, target));
        assertEquals(204, admin.post("/api/auth/logout", "{}").statusCode());
        assertEquals(401, admin.call("GET", "/api/me", null, false).statusCode());
        delivery.deliver();
        assertEquals(0, db.queryForObject("SELECT count(*) FROM mail_outbox WHERE sent_at IS NULL", Integer.class));
        assertTrue(db.queryForObject("SELECT count(*) FROM security_audit_event", Integer.class) > 5);
    }

    @Test
    void expiredLinksAndTamperedSessionsAreRejected() throws Exception {
        var user = new Client();
        register(user, "expired@example.com");
        String token = token("expired@example.com");
        db.update("UPDATE email_action SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 minute'");
        assertEquals(400, user.post("/api/auth/verify", "{\"token\":\"" + token + "\"}").statusCode());
        var cookie = new HttpCookie("IDENTITY_SESSION", "tampered.jwt.value");
        cookie.setPath("/");
        cookie.setVersion(0);
        user.cookies.getCookieStore().add(URI.create("http://localhost:" + port), cookie);
        assertEquals(401, user.call("GET", "/api/me", null, false).statusCode());
        assertEquals(200, user.call("GET", "/api/csrf", null, false).statusCode());
    }

    @Test
    void duplicateAndUnknownEmailHaveSamePublicResponse() throws Exception {
        var user = new Client();
        register(user, "duplicate@example.com");
        var a = user.post("/api/auth/forgot-password", "{\"email\":\"duplicate@example.com\"}");
        var b = user.post("/api/auth/forgot-password", "{\"email\":\"unknown@example.com\"}");
        assertEquals(a.body(), b.body());
        assertEquals(202, b.statusCode());
        register(user, "duplicate@example.com");
        assertEquals(1, db.queryForObject("SELECT count(*) FROM app_user", Integer.class));
    }

    @Test
    void actionIsConsumedAtMostOnceConcurrently() throws Exception {
        var client = new Client();
        register(client, "concurrent@example.com");
        String token = token("concurrent@example.com");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var ready = new java.util.concurrent.CountDownLatch(2);
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> attempt = () -> {
                var c = new Client();
                ready.countDown();
                start.await();
                return c.post("/api/auth/verify", "{\"token\":\"" + token + "\"}").statusCode();
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            ready.await();
            start.countDown();
            var statuses = new ArrayList<>(List.of(first.get(), second.get()));
            Collections.sort(statuses);
            assertEquals(List.of(200, 400), statuses);
        }
    }

    @Test
    void publicActionRequestsAreRateLimited() throws Exception {
        var client = new Client();
        int status = 0;
        for (int i = 0; i < 16; i++)
            status = client.post("/api/auth/resend", "{\"email\":\"unknown@example.com\"}").statusCode();
        assertEquals(429, status);
    }

}
