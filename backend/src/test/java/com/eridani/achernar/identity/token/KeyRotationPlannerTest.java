package com.eridani.achernar.identity.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.eridani.achernar.identity.token.KeyRotationPlanner.Action;
import com.eridani.achernar.identity.token.KeyRotationPlanner.KeyState;
import com.eridani.achernar.identity.token.KeyRotationPlanner.Params;
import com.eridani.achernar.identity.token.KeyRotationPlanner.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeyRotationPlannerTest {

    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");
    private static final Params P = new Params(Duration.ofDays(90), Duration.ofDays(7), Duration.ofDays(30));

    private static List<Type> types(List<Action> a) {
        return a.stream().map(Action::type).toList();
    }

    @Test
    void semChavesFazBootstrap() {
        assertThat(types(KeyRotationPlanner.plan(List.of(), NOW, P))).containsExactly(Type.BOOTSTRAP);
    }

    @Test
    void semAtivaMasComPublicadaVencidaPromove() {
        KeyState pub = new KeyState("k1", KeyStatus.PUBLISHED, NOW.minusSeconds(100), NOW.minusSeconds(1), null);
        List<Action> a = KeyRotationPlanner.plan(List.of(pub), NOW, P);
        assertThat(a).hasSize(1);
        assertThat(a.get(0).type()).isEqualTo(Type.PROMOTE);
        assertThat(a.get(0).kid()).isEqualTo("k1");
    }

    @Test
    void ativaRecenteNaoFazNada() {
        KeyState active = new KeyState("a", KeyStatus.ACTIVE, NOW.minus(Duration.ofDays(10)),
                NOW.minus(Duration.ofDays(10)), null);
        assertThat(KeyRotationPlanner.plan(List.of(active), NOW, P)).isEmpty();
    }

    @Test
    void geraProximaComAntecedenciaDePublicacao() {
        KeyState active = new KeyState("a", KeyStatus.ACTIVE, NOW.minus(Duration.ofDays(83)),
                NOW.minus(Duration.ofDays(83)), null);
        assertThat(types(KeyRotationPlanner.plan(List.of(active), NOW, P))).containsExactly(Type.GENERATE_NEXT);
    }

    @Test
    void naoGeraSeJaHaPublicadaAguardando() {
        KeyState active = new KeyState("a", KeyStatus.ACTIVE, NOW.minus(Duration.ofDays(85)),
                NOW.minus(Duration.ofDays(85)), null);
        KeyState next = new KeyState("b", KeyStatus.PUBLISHED, NOW.minus(Duration.ofDays(1)),
                NOW.plus(Duration.ofDays(5)), null);
        assertThat(KeyRotationPlanner.plan(List.of(active, next), NOW, P)).isEmpty();
    }

    @Test
    void promoveQuandoAtivacaoChega() {
        KeyState active = new KeyState("a", KeyStatus.ACTIVE, NOW.minus(Duration.ofDays(90)),
                NOW.minus(Duration.ofDays(90)), null);
        KeyState next = new KeyState("b", KeyStatus.PUBLISHED, NOW.minus(Duration.ofDays(7)), NOW, null);
        List<Action> a = KeyRotationPlanner.plan(List.of(active, next), NOW, P);
        assertThat(a).hasSize(1);
        assertThat(a.get(0).kid()).isEqualTo("b");
    }

    @Test
    void removeAposenteadaVencidaDaRetencao() {
        KeyState active = new KeyState("a", KeyStatus.ACTIVE, NOW.minus(Duration.ofDays(1)),
                NOW.minus(Duration.ofDays(1)), null);
        KeyState old = new KeyState("o", KeyStatus.RETIRED, NOW.minus(Duration.ofDays(200)),
                NOW.minus(Duration.ofDays(120)), NOW.minus(Duration.ofDays(31)));
        KeyState recent = new KeyState("r", KeyStatus.RETIRED, NOW.minus(Duration.ofDays(100)),
                NOW.minus(Duration.ofDays(50)), NOW.minus(Duration.ofDays(29)));
        List<Action> a = KeyRotationPlanner.plan(List.of(active, old, recent), NOW, P);
        assertThat(a).hasSize(1);
        assertThat(a.get(0).type()).isEqualTo(Type.REMOVE_EXPIRED);
        assertThat(a.get(0).kid()).isEqualTo("o");
    }
}
