package com.achernar.identity.profiles.repository;

import com.achernar.identity.profiles.entity.ProfileJpaEntity;
import com.achernar.identity.profiles.entity.ProfileView;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<ProfileJpaEntity, UUID> {
    @Query("select p from ProfileJpaEntity p where p.id = :id")
    Optional<ProfileJpaEntity> findRowById(@Param("id") UUID id);

    default Optional<ProfileView> byId(UUID id) {
        return findRowById(id).map(ProfileRepository::toView);
    }

    @Query("select p from ProfileJpaEntity p where p.code in :codes and p.active = true")
    List<ProfileJpaEntity> findActiveByCodes(@Param("codes") List<String> codes);

    default List<ProfileView> resolve(List<String> codes) {
        return findActiveByCodes(codes).stream().map(ProfileRepository::toView).toList();
    }

    @Modifying
    @Query(value = "INSERT INTO access_profile(id,code,name,description) "
            + "VALUES(:id,:code,:name,:description) ON CONFLICT(code) DO NOTHING", nativeQuery = true)
    int insert(@Param("id") UUID id, @Param("code") String code, @Param("name") String name,
            @Param("description") String description);

    default boolean create(UUID id, String code, String name, String description) {
        return insert(id, code, name, description) == 1;
    }

    @Modifying(clearAutomatically = true)
    @Query("update ProfileJpaEntity p set p.name = :name, p.description = :description, p.active = :active "
            + "where p.id = :id")
    void update(@Param("id") UUID id, @Param("name") String name, @Param("description") String description,
            @Param("active") boolean active);

    @Modifying(clearAutomatically = true)
    @Query("delete from ProfileJpaEntity p where p.id = :id")
    void delete(@Param("id") UUID id);

    @Query("select p from ProfileJpaEntity p where (:active is null or p.active = :active) "
            + "and (lower(p.name) like :q or lower(p.code) like :q) order by p.code, p.id")
    List<ProfileJpaEntity> findPage(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    default List<ProfileView> list(String q, Boolean active, int page, int size) {
        return findPage(term(q), active, PageRequest.of(page, size)).stream().map(ProfileRepository::toView).toList();
    }

    @Query("select count(p) from ProfileJpaEntity p where (:active is null or p.active = :active) "
            + "and (lower(p.name) like :q or lower(p.code) like :q)")
    long count(@Param("q") String q, @Param("active") Boolean active);

    private static ProfileView toView(ProfileJpaEntity profile) {
        return new ProfileView(profile.getId(), profile.getCode(), profile.getName(), profile.getDescription(),
                profile.isActive(), profile.isSystem(), profile.getUserCount(), profile.getCreatedAt());
    }

    private static String term(String q) {
        String slash = Character.toString(92);
        return "%" + q.toLowerCase(Locale.ROOT).replace(slash, slash + slash).replace("%", slash + "%").replace("_",
                slash + "_") + "%";
    }
}
