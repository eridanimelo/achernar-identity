package com.eridani.achernar.identity.social;

/** Identidade devolvida pelo provedor; os tokens do provedor nunca saem do cliente HTTP (SEC-18). */
public record ProviderIdentity(String subject, String email, boolean emailVerified) {
}
