package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SocialLinkRepository {

    private static final String COLUMNS = "id, account_id, provider, provider_subject, provider_email, linked_at, "
            + "last_login_at";
    private static final RowMapper<SocialLink> MAPPER = (rs, i) -> new SocialLink(Db.uuid(rs, "id"),
            Db.uuid(rs, "account_id"), Provider.valueOf(rs.getString("provider")), rs.getString("provider_subject"),
            rs.getString("provider_email"), Db.instant(rs, "linked_at"), Db.instant(rs, "last_login_at"));

    private final JdbcTemplate jdbc;

    public SocialLinkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SocialLink> findByAccount(UUID accountId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM social_link WHERE account_id = ? ORDER BY linked_at",
                MAPPER, accountId);
    }

    public Optional<SocialLink> findByIdentity(Provider provider, String subject) {
        List<SocialLink> rows = jdbc.query("SELECT " + COLUMNS + " FROM social_link WHERE provider = ? "
                + "AND provider_subject = ?", MAPPER, provider.name(), subject);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<SocialLink> findByAccountAndProvider(UUID accountId, Provider provider) {
        List<SocialLink> rows = jdbc.query("SELECT " + COLUMNS + " FROM social_link WHERE account_id = ? "
                + "AND provider = ?", MAPPER, accountId, provider.name());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public int count(UUID accountId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM social_link WHERE account_id = ?", Integer.class,
                accountId);
        return n == null ? 0 : n;
    }

    /** Insere; retorna false se a identidade ou o par conta/provedor já existem. */
    public boolean insert(SocialLink l) {
        return jdbc.update("INSERT INTO social_link (id, account_id, provider, provider_subject, provider_email, "
                        + "linked_at, last_login_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                l.id(), l.accountId(), l.provider().name(), l.providerSubject(), l.providerEmail(),
                Db.ts(l.linkedAt()), Db.ts(l.lastLoginAt())) > 0;
    }

    public boolean delete(UUID accountId, Provider provider) {
        return jdbc.update("DELETE FROM social_link WHERE account_id = ? AND provider = ?", accountId,
                provider.name()) > 0;
    }

    public void deleteAll(UUID accountId) {
        jdbc.update("DELETE FROM social_link WHERE account_id = ?", accountId);
    }

    public void touchLogin(UUID id, Instant now) {
        jdbc.update("UPDATE social_link SET last_login_at = ? WHERE id = ?", Db.ts(now), id);
    }

}
