package com.achernar.identity.auth.repository;

import com.achernar.identity.auth.entity.Action;
import com.achernar.identity.auth.entity.ActionJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActionRepository extends JpaRepository<ActionJpaEntity, UUID> {
    @Modifying
    @Query(value = "INSERT INTO email_action(id,user_id,token_hash,kind,expires_at) "
            + "VALUES(:id,:userId,:hash,:kind,:expiresAt)", nativeQuery = true)
    void insert(@Param("id") UUID id, @Param("userId") UUID userId, @Param("hash") String hash,
            @Param("kind") String kind, @Param("expiresAt") Instant expiresAt);

    default void create(UUID userId, String hash, String kind, Instant expiry) {
        insert(UUID.randomUUID(), userId, hash, kind, expiry);
    }

    @Query("select a from ActionJpaEntity a where a.tokenHash=:hash and a.kind=:kind "
            + "and a.consumedAt is null and a.expiresAt > current_timestamp")
    Optional<ActionJpaEntity> findActive(@Param("hash") String hash, @Param("kind") String kind);

    default Optional<Action> find(String hash, String kind) {
        return findActive(hash, kind).map(action -> new Action(action.getId(), action.getUserId()));
    }

    @Modifying(clearAutomatically = true)
    @Query("update ActionJpaEntity a set a.consumedAt = current_timestamp where a.id=:id "
            + "and a.consumedAt is null and a.expiresAt > current_timestamp")
    int markConsumed(@Param("id") UUID id);

    default boolean consume(UUID id) {
        return markConsumed(id) == 1;
    }

    @Modifying(clearAutomatically = true)
    @Query("delete from ActionJpaEntity a where a.userId=:userId and a.kind=:kind")
    void invalidate(@Param("userId") UUID userId, @Param("kind") String kind);

    @Modifying(clearAutomatically = true)
    @Query("delete from ActionJpaEntity a where a.userId=:userId")
    void remove(@Param("userId") UUID userId);
}
