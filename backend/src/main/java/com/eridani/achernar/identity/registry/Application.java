package com.eridani.achernar.identity.registry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Aplicação cliente registrada (RN-APP-*). Objeto de leitura imutável. */
public record Application(
        UUID id,
        String clientId,
        String name,
        String description,
        ClientType type,
        RegistryStatus status,
        boolean allowRegistration,
        boolean identityApiAccess,
        boolean googleEnabled,
        boolean facebookEnabled,
        Integer accessTokenTtlSeconds,
        String verifyEmailUrl,
        String resetPasswordUrl,
        String confirmEmailChangeUrl,
        boolean system,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<String> origins,
        List<String> redirectUris,
        List<AudienceGrant> audiences) {

    public boolean active() {
        return status == RegistryStatus.ACTIVE;
    }

    public boolean hasOrigin(String origin) {
        return origin != null && origins.contains(origin);
    }

    public boolean hasRedirectUri(String uri) {
        return uri != null && redirectUris.contains(uri);
    }
}
