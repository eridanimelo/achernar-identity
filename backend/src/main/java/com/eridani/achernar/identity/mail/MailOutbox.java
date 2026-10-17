package com.eridani.achernar.identity.mail;

import com.eridani.achernar.identity.shared.crypto.MasterKeyCipher;
import com.eridani.achernar.identity.shared.json.MiniJson;
import com.eridani.achernar.identity.shared.util.UuidV7;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox transacional (D-18, emails.md §1). {@link #enqueue} <b>exige</b> transação ativa: a mensagem é gravada
 * junto da operação de negócio (RN-EML-02) e some com um rollback.
 */
@Service
public class MailOutbox {

    static final String PURPOSE = "mail-outbox";

    public record Claimed(UUID id, MailTemplate template, String locale, String toAddress, int attempts,
                          Map<String, Object> payload) {
    }

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final MasterKeyCipher cipher;

    public MailOutbox(JdbcTemplate jdbc, Clock clock, MasterKeyCipher cipher) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.cipher = cipher;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID enqueue(MailTemplate template, String locale, String toAddress, UUID accountId, UUID applicationId,
                        Map<String, String> vars) {
        Instant now = clock.instant();
        UUID id = UuidV7.generate(now);
        byte[] payload = cipher.encryptString(MiniJson.write(vars), PURPOSE);
        jdbc.update("INSERT INTO email_outbox (id, template, locale, to_address, account_id, application_id, "
                        + "payload_enc, status, attempts, next_attempt_at, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?, ?)",
                id, template.id(), locale, toAddress, accountId, applicationId, payload,
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    /** Reserva um lote vencido (lease) com {@code SKIP LOCKED}; múltiplas instâncias não colidem (CA-FT13-06). */
    @Transactional
    public List<Claimed> claim(int batch, Duration lease) {
        Instant now = clock.instant();
        return jdbc.query("UPDATE email_outbox SET locked_until = ? WHERE id IN ("
                        + "SELECT id FROM email_outbox WHERE status = 'PENDING' AND next_attempt_at <= ? "
                        + "AND (locked_until IS NULL OR locked_until < ?) ORDER BY next_attempt_at LIMIT ? "
                        + "FOR UPDATE SKIP LOCKED) "
                        + "RETURNING id, template, locale, to_address, attempts, payload_enc",
                (rs, i) -> {
                    byte[] enc = rs.getBytes("payload_enc");
                    Map<String, Object> payload = enc == null ? Map.of()
                            : MiniJson.parseObject(cipher.decryptString(enc, PURPOSE));
                    return new Claimed((UUID) rs.getObject("id"), MailTemplate.fromId(rs.getString("template")),
                            rs.getString("locale"), rs.getString("to_address"), rs.getInt("attempts"), payload);
                },
                Timestamp.from(now.plus(lease)), Timestamp.from(now), Timestamp.from(now), batch);
    }

    @Transactional
    public void markSent(UUID id) {
        jdbc.update("UPDATE email_outbox SET status = 'SENT', payload_enc = NULL, sent_at = ?, locked_until = NULL, "
                + "attempts = attempts + 1, last_error = NULL WHERE id = ?", Timestamp.from(clock.instant()), id);
    }

    @Transactional
    public void markRetry(UUID id, Duration delay, String safeError) {
        jdbc.update("UPDATE email_outbox SET attempts = attempts + 1, next_attempt_at = ?, locked_until = NULL, "
                        + "last_error = ? WHERE id = ?",
                Timestamp.from(clock.instant().plus(delay)), truncate(safeError), id);
    }

    @Transactional
    public void markFailed(UUID id, String safeError) {
        jdbc.update("UPDATE email_outbox SET status = 'FAILED', payload_enc = NULL, attempts = attempts + 1, "
                + "locked_until = NULL, last_error = ? WHERE id = ?", truncate(safeError), id);
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        Long n = jdbc.queryForObject("SELECT count(*) FROM email_outbox WHERE status = 'PENDING'", Long.class);
        return n == null ? 0 : n;
    }

    @Transactional
    public int purge(Instant sentCutoff, Instant failedCutoff, int batch) {
        return jdbc.update("DELETE FROM email_outbox WHERE id IN (SELECT id FROM email_outbox WHERE "
                        + "(status = 'SENT' AND created_at < ?) OR (status = 'FAILED' AND created_at < ?) LIMIT ?)",
                Timestamp.from(sentCutoff), Timestamp.from(failedCutoff), batch);
    }

    @Transactional
    public int purgeForAccount(UUID accountId) {
        return jdbc.update("DELETE FROM email_outbox WHERE account_id = ?", accountId);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
