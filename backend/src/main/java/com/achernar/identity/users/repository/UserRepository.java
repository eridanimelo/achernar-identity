package com.achernar.identity.users.repository;

import com.achernar.identity.users.entity.UserEntity;
import com.achernar.identity.users.entity.UserJpaEntity;
import com.achernar.identity.users.entity.UserView;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserJpaEntity, UUID>, UserAdvisoryLock {
    String COLUMNS = "u.id AS \"id\",u.name AS \"name\",u.email AS \"email\",u.password_hash AS \"passwordHash\","
            + "u.status AS \"status\",u.verified AS \"verified\",u.session_version AS \"sessionVersion\","
            + "u.created_at AS \"createdAt\","
            + "COALESCE((SELECT string_agg(p.code, ',' ORDER BY p.code) FROM user_profile up "
            + "JOIN access_profile p ON p.id=up.profile_id WHERE up.user_id=u.id AND p.active),'') AS \"profiles\","
            + "COALESCE((SELECT string_agg(p.code, ',' ORDER BY p.code) FROM user_profile up "
            + "JOIN access_profile p ON p.id=up.profile_id WHERE up.user_id=u.id),'') AS \"assignedProfiles\"";

    interface UserRow {
        UUID getId();

        String getName();

        String getEmail();

        String getPasswordHash();

        String getStatus();

        boolean isVerified();

        long getSessionVersion();

        Instant getCreatedAt();

        String getProfiles();

        String getAssignedProfiles();
    }

    @Query(value = "SELECT " + COLUMNS + " FROM app_user u WHERE u.email=:email", nativeQuery = true)
    Optional<UserRow> findRowByEmail(@Param("email") String email);

    default Optional<UserEntity> byEmail(String email) {
        return findRowByEmail(email).map(UserRepository::toEntity);
    }

    @Query(value = "SELECT " + COLUMNS + " FROM app_user u WHERE u.id=:id", nativeQuery = true)
    Optional<UserRow> findRowById(@Param("id") UUID id);

    default Optional<UserEntity> byId(UUID id) {
        return findRowById(id).map(UserRepository::toEntity);
    }

    @Query(value = "SELECT " + COLUMNS + " FROM app_user u WHERE u.id=:id FOR UPDATE OF u", nativeQuery = true)
    Optional<UserRow> findRowForUpdate(@Param("id") UUID id);

    default Optional<UserEntity> lock(UUID id) {
        return findRowForUpdate(id).map(UserRepository::toEntity);
    }

    @Modifying
    @Query(value = "INSERT INTO app_user(id,name,email,password_hash,status,verified) "
            + "VALUES(:id,:name,:email,:hash,'ACTIVE',:verified) ON CONFLICT(email) DO NOTHING", nativeQuery = true)
    int insert(@Param("id") UUID id, @Param("name") String name, @Param("email") String email,
            @Param("hash") String hash, @Param("verified") boolean verified);

    @Modifying
    @Query(value = "INSERT INTO user_profile(user_id,profile_id) "
            + "SELECT :id,id FROM access_profile WHERE code=:code AND active", nativeQuery = true)
    void assignRole(@Param("id") UUID id, @Param("code") String code);

    default boolean create(UUID id, String name, String email, String hash, String role, boolean verified) {
        if (insert(id, name, email, hash, verified) != 1)
            return false;
        assignRole(id, role);
        return true;
    }

    @Modifying(clearAutomatically = true)
    @Query("update UserJpaEntity u set u.name=:name where u.id=:id")
    void rename(@Param("id") UUID id, @Param("name") String name);

    @Modifying(clearAutomatically = true)
    @Query("update UserJpaEntity u set u.verified=true where u.id=:id")
    void verify(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("update UserJpaEntity u set u.passwordHash=:hash, u.sessionVersion=u.sessionVersion+1 where u.id=:id")
    void reset(@Param("id") UUID id, @Param("hash") String hash);

    @Modifying(clearAutomatically = true)
    @Query("update UserJpaEntity u set u.sessionVersion=u.sessionVersion+1 where u.id=:id")
    void revoke(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE app_user SET session_version=session_version+1 "
            + "WHERE id IN (SELECT user_id FROM user_profile WHERE profile_id=:id)", nativeQuery = true)
    void revokeProfile(@Param("id") UUID profileId);

    @Query(value = "SELECT count(*) FROM app_user u JOIN user_profile up ON up.user_id=u.id "
            + "JOIN access_profile p ON p.id=up.profile_id "
            + "WHERE p.code='ADMIN' AND p.active AND u.status='ACTIVE' AND u.verified", nativeQuery = true)
    long activeAdmins();

    @Modifying
    @Query(value = "DELETE FROM user_profile WHERE user_id=:id", nativeQuery = true)
    void clearProfiles(@Param("id") UUID id);

    @Modifying
    @Query(value = "INSERT INTO user_profile(user_id,profile_id) VALUES(:id,:profile)", nativeQuery = true)
    void insertProfile(@Param("id") UUID id, @Param("profile") UUID profile);

    default void assign(UUID id, List<UUID> profiles) {
        clearProfiles(id);
        profiles.forEach(profile -> insertProfile(id, profile));
    }

    @Modifying(clearAutomatically = true)
    @Query("update UserJpaEntity u set u.name=:name, u.email=:email, u.status=:status, u.verified=:verified, "
            + "u.sessionVersion=u.sessionVersion+1 where u.id=:id")
    void update(@Param("id") UUID id, @Param("name") String name, @Param("email") String email,
            @Param("status") String status, @Param("verified") boolean verified);

    @Modifying(clearAutomatically = true)
    @Query("update UserJpaEntity u set u.status='DELETED', u.name='Usuário removido', u.email=:email, "
            + "u.passwordHash='disabled', u.sessionVersion=u.sessionVersion+1 where u.id=:id")
    void softDelete(@Param("id") UUID id, @Param("email") String email);

    default void delete(UUID id) {
        clearProfiles(id);
        softDelete(id, id + "@deleted.invalid");
    }

    @Query(value = "SELECT " + COLUMNS + " FROM app_user u WHERE u.status<>'DELETED' "
            + "AND (lower(u.name) LIKE :q OR lower(u.email) LIKE :q) "
            + "ORDER BY u.created_at DESC,u.id LIMIT :size OFFSET :offset", nativeQuery = true)
    List<UserRow> listRows(@Param("q") String q, @Param("size") int size, @Param("offset") long offset);

    default List<UserView> list(String q, int page, int size) {
        return listRows(term(q), size, (long) page * size).stream().map(UserRepository::toView).toList();
    }

    @Query(value = "SELECT count(*) FROM app_user WHERE status<>'DELETED' "
            + "AND (lower(name) LIKE :q OR lower(email) LIKE :q)", nativeQuery = true)
    long countByTerm(@Param("q") String q);

    default long count(String q) {
        return countByTerm(term(q));
    }

    private static UserEntity toEntity(UserRow row) {
        var profiles = codes(row.getProfiles());
        return new UserEntity(row.getId(), row.getName(), row.getEmail(), row.getPasswordHash(),
                profiles.contains("ADMIN") ? "ADMIN" : "USER", row.getStatus(), row.isVerified(),
                row.getSessionVersion(), row.getCreatedAt(), profiles, codes(row.getAssignedProfiles()));
    }

    private static UserView toView(UserRow row) {
        var profiles = codes(row.getProfiles());
        return new UserView(row.getId(), row.getName(), row.getEmail(), profiles.contains("ADMIN") ? "ADMIN" : "USER",
                row.getStatus(), row.isVerified(), row.getCreatedAt(), profiles, codes(row.getAssignedProfiles()));
    }

    private static List<String> codes(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.split(","));
    }

    private static String term(String q) {
        String slash = Character.toString(92);
        return "%" + q.toLowerCase(Locale.ROOT).replace(slash, slash + slash).replace("%", slash + "%").replace("_",
                slash + "_") + "%";
    }
}
