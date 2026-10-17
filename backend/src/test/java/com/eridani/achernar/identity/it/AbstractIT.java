package com.eridani.achernar.identity.it;

import com.eridani.achernar.identity.shared.json.MiniJson;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base dos testes de integração (executados por {@code mvn verify}, sufixo {@code IT}): PostgreSQL 18 real em
 * contêiner (Testcontainers), SMTP em memória (GreenMail) e o servidor HTTP completo em porta aleatória.
 * Um único contêiner/servidor é compartilhado por todas as classes (inicialização única por JVM).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIT {

    protected static final String UI_ORIGIN = "http://localhost:4200";
    protected static final String UI_CLIENT = "achernar-identity-ui";
    protected static final String BOOTSTRAP = "it-bootstrap-token-0123456789-0123456789";
    protected static final String PASSWORD = "cavalo-bateria-grampo-certo";

    private static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:18-alpine")
            .withEnv("POSTGRES_DB", "achernar_identity").withEnv("POSTGRES_USER", "achernar")
            .withEnv("POSTGRES_PASSWORD", "achernar").withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
    private static final GreenMail MAIL;
    private static final int SMTP_PORT;

    static {
        POSTGRES.start();
        SMTP_PORT = freePort();
        MAIL = new GreenMail(new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        MAIL.start();
    }

    private static int freePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
                + "/achernar_identity");
        r.add("spring.datasource.username", () -> "achernar");
        r.add("spring.datasource.password", () -> "achernar");
        r.add("spring.mail.host", () -> "127.0.0.1");
        r.add("spring.mail.port", () -> SMTP_PORT);
        r.add("achernar.mail.worker-interval", () -> "PT1S");
        r.add("achernar.password.argon2-memory-kib", () -> "4096");
        r.add("achernar.key.rsa-bits", () -> "2048");
        r.add("achernar.ui-origins", () -> UI_ORIGIN);
        r.add("achernar.ui-base-url", () -> UI_ORIGIN);
    }

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** Resposta HTTP simplificada. */
    public record Resp(int status, Map<String, List<String>> headers, String raw) {
        public Map<String, Object> json() {
            return raw == null || raw.isBlank() ? Map.of() : MiniJson.parseObject(raw);
        }

        public String header(String name) {
            return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name)).findFirst()
                    .map(e -> e.getValue().get(0)).orElse(null);
        }

        public List<String> all(String name) {
            return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name)).findFirst()
                    .map(Map.Entry::getValue).orElse(List.of());
        }

        public String str(String field) {
            Object v = json().get(field);
            return v == null ? null : v.toString();
        }
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected Resp send(String method, String path, Object body, String... headers) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url(path))).timeout(Duration.ofSeconds(30));
            boolean hasBody = body != null;
            b.method(method, hasBody ? HttpRequest.BodyPublishers.ofString(
                    body instanceof String s ? s : MiniJson.write(body), StandardCharsets.UTF_8)
                    : HttpRequest.BodyPublishers.noBody());
            if (hasBody) {
                b.header("Content-Type", "application/json");
            }
            for (int i = 0; i + 1 < headers.length; i += 2) {
                b.header(headers[i], headers[i + 1]);
            }
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(r.statusCode(), r.headers().map(), r.body());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Resp get(String path, String... headers) {
        return send("GET", path, null, headers);
    }

    protected Resp post(String path, Object body, String... headers) {
        return send("POST", path, body, headers);
    }

    protected Resp bearerGet(String path, String accessToken) {
        return get(path, "Authorization", "Bearer " + accessToken);
    }

    protected Resp bearerPost(String path, Object body, String accessToken) {
        return post(path, body, "Authorization", "Bearer " + accessToken);
    }

    /** Cabeçalhos do cliente BROWSER (defesa CSRF: Origin + X-Achernar-Client). */
    protected static String[] browserHeaders(String clientId, String cookie) {
        return cookie == null
                ? new String[] {"Origin", UI_ORIGIN, "X-Achernar-Client", clientId}
                : new String[] {"Origin", UI_ORIGIN, "X-Achernar-Client", clientId, "Cookie", cookie};
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.test";
    }

    /** Extrai o par {@code nome=valor} do primeiro Set-Cookie. */
    protected static String cookieOf(Resp r) {
        String sc = r.header("Set-Cookie");
        return sc == null ? null : sc.substring(0, sc.indexOf(';'));
    }

    // ---------------------------------------------------------------- e-mail (GreenMail)
    private static final Pattern TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]{20,})");

    /** Espera um e-mail para o destinatário e devolve o token do link de ação. */
    protected String awaitMailToken(String to) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            for (MimeMessage m : MAIL.getReceivedMessages()) {
                try {
                    if (m.getAllRecipients()[0].toString().equalsIgnoreCase(to)) {
                        Matcher mt = TOKEN.matcher(bodyOf(m));
                        if (mt.find()) {
                            return mt.group(1);
                        }
                    }
                } catch (Exception ignored) {
                    // tenta novamente
                }
            }
            sleep(250);
        }
        throw new AssertionError("e-mail para " + to + " não chegou em 30 s");
    }

    private static String bodyOf(MimeMessage m) throws Exception {
        Object c = m.getContent();
        if (c instanceof String s) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        collect(c, sb);
        return sb.toString();
    }

    private static void collect(Object content, StringBuilder sb) throws Exception {
        if (content instanceof String s) {
            sb.append(s).append('\n');
        } else if (content instanceof jakarta.mail.Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                collect(mp.getBodyPart(i).getContent(), sb);
            }
        }
    }

    protected static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------------------------------------------------------------- cenários reutilizáveis
    /** Garante que exista um administrador e devolve suas credenciais (cria na primeira chamada). */
    protected synchronized String[] ensureAdmin() {
        Integer admins = jdbc.queryForObject("SELECT count(*) FROM account WHERE role = 'ADMIN'", Integer.class);
        if (admins != null && admins > 0) {
            return new String[] {adminEmail, PASSWORD};
        }
        adminEmail = uniqueEmail("admin");
        Resp r = post("/setup/admin", Map.of("bootstrap_token", BOOTSTRAP, "email", adminEmail, "password", PASSWORD,
                "locale", "pt-BR"));
        if (r.status() != 201) {
            throw new AssertionError("setup falhou: " + r.status() + " " + r.raw());
        }
        return new String[] {adminEmail, PASSWORD};
    }

    private static volatile String adminEmail;

    /** Login pelo cliente da UI (BROWSER): devolve {access_token, cookie}. */
    protected String[] loginUi(String email, String password) {
        Resp r = post("/auth/login", Map.of("client_id", UI_CLIENT, "email", email, "password", password),
                browserHeaders(UI_CLIENT, null));
        if (r.status() != 200) {
            throw new AssertionError("login falhou: " + r.status() + " " + r.raw());
        }
        return new String[] {r.str("access_token"), cookieOf(r), r.str("session_id")};
    }

    /** Cria uma aplicação BROWSER com cadastro aberto (usa o administrador) e devolve o client_id. */
    protected String createOpenApp(String adminToken) {
        String clientId = "app-" + UUID.randomUUID().toString().substring(0, 8);
        Resp r = bearerPost("/api/v1/admin/applications", Map.of("client_id", clientId, "name", "App de teste",
                "type", "BROWSER", "origins", List.of(UI_ORIGIN), "allow_registration", true,
                "identity_api_access", true), adminToken);
        if (r.status() != 201) {
            throw new AssertionError("criação da aplicação falhou: " + r.status() + " " + r.raw());
        }
        return clientId;
    }

    /** Cadastra e verifica uma conta comum na aplicação; devolve o e-mail. */
    protected String registerVerified(String clientId) {
        String email = uniqueEmail("user");
        Resp reg = post("/auth/register", Map.of("client_id", clientId, "email", email, "password", PASSWORD,
                "locale", "pt-BR"), browserHeaders(clientId, null));
        if (reg.status() != 202 && reg.status() != 201 && reg.status() != 200) {
            throw new AssertionError("cadastro falhou: " + reg.status() + " " + reg.raw());
        }
        String token = awaitMailToken(email);
        Resp ver = post("/auth/email/verify", Map.of("client_id", clientId, "token", token),
                browserHeaders(clientId, null));
        if (ver.status() / 100 != 2) {
            throw new AssertionError("verificação falhou: " + ver.status() + " " + ver.raw());
        }
        return email;
    }
}
