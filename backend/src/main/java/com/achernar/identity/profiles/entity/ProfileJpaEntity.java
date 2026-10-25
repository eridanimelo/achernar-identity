package com.achernar.identity.profiles.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Formula;

/**
 * Mapeamento JPA da tabela {@code access_profile}. {@code userCount} é
 * calculado por {@link Formula}, preservando a contagem de vínculos que o SQL
 * original projetava.
 */
@Entity
@Table(name = "access_profile")
public class ProfileJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean system;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Formula("(SELECT count(*) FROM user_profile up WHERE up.profile_id = id)")
    private long userCount;

    protected ProfileJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSystem() {
        return system;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getUserCount() {
        return userCount;
    }
}
