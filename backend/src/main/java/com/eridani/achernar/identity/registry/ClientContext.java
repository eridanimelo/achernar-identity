package com.eridani.achernar.identity.registry;

/** Cliente identificado numa requisição de {@code /auth/**} (D-08). */
public record ClientContext(Application application, boolean secretAuthenticated) {
}
