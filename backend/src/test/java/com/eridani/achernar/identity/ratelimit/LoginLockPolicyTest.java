package com.eridani.achernar.identity.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.eridani.achernar.identity.ratelimit.LoginLockPolicy.Result;
import com.eridani.achernar.identity.ratelimit.LoginLockPolicy.State;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginLockPolicyTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private static State failTimes(State s, int n, Instant start, Duration step) {
        State cur = s;
        for (int i = 0; i < n; i++) {
            Result r = LoginLockPolicy.onFailure(cur, start.plus(step.multipliedBy(i)));
            cur = r.state();
        }
        return cur;
    }

    @Test
    void quintaFalhaDentroDaJanelaBloqueiaPor15Minutos() {
        State s = failTimes(State.empty(T0), 4, T0, Duration.ofSeconds(10));
        assertThat(s.lockedUntil()).isNull();
        Result r = LoginLockPolicy.onFailure(s, T0.plusSeconds(60));
        assertThat(r.newlyLocked()).isTrue();
        assertThat(r.lockDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(r.state().lockLevel()).isEqualTo(1);
        assertThat(LoginLockPolicy.isLocked(r.state(), T0.plusSeconds(61))).isTrue();
        assertThat(LoginLockPolicy.isLocked(r.state(), T0.plusSeconds(60).plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void falhasForaDaJanelaReiniciamContagem() {
        State s = failTimes(State.empty(T0), 4, T0, Duration.ofSeconds(1));
        Result r = LoginLockPolicy.onFailure(s, T0.plus(Duration.ofMinutes(16)));
        assertThat(r.newlyLocked()).isFalse();
        assertThat(r.state().failureCount()).isEqualTo(1);
    }

    @Test
    void reincidenciaEm24hDobraAteOTeto() {
        Instant now = T0;
        State s = State.empty(now);
        Duration[] esperado = {Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofMinutes(60),
            Duration.ofMinutes(120), Duration.ofMinutes(120)};
        for (Duration d : esperado) {
            s = failTimes(s, 4, now, Duration.ofSeconds(1));
            Result r = LoginLockPolicy.onFailure(s, now.plusSeconds(5));
            assertThat(r.newlyLocked()).isTrue();
            assertThat(r.lockDuration()).isEqualTo(d);
            s = r.state();
            now = s.lockedUntil().plusSeconds(1);
        }
    }

    @Test
    void semReincidenciaDepoisDe24hVoltaAoNivel1() {
        State s = failTimes(State.empty(T0), 4, T0, Duration.ofSeconds(1));
        Result first = LoginLockPolicy.onFailure(s, T0.plusSeconds(5));
        Instant later = first.state().lastLockAt().plus(Duration.ofHours(25));
        State s2 = failTimes(State.empty(later), 4, later, Duration.ofSeconds(1));
        State withHistory = new State(s2.failureCount(), s2.windowStart(), null, first.state().lockLevel(),
                first.state().lastLockAt());
        Result r = LoginLockPolicy.onFailure(withHistory, later.plusSeconds(5));
        assertThat(r.state().lockLevel()).isEqualTo(1);
        assertThat(r.lockDuration()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void falhaEnquantoBloqueadoNaoAlteraEstado() {
        State s = failTimes(State.empty(T0), 4, T0, Duration.ofSeconds(1));
        State locked = LoginLockPolicy.onFailure(s, T0.plusSeconds(5)).state();
        Result r = LoginLockPolicy.onFailure(locked, T0.plusSeconds(6));
        assertThat(r.newlyLocked()).isFalse();
        assertThat(r.state()).isEqualTo(locked);
    }

    @Test
    void segundosRestantesArredondamParaCima() {
        State s = new State(0, T0, T0.plusMillis(1500), 1, T0);
        assertThat(LoginLockPolicy.remainingSeconds(s, T0)).isEqualTo(2L);
        assertThat(LoginLockPolicy.remainingSeconds(s, T0.plusSeconds(10))).isEqualTo(0L);
    }

    @Test
    void duracaoPorNivel() {
        assertThat(LoginLockPolicy.durationForLevel(1)).isEqualTo(Duration.ofMinutes(15));
        assertThat(LoginLockPolicy.durationForLevel(2)).isEqualTo(Duration.ofMinutes(30));
        assertThat(LoginLockPolicy.durationForLevel(4)).isEqualTo(Duration.ofMinutes(120));
        assertThat(LoginLockPolicy.durationForLevel(50)).isEqualTo(Duration.ofMinutes(120));
    }
}
