package com.eridani.achernar.identity.registry;

import java.util.List;

/** Dados de cadastro/alteração de uma aplicação (já tipados; validados por {@link ApplicationValidator}). */
public record ApplicationInput(
        String clientId,
        String name,
        String description,
        String type,
        String status,
        List<String> origins,
        List<String> redirectUris,
        String verifyEmailUrl,
        String resetPasswordUrl,
        String confirmEmailChangeUrl,
        boolean allowRegistration,
        boolean identityApiAccess,
        boolean googleEnabled,
        boolean facebookEnabled,
        Integer accessTokenTtlSeconds,
        List<AudienceInput> audiences) {

    public record AudienceInput(String audience, boolean isDefault) {
    }
}
