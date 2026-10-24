CREATE TABLE access_profile (
 id UUID PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE CHECK(code ~ '^[A-Z][A-Z0-9_]{1,49}$'),
 name VARCHAR(100) NOT NULL, description VARCHAR(500) NOT NULL DEFAULT '', active BOOLEAN NOT NULL DEFAULT TRUE,
 system BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO access_profile(id,code,name,description,system) VALUES
 ('00000000-0000-0000-0000-000000000001','ADMIN','Administrador','Administração de usuários e perfis',true),
 ('00000000-0000-0000-0000-000000000002','USER','Usuário','Acesso padrão à conta',true);
CREATE TABLE user_profile (
 user_id UUID NOT NULL REFERENCES app_user(id), profile_id UUID NOT NULL REFERENCES access_profile(id),
 PRIMARY KEY(user_id,profile_id)
);
CREATE INDEX user_profile_by_profile ON user_profile(profile_id,user_id);
INSERT INTO user_profile(user_id,profile_id) SELECT u.id,p.id FROM app_user u JOIN access_profile p ON p.code=u.role WHERE u.status<>'DELETED';
UPDATE app_user SET session_version=session_version+1;
ALTER TABLE app_user DROP COLUMN role;
ALTER TABLE email_action DROP CONSTRAINT email_action_kind_check;
ALTER TABLE email_action ADD CONSTRAINT email_action_kind_check CHECK(kind IN ('VERIFY','RESET','INVITE'));
