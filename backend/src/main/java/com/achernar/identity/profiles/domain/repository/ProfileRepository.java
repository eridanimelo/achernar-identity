package com.achernar.identity.profiles.domain.repository;

import com.achernar.identity.profiles.domain.model.ProfileView;
import java.util.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
    private static final String COLUMNS = "p.id,p.code,p.name,p.description,p.active,p.system,p.created_at,(SELECT count(*) FROM user_profile up WHERE up.profile_id=p.id) AS user_count";
    private final NamedParameterJdbcTemplate db;
    private final RowMapper<ProfileView> mapper = (r, n) -> new ProfileView(r.getObject("id", UUID.class),
            r.getString("code"), r.getString("name"), r.getString("description"), r.getBoolean("active"),
            r.getBoolean("system"), r.getLong("user_count"), r.getTimestamp("created_at").toInstant());

    public ProfileRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public Optional<ProfileView> byId(UUID id) {
        return db.query("SELECT " + COLUMNS + " FROM access_profile p WHERE p.id=:id", Map.of("id", id), mapper)
                .stream().findFirst();
    }

    public List<ProfileView> resolve(List<String> codes) {
        return db.query("SELECT " + COLUMNS + " FROM access_profile p WHERE p.code IN (:codes) AND p.active",
                Map.of("codes", codes), mapper);
    }

    public boolean create(UUID id, String code, String name, String description) {
        return db.update(
                "INSERT INTO access_profile(id,code,name,description) VALUES(:id,:code,:name,:description) ON CONFLICT(code) DO NOTHING",
                Map.of("id", id, "code", code, "name", name, "description", description)) == 1;
    }

    public void update(UUID id, String name, String description, boolean active) {
        db.update("UPDATE access_profile SET name=:name,description=:description,active=:active WHERE id=:id",
                Map.of("id", id, "name", name, "description", description, "active", active));
    }

    public void delete(UUID id) {
        db.update("DELETE FROM access_profile WHERE id=:id", Map.of("id", id));
    }

    private Map<String, Object> params(String q, Boolean active) {
        String slash = Character.toString(92);
        var args = new HashMap<String, Object>();
        args.put("q", "%" + q.toLowerCase(Locale.ROOT).replace(slash, slash + slash).replace("%", slash + "%")
                .replace("_", slash + "_") + "%");
        args.put("active", active);
        return args;
    }

    private static final String FILTER = " WHERE (CAST(:active AS boolean) IS NULL OR p.active=:active) AND (lower(p.name) LIKE :q OR lower(p.code) LIKE :q)";

    public List<ProfileView> list(String q, Boolean active, int page, int size) {
        var args = params(q, active);
        args.put("size", size);
        args.put("offset", (long) page * size);
        return db.query("SELECT " + COLUMNS + " FROM access_profile p" + FILTER
                + " ORDER BY p.code,p.id LIMIT :size OFFSET :offset", args, mapper);
    }

    public long count(String q, Boolean active) {
        return db.queryForObject("SELECT count(*) FROM access_profile p" + FILTER, params(q, active), Long.class);
    }
}
