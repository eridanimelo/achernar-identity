package com.achernar.identity.users;
import java.util.*;
import java.sql.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class UserRepository {
    private static final String PROFILE_COLUMNS = "COALESCE((SELECT array_agg(p.code ORDER BY p.code) FROM user_profile up JOIN access_profile p ON p.id=up.profile_id WHERE up.user_id=u.id AND p.active),'{}') AS profiles, COALESCE((SELECT array_agg(p.code ORDER BY p.code) FROM user_profile up JOIN access_profile p ON p.id=up.profile_id WHERE up.user_id=u.id),'{}') AS assigned_profiles";
    private static final String PUBLIC_COLUMNS = "u.id,u.name,u.email,u.status,u.verified,u.created_at,"+PROFILE_COLUMNS;
    private static final String COLUMNS = PUBLIC_COLUMNS+",u.password_hash,u.session_version";
    private final NamedParameterJdbcTemplate db;
    private static List<String> codes(ResultSet r,String column)throws SQLException {return List.of((String[])r.getArray(column).getArray());}
    private final RowMapper<UserEntity> mapper = (r,n) -> {
        var profiles=codes(r,"profiles");
        return new UserEntity(r.getObject("id",UUID.class),r.getString("name"),r.getString("email"),r.getString("password_hash"),profiles.contains("ADMIN")?"ADMIN":"USER",
            r.getString("status"),r.getBoolean("verified"),r.getLong("session_version"),r.getTimestamp("created_at").toInstant(),profiles,codes(r,"assigned_profiles"));
    };
    public UserRepository(NamedParameterJdbcTemplate db) {this.db=db;}
    public Optional<UserEntity> byEmail(String email) {return db.query("SELECT "+COLUMNS+" FROM app_user u WHERE u.email=:email",Map.of("email",email),mapper).stream().findFirst();}
    public Optional<UserEntity> byId(UUID id) {return db.query("SELECT "+COLUMNS+" FROM app_user u WHERE u.id=:id",Map.of("id",id),mapper).stream().findFirst();}
    public Optional<UserEntity> lock(UUID id) {return db.query("SELECT "+COLUMNS+" FROM app_user u WHERE u.id=:id FOR UPDATE OF u",Map.of("id",id),mapper).stream().findFirst();}
    public boolean create(UUID id,String name,String email,String hash,String role,boolean verified) {
        boolean created=db.update("INSERT INTO app_user(id,name,email,password_hash,status,verified) VALUES(:id,:name,:email,:hash,'ACTIVE',:verified) ON CONFLICT(email) DO NOTHING",Map.of("id",id,"name",name,"email",email,"hash",hash,"verified",verified))==1;
        if(created) db.update("INSERT INTO user_profile(user_id,profile_id) SELECT :id,id FROM access_profile WHERE code=:code AND active",Map.of("id",id,"code",role));
        return created;
    }
    public void rename(UUID id,String name) {db.update("UPDATE app_user SET name=:name WHERE id=:id",Map.of("id",id,"name",name));}
    public void verify(UUID id) {db.update("UPDATE app_user SET verified=true WHERE id=:id",Map.of("id",id));}
    public void reset(UUID id,String hash) {db.update("UPDATE app_user SET password_hash=:hash,session_version=session_version+1 WHERE id=:id",Map.of("id",id,"hash",hash));}
    public void revoke(UUID id) {db.update("UPDATE app_user SET session_version=session_version+1 WHERE id=:id",Map.of("id",id));}
    public void revokeProfile(UUID profileId) {db.update("UPDATE app_user SET session_version=session_version+1 WHERE id IN (SELECT user_id FROM user_profile WHERE profile_id=:id)",Map.of("id",profileId));}
    public void adminLock() {db.getJdbcTemplate().execute("SELECT pg_advisory_xact_lock(71423001)");}
    public long activeAdmins() {return db.queryForObject("SELECT count(*) FROM app_user u JOIN user_profile up ON up.user_id=u.id JOIN access_profile p ON p.id=up.profile_id WHERE p.code='ADMIN' AND p.active AND u.status='ACTIVE' AND u.verified",Map.of(),Long.class);}
    public void assign(UUID id,List<UUID> profiles) {
        db.update("DELETE FROM user_profile WHERE user_id=:id",Map.of("id",id));
        for(UUID profile:profiles) db.update("INSERT INTO user_profile(user_id,profile_id) VALUES(:id,:profile)",Map.of("id",id,"profile",profile));
    }
    public void update(UUID id,String name,String email,String status,boolean verified) {db.update("UPDATE app_user SET name=:name,email=:email,status=:status,verified=:verified,session_version=session_version+1 WHERE id=:id",Map.of("id",id,"name",name,"email",email,"status",status,"verified",verified));}
    public void delete(UUID id) {
        db.update("DELETE FROM user_profile WHERE user_id=:id",Map.of("id",id));
        db.update("UPDATE app_user SET status='DELETED',name='Usuário removido',email=:email,password_hash='disabled',session_version=session_version+1 WHERE id=:id",Map.of("id",id,"email",id+"@deleted.invalid"));
    }
    private String term(String q) {String slash=Character.toString(92);return "%"+q.toLowerCase(Locale.ROOT).replace(slash,slash+slash).replace("%",slash+"%").replace("_",slash+"_")+"%";}
    public List<UserDto> list(String q,int page,int size) {
        return db.query("SELECT "+PUBLIC_COLUMNS+" FROM app_user u WHERE u.status<>'DELETED' AND (lower(u.name) LIKE :q OR lower(u.email) LIKE :q) ORDER BY u.created_at DESC,u.id LIMIT :size OFFSET :offset",
            Map.of("q",term(q),"size",size,"offset",(long)page*size),(r,n)->{var profiles=codes(r,"profiles");return new UserDto(r.getObject("id",UUID.class),r.getString("name"),r.getString("email"),profiles.contains("ADMIN")?"ADMIN":"USER",r.getString("status"),r.getBoolean("verified"),r.getTimestamp("created_at").toInstant(),profiles,codes(r,"assigned_profiles"));});
    }
    public long count(String q) {return db.queryForObject("SELECT count(*) FROM app_user WHERE status<>'DELETED' AND (lower(name) LIKE :q OR lower(email) LIKE :q)",Map.of("q",term(q)),Long.class);}
}
