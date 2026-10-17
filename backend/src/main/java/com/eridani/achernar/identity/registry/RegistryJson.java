package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.Time;
import java.util.List;
import java.util.Map;

/** Representações JSON (snake_case) de aplicações e APIs. */
final class RegistryJson {

    private RegistryJson() {
    }

    static Map<String, Object> summary(ApplicationRepository.Summary s) {
        return Obj.of().with("id", s.id().toString()).with("client_id", s.clientId()).with("name", s.name())
                .with("type", s.type().name()).with("status", s.status().name())
                .with("allow_registration", s.allowRegistration()).with("identity_api_access", s.identityApiAccess())
                .with("created_at", Time.iso(s.createdAt()));
    }

    static Map<String, Object> application(Application a, List<ApplicationRepository.SecretRow> secrets) {
        Obj o = Obj.of().with("id", a.id().toString()).with("client_id", a.clientId()).with("name", a.name())
                .with("description", a.description()).with("type", a.type().name()).with("status", a.status().name())
                .with("allow_registration", a.allowRegistration()).with("identity_api_access", a.identityApiAccess())
                .with("google_enabled", a.googleEnabled()).with("facebook_enabled", a.facebookEnabled())
                .with("access_token_ttl_seconds", a.accessTokenTtlSeconds())
                .with("origins", a.origins()).with("redirect_uris", a.redirectUris())
                .with("verify_email_url", a.verifyEmailUrl()).with("reset_password_url", a.resetPasswordUrl())
                .with("confirm_email_change_url", a.confirmEmailChangeUrl())
                .with("audiences", a.audiences().stream()
                        .map(g -> Obj.of().with("audience", g.audience()).with("is_default", g.isDefault())
                                .with("active", g.apiActive())).toList())
                .with("is_system", a.system()).with("created_at", Time.iso(a.createdAt()))
                .with("updated_at", Time.iso(a.updatedAt())).with("version", a.version());
        if (secrets != null) {
            o.with("secrets", secrets.stream().map(r -> Obj.of().with("id", r.id().toString())
                    .with("created_at", Time.iso(r.createdAt())).with("last_used_at", Time.iso(r.lastUsedAt()))
                    .with("revoked_at", Time.iso(r.revokedAt()))).toList());
        }
        return o;
    }

    static Map<String, Object> api(ResourceApi a, List<ResourceApiRepository.CredentialRow> credentials) {
        Obj o = Obj.of().with("id", a.id().toString()).with("audience", a.audience()).with("name", a.name())
                .with("description", a.description()).with("status", a.status().name())
                .with("created_at", Time.iso(a.createdAt())).with("updated_at", Time.iso(a.updatedAt()))
                .with("version", a.version());
        if (credentials != null) {
            o.with("credentials", credentials.stream().map(r -> Obj.of().with("id", r.id().toString())
                    .with("created_at", Time.iso(r.createdAt())).with("last_used_at", Time.iso(r.lastUsedAt()))
                    .with("revoked_at", Time.iso(r.revokedAt()))).toList());
        }
        return o;
    }
}
