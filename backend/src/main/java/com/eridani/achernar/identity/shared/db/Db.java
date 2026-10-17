package com.eridani.achernar.identity.shared.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Utilitários de acesso JDBC (conversões de tipos e travas consultivas). */
public final class Db {

    private Db() {
    }

    public static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    public static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }

    public static UUID uuid(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        if (o == null) {
            return null;
        }
        return o instanceof UUID u ? u : UUID.fromString(o.toString());
    }

    public static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    /** Trava consultiva transacional (liberada no fim da transação). */
    public static void advisoryXactLock(JdbcTemplate jdbc, String key) {
        jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> { }, hash64(key));
    }

    /** Trava consultiva de sessão não bloqueante (jobs). Retorna se obteve. */
    public static boolean tryAdvisoryXactLock(JdbcTemplate jdbc, String key) {
        Boolean ok = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, hash64(key));
        return Boolean.TRUE.equals(ok);
    }

    /** Hash determinístico de 64 bits (FNV-1a) para chaves de travas consultivas. */
    public static long hash64(String key) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < key.length(); i++) {
            h ^= key.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    /** Escapa curingas de LIKE ({@code % _ \}). */
    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
