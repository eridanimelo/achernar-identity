package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    static final String COLUMNS = "id, email_display, email_normalized, status, role, email_verified_at, locale, "
            + "disabled_at, disabled_reason, anonymized_at, created_at, updated_at, version";

    public static final RowMapper<Account> MAPPER = (rs, i) -> new Account(Db.uuid(rs, "id"),
            rs.getString("email_display"), rs.getString("email_normalized"),
            AccountStatus.valueOf(rs.getString("status")), AccountRole.valueOf(rs.getString("role")),
            Db.instant(rs, "email_verified_at"), rs.getString("locale"), Db.instant(rs, "disabled_at"),
            rs.getString("disabled_reason"), Db.instant(rs, "anonymized_at"), Db.instant(rs, "created_at"),
            Db.instant(rs, "updated_at"), rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Account> findById(UUID id) {
        return one("SELECT " + COLUMNS + " FROM account WHERE id = ?", id);
    }

    public Optional<Account> findByEmail(String emailNormalized) {
        return one("SELECT " + COLUMNS + " FROM account WHERE email_normalized = ?", emailNormalized);
    }

    /** Leitura com trava de linha (fluxos que decidem sobre o estado atual). */
    public Optional<Account> findByIdForUpdate(UUID id) {
        return one("SELECT " + COLUMNS + " FROM account WHERE id = ? FOR UPDATE", id);
    }

    private Optional<Account> one(String sql, Object arg) {
        List<Account> rows = jdbc.query(sql, MAPPER, arg);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Insere se o e-mail ainda não existe; {@code false} em conflito de unicidade (D-43). */
    public boolean insertIfAbsent(Account a) {
        return jdbc.update("INSERT INTO account (id, email_display, email_normalized, status, role, email_verified_at, "
                        + "locale, created_at, updated_at, version) VALUES (?,?,?,?,?,?,?,?,?,0) "
                        + "ON CONFLICT (email_normalized) DO NOTHING",
                a.id(), a.emailDisplay(), a.emailNormalized(), a.status().name(), a.role().name(),
                Db.ts(a.emailVerifiedAt()), a.locale(), Db.ts(a.createdAt()), Db.ts(a.updatedAt())) > 0;
    }

    /** PENDING_VERIFICATION → ACTIVE. */
    public boolean activate(UUID id, Instant now) {
        return jdbc.update("UPDATE account SET status = 'ACTIVE', email_verified_at = ?, updated_at = ?, "
                + "version = version + 1 WHERE id = ? AND status = 'PENDING_VERIFICATION'", Db.ts(now), Db.ts(now), id) > 0;
    }

    public boolean disable(UUID id, String reason, Instant now) {
        return jdbc.update("UPDATE account SET status = 'DISABLED', disabled_at = ?, disabled_reason = ?, "
                + "updated_at = ?, version = version + 1 WHERE id = ? AND status = 'ACTIVE'",
                Db.ts(now), reason, Db.ts(now), id) > 0;
    }

    public boolean enable(UUID id, Instant now) {
        return jdbc.update("UPDATE account SET status = 'ACTIVE', disabled_at = NULL, disabled_reason = NULL, "
                + "updated_at = ?, version = version + 1 WHERE id = ? AND status = 'DISABLED' "
                + "AND anonymized_at IS NULL", Db.ts(now), id) > 0;
    }

    /** Altera o e-mail; retorna false se o novo endereço colidiu (unicidade revalidada). */
    public boolean updateEmail(UUID id, String display, String normalized, Instant now) {
        return jdbc.update("UPDATE account SET email_display = ?, email_normalized = ?, updated_at = ?, "
                        + "version = version + 1 WHERE id = ? AND NOT EXISTS (SELECT 1 FROM account o "
                        + "WHERE o.email_normalized = ? AND o.id <> ?)",
                display, normalized, Db.ts(now), id, normalized, id) > 0;
    }

    public boolean updateRole(UUID id, AccountRole role, long expectedVersion, Instant now) {
        return jdbc.update("UPDATE account SET role = ?, updated_at = ?, version = version + 1 "
                + "WHERE id = ? AND version = ?", role.name(), Db.ts(now), id, expectedVersion) > 0;
    }

    public void updateLocale(UUID id, String locale, Instant now) {
        jdbc.update("UPDATE account SET locale = ?, updated_at = ? WHERE id = ?", locale, Db.ts(now), id);
    }

    public void anonymize(UUID id, String anonEmail, Instant now) {
        jdbc.update("UPDATE account SET email_display = ?, email_normalized = ?, anonymized_at = ?, updated_at = ?, "
                + "version = version + 1 WHERE id = ?", anonEmail, anonEmail, Db.ts(now), Db.ts(now), id);
    }

    public int deletePending(UUID id) {
        return jdbc.update("DELETE FROM account WHERE id = ? AND status = 'PENDING_VERIFICATION'", id);
    }

    public long countActiveAdmins() {
        Long n = jdbc.queryForObject("SELECT count(*) FROM account WHERE role = 'ADMIN' AND status = 'ACTIVE'",
                Long.class);
        return n == null ? 0 : n;
    }

    public boolean hasActiveAdmin() {
        Boolean b = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM account WHERE role = 'ADMIN' AND status = 'ACTIVE')", Boolean.class);
        return Boolean.TRUE.equals(b);
    }

    /** Insere administrador inicial (setup); retorna false se o e-mail já existe. */
    public boolean insertAdmin(Account a) {
        return insertIfAbsent(a);
    }

    // retenção
    public List<UUID> pendingOlderThan(Instant cutoff, int batch) {
        return jdbc.query("SELECT id FROM account WHERE status = 'PENDING_VERIFICATION' AND created_at < ? "
                + "ORDER BY created_at LIMIT ?", (rs, i) -> Db.uuid(rs, "id"), Db.ts(cutoff), batch);
    }
}
