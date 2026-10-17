package com.eridani.achernar.identity.social;

import java.time.Instant;
import java.util.UUID;

/** Transação de login/vínculo/reautenticação social (data-model §2.9). */
public record SocialTransaction(UUID id, Provider provider, Purpose purpose, UUID applicationId, String redirectUri,
                                String clientState, String clientCodeChallenge, String audiences, String nonce,
                                byte[] providerCodeVerifierEnc, String browserBindingHash, UUID accountId,
                                UUID sessionId, Instant createdAt, Instant expiresAt) {

    public enum Purpose { LOGIN, LINK, REAUTH }
}
