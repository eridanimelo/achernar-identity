package com.eridani.achernar.identity.shared.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/**
 * Exclusão mútua entre instâncias para jobs agendados (travas consultivas de sessão do PostgreSQL).
 * A conexão da trava é dedicada e mantida durante a execução.
 */
@Component
public class JobLock {

    private final DataSource dataSource;

    public JobLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** @return {@code true} se este nó obteve a trava e executou; {@code false} se outro nó a detém. */
    public boolean runExclusive(String key, Runnable job) {
        long k = Db.hash64(key);
        try (Connection c = dataSource.getConnection()) {
            if (!tryLock(c, k)) {
                return false;
            }
            try {
                job.run();
                return true;
            } finally {
                unlock(c, k);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao obter trava consultiva do job " + key, e);
        }
    }

    private static boolean tryLock(Connection c, long key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            ps.setLong(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private static void unlock(Connection c, long key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            ps.setLong(1, key);
            ps.execute();
        }
    }
}
