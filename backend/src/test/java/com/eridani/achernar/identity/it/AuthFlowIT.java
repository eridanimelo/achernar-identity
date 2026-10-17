package com.eridani.achernar.identity.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ciclo completo: setup → aplicação → cadastro → verificação → login → refresh → logout. */
class AuthFlowIT extends AbstractIT {

    @Test
    void setupEncerradoRecusaCriacaoMesmoComTokenCorreto() {
        ensureAdmin();
        Resp r = post("/setup/admin", Map.of("bootstrap_token", BOOTSTRAP, "email",
                uniqueEmail("x"), "password", PASSWORD));
        assertThat(r.status()).isEqualTo(404);
        assertThat(r.str("code")).isEqualTo("SETUP_NOT_AVAILABLE");
    }

    @Test
    void cicloCompletoDeCadastroLoginRefreshELogout() {
        String[] admin = ensureAdmin();
        String adminToken = loginUi(admin[0], admin[1])[0];
        String clientId = createOpenApp(adminToken);

        String email = registerVerified(clientId);

        // login (BROWSER): token no corpo, refresh em cookie HttpOnly
        Resp login = post("/auth/login", Map.of("client_id", clientId, "email", email, "password", PASSWORD),
                browserHeaders(clientId, null));
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.str("token_type")).isEqualTo("Bearer");
        assertThat(login.json().containsKey("refresh_token")).isFalse();
        String setCookie = login.header("Set-Cookie");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(login.header("Cache-Control")).contains("no-store");

        // access token acessa a própria conta
        Resp me = bearerGet("/api/v1/account", login.str("access_token"));
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.str("email")).isEqualTo(email);

        // refresh rotaciona o cookie
        String cookie1 = cookieOf(login);
        Resp refreshed = post("/auth/refresh", Map.of(), browserHeaders(clientId, cookie1));
        assertThat(refreshed.status()).isEqualTo(200);
        String cookie2 = cookieOf(refreshed);
        assertThat(cookie2).isNotEqualTo(cookie1);

        // logout revoga a sessão: refresh posterior falha
        Resp out = post("/auth/logout", Map.of(), browserHeaders(clientId, cookie2));
        assertThat(out.status()).isIn(200, 204);
        Resp after = post("/auth/refresh", Map.of(), browserHeaders(clientId, cookie2));
        assertThat(after.status()).isEqualTo(401);
        assertThat(after.str("code")).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void refreshBrowserSemCabecalhoDeClienteEhRecusado() {
        String[] admin = ensureAdmin();
        String[] s = loginUi(admin[0], admin[1]);
        Resp r = post("/auth/refresh", Map.of(), "Origin", UI_ORIGIN, "Cookie", s[1]);
        assertThat(r.status()).isIn(400, 401, 403);
    }

    @Test
    void loginComSenhaErradaERespostaNeutra() {
        String[] admin = ensureAdmin();
        Resp wrong = post("/auth/login", Map.of("client_id", UI_CLIENT, "email", admin[0], "password",
                "senha-totalmente-errada-1"), browserHeaders(UI_CLIENT, null));
        Resp unknown = post("/auth/login", Map.of("client_id", UI_CLIENT, "email", uniqueEmail("ninguem"), "password",
                "senha-totalmente-errada-1"), browserHeaders(UI_CLIENT, null));
        assertThat(wrong.status()).isEqualTo(401);
        assertThat(unknown.status()).isEqualTo(401);
        assertThat(wrong.str("code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(unknown.str("code")).isEqualTo(wrong.str("code"));
    }

    @Test
    void cadastroNaoRevelaSeEmailJaExiste() {
        String[] admin = ensureAdmin();
        String adminToken = loginUi(admin[0], admin[1])[0];
        String clientId = createOpenApp(adminToken);
        String email = registerVerified(clientId);
        Resp again = post("/auth/register", Map.of("client_id", clientId, "email", email, "password", PASSWORD),
                browserHeaders(clientId, null));
        Resp fresh = post("/auth/register", Map.of("client_id", clientId, "email", uniqueEmail("novo"), "password",
                PASSWORD), browserHeaders(clientId, null));
        assertThat(again.status()).isEqualTo(fresh.status());
    }

    @Test
    void cadastroRecusaSenhaFracaComErroPorCampo() {
        String[] admin = ensureAdmin();
        String clientId = createOpenApp(loginUi(admin[0], admin[1])[0]);
        Resp r = post("/auth/register", Map.of("client_id", clientId, "email", uniqueEmail("fraca"), "password",
                "123456789012"), browserHeaders(clientId, null));
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.raw()).contains("password");
        assertThat((List<?>) r.json().get("errors")).isNotEmpty();
    }

    @Test
    void bloqueioTemporarioAposVariasFalhas() {
        String[] admin = ensureAdmin();
        String clientId = createOpenApp(loginUi(admin[0], admin[1])[0]);
        String email = registerVerified(clientId);
        Resp last = null;
        for (int i = 0; i < 6; i++) {
            last = post("/auth/login", Map.of("client_id", clientId, "email", email, "password", "errada-errada-" + i),
                    browserHeaders(clientId, null));
        }
        assertThat(last.status()).isEqualTo(401);
        assertThat(last.str("code")).isEqualTo("INVALID_CREDENTIALS");
        // mesmo com a senha correta, enquanto bloqueado
        Resp ok = post("/auth/login", Map.of("client_id", clientId, "email", email, "password", PASSWORD),
                browserHeaders(clientId, null));
        assertThat(ok.status()).isEqualTo(401);
        assertThat(ok.str("code")).isEqualTo(last.str("code"));
        assertThat(ok.json().containsKey("access_token")).isFalse();
        assertThat(ok.header("Retry-After")).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM login_lock WHERE key_hash = ? AND locked_until > now()",
                Integer.class, com.eridani.achernar.identity.shared.crypto.Sha256.hex(email.toLowerCase())))
                .isEqualTo(1);
    }
}
