package com.eridani.achernar.identity.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** CT-CON-01/02: rotação sob concorrência e detecção de reuso (token-lifecycle §2.1). */
class RefreshConcurrencyIT extends AbstractIT {

    private String[] session() {
        String[] admin = ensureAdmin();
        String clientId = createOpenApp(loginUi(admin[0], admin[1])[0]);
        String email = registerVerified(clientId);
        Resp login = post("/auth/login", Map.of("client_id", clientId, "email", email, "password", PASSWORD),
                browserHeaders(clientId, null));
        return new String[] {clientId, cookieOf(login), email};
    }

    @Test
    void duasRequisicoesSimultaneasComOMesmoTokenTemUmaRotacaoEUmaTolerada() throws Exception {
        String[] s = session();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Resp>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> {
                start.await();
                return post("/auth/refresh", Map.of(), browserHeaders(s[0], s[1]));
            });
        }
        List<Future<Resp>> futures = new ArrayList<>();
        for (Callable<Resp> t : tasks) {
            futures.add(pool.submit(t));
        }
        start.countDown();
        List<Resp> results = new ArrayList<>();
        for (Future<Resp> f : futures) {
            results.add(f.get());
        }
        pool.shutdown();
        assertThat(results.get(0).status()).isEqualTo(200);
        assertThat(results.get(1).status()).isEqualTo(200);
        long rotated = results.stream().filter(r -> Boolean.TRUE.equals(r.json().get("refresh_rotated"))).count();
        assertThat(rotated).isEqualTo(1L);
        // a sessão continua válida (a tolerância não revoga)
        Integer ativas = jdbc.queryForObject("SELECT count(*) FROM session WHERE revoked_at IS NULL AND account_id = "
                + "(SELECT id FROM account WHERE email_normalized = ?)", Integer.class, s[2].toLowerCase());
        assertThat(ativas).isEqualTo(1);
    }

    @Test
    void reapresentarTokenAntigoForaDaGracaRevogaASessao() {
        String[] s = session();
        Resp first = post("/auth/refresh", Map.of(), browserHeaders(s[0], s[1]));
        assertThat(first.status()).isEqualTo(200);
        String novo = cookieOf(first);
        // simula passagem da janela de tolerância
        jdbc.update("UPDATE refresh_token SET rotated_at = rotated_at - interval '1 minute' WHERE rotated_at IS NOT NULL");
        Resp reuse = post("/auth/refresh", Map.of(), browserHeaders(s[0], s[1]));
        assertThat(reuse.status()).isEqualTo(401);
        assertThat(reuse.str("code")).isEqualTo("INVALID_REFRESH_TOKEN");
        // o token legítimo mais recente também deixa de funcionar (sessão revogada)
        Resp legit = post("/auth/refresh", Map.of(), browserHeaders(s[0], novo));
        assertThat(legit.status()).isEqualTo(401);
        Integer evento = jdbc.queryForObject("SELECT count(*) FROM security_event WHERE event_type = 'REFRESH_REUSE_DETECTED'",
                Integer.class);
        assertThat(evento).isGreaterThan(0);
    }
}
