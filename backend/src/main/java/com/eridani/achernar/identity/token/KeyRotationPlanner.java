package com.eridani.achernar.identity.token;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Planejador puro do ciclo de vida das chaves (token-lifecycle §6, RN-TOK-12):
 * {@code PUBLISHED → ACTIVE → RETIRED → REMOVED}. Retorna as ações a aplicar; quem executa as aplica sob trava.
 */
public final class KeyRotationPlanner {

    public enum Type { BOOTSTRAP, GENERATE_NEXT, PROMOTE, REMOVE_EXPIRED }

    public record Action(Type type, String kid) {
    }

    public record KeyState(String kid, KeyStatus status, Instant publishAt, Instant activateAt, Instant retireAt) {
    }

    public record Params(Duration rotationInterval, Duration publishLead, Duration retiredRetention) {
    }

    private KeyRotationPlanner() {
    }

    public static List<Action> plan(List<KeyState> keys, Instant now, Params p) {
        List<Action> actions = new ArrayList<>();
        KeyState active = keys.stream().filter(k -> k.status() == KeyStatus.ACTIVE).findFirst().orElse(null);
        List<KeyState> published = keys.stream().filter(k -> k.status() == KeyStatus.PUBLISHED)
                .sorted(Comparator.comparing(KeyState::activateAt)).toList();

        if (active == null) {
            KeyState ready = published.stream().filter(k -> !now.isBefore(k.activateAt())).findFirst().orElse(null);
            actions.add(ready != null ? new Action(Type.PROMOTE, ready.kid()) : new Action(Type.BOOTSTRAP, null));
        } else {
            KeyState due = published.stream().filter(k -> !now.isBefore(k.activateAt())).findFirst().orElse(null);
            if (due != null) {
                actions.add(new Action(Type.PROMOTE, due.kid()));
            } else if (published.isEmpty()) {
                Instant generateAt = active.activateAt().plus(p.rotationInterval()).minus(p.publishLead());
                if (!now.isBefore(generateAt)) {
                    actions.add(new Action(Type.GENERATE_NEXT, null));
                }
            }
        }
        for (KeyState k : keys) {
            if (k.status() == KeyStatus.RETIRED && k.retireAt() != null
                    && !now.isBefore(k.retireAt().plus(p.retiredRetention()))) {
                actions.add(new Action(Type.REMOVE_EXPIRED, k.kid()));
            }
        }
        return actions;
    }
}
