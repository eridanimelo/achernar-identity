package com.achernar.identity.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeamento JPA da tabela {@code app_user}. Uso interno do repository: os
 * services continuam lendo {@link UserEntity}, que é a projeção pública.
 */
@Entity
@Table(name = "app_user")
public class UserJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
    }
}
