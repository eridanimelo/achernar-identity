package com.eridani.achernar.identity.registry;

import java.util.UUID;

/** Audiência (API) permitida a uma aplicação. */
public record AudienceGrant(UUID apiId, String audience, boolean isDefault, boolean apiActive) {
}
