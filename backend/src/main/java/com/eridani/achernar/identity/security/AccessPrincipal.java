package com.eridani.achernar.identity.security;

import java.util.List;
import java.util.UUID;

/** Identidade autenticada por access token do Identity (RN-TOK-15): conta, sessão e aplicação de origem. */
public record AccessPrincipal(UUID accountId, UUID sessionId, String clientId, List<String> audiences, String jti) {
}
