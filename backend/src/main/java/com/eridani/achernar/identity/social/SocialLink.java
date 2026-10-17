package com.eridani.achernar.identity.social;

import java.time.Instant;
import java.util.UUID;

public record SocialLink(UUID id, UUID accountId, Provider provider, String providerSubject, String providerEmail,
                         Instant linkedAt, Instant lastLoginAt) {
}
