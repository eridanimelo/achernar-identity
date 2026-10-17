package com.eridani.achernar.identity.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Decisão pura sobre a reapresentação de um refresh já rotacionado (token-lifecycle §2.1, passo 7). */
public final class RefreshDecision {

    public enum Reuse { TOLERATED, DETECTED }

    private RefreshDecision() {
    }

    /**
     * Tolerado somente se o token apresentado é o <b>antecessor imediato</b> do corrente da sessão e a rotação
     * ocorreu há no máximo {@code grace} (P-04). Qualquer outro caso é reuso detectado.
     */
    public static Reuse decide(UUID replacedById, UUID currentIdOfSession, Instant rotatedAt, Instant now,
                               Duration grace) {
        boolean immediate = replacedById != null && replacedById.equals(currentIdOfSession);
        boolean withinGrace = rotatedAt != null && !now.isAfter(rotatedAt.plus(grace));
        return immediate && withinGrace ? Reuse.TOLERATED : Reuse.DETECTED;
    }
}
