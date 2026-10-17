package com.eridani.achernar.identity.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminProtectionIT extends AbstractIT {

    @Test
    void contaComumNaoAcessaAreaAdministrativa() {
        String[] admin = ensureAdmin();
        String clientId = createOpenApp(loginUi(admin[0], admin[1])[0]);
        String email = registerVerified(clientId);
        Resp login = post("/auth/login", Map.of("client_id", clientId, "email", email, "password", PASSWORD),
                browserHeaders(clientId, null));
        Resp r = bearerGet("/api/v1/admin/accounts", login.str("access_token"));
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.header("Content-Type")).contains("application/problem+json");
    }

    @Test
    void administradorListaContasMasNaoEnxergaHashDeSenha() {
        String[] admin = ensureAdmin();
        String token = loginUi(admin[0], admin[1])[0];
        Resp r = bearerGet("/api/v1/admin/accounts", token);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.raw()).doesNotContain("$argon2");
        assertThat(r.raw()).doesNotContain("password_hash");
    }

    @Test
    void ultimoAdministradorNaoPodeSerDesativadoNemRebaixado() {
        String[] admin = ensureAdmin();
        String[] s = loginUi(admin[0], admin[1]);
        String token = s[0];
        String id = jdbc.queryForObject("SELECT id::text FROM account WHERE email_normalized = ?", String.class,
                admin[0].toLowerCase());
        Integer admins = jdbc.queryForObject("SELECT count(*) FROM account WHERE role='ADMIN' AND status='ACTIVE'",
                Integer.class);
        assertThat(admins).isEqualTo(1);
        assertThat(bearerPost("/api/v1/account/reauth", Map.of("password", admin[1]), token).status()).isEqualTo(200);
        // CA-FT11-06: auto-desativação tem resposta própria.
        Resp disable = bearerPost("/api/v1/admin/accounts/" + id + "/disable", Map.of("reason", "teste"), token);
        assertThat(disable.status()).isEqualTo(403);
        assertThat(disable.str("code")).isEqualTo("FORBIDDEN");
        // CA-FT11-07: rebaixar o último administrador não pode eliminar a administração.
        Long version = jdbc.queryForObject("SELECT version FROM account WHERE id::text = ?", Long.class, id);
        Resp demote = send("PUT", "/api/v1/admin/accounts/" + id + "/role",
                Map.of("role", "USER", "version", version), "Authorization", "Bearer " + token);
        assertThat(demote.status()).isEqualTo(409);
        assertThat(demote.str("code")).isEqualTo("LAST_ADMIN_PROTECTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM account WHERE role='ADMIN' AND status='ACTIVE'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void acaoSensivelExigeReautenticacaoRecente() {
        String[] admin = ensureAdmin();
        String token = loginUi(admin[0], admin[1])[0];
        jdbc.update("UPDATE session SET last_reauth_at = now() - interval '1 hour'");
        String qualquer = "00000000-0000-7000-8000-000000000000";
        Resp r = bearerPost("/api/v1/admin/accounts/" + qualquer + "/disable", Map.of("reason", "x"), token);
        assertThat(r.status()).isIn(401, 403);
        assertThat(r.str("code")).isEqualTo("REAUTH_REQUIRED");
    }
}
