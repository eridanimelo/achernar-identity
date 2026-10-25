package com.achernar.identity.users.repository;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fragmento do repository para o advisory lock transacional do PostgreSQL, que
 * não é expressável em JPQL.
 */
public class UserAdvisoryLockImpl implements UserAdvisoryLock {
    private final JdbcTemplate jdbc;

    public UserAdvisoryLockImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void adminLock() {
        jdbc.execute("SELECT pg_advisory_xact_lock(71423001)");
    }
}
