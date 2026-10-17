package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.security.AdminGuard;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.Time;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FT-10: API-FT10-03…05 (Bearer + ADMIN). */
@RestController
@RequestMapping("/api/v1/admin/signing-keys")
public class AdminKeyController {

    public record RemoveRequest(@JsonProperty("emergency") Boolean emergency,
                                @JsonProperty("revoke_all_sessions") Boolean revokeAllSessions,
                                @JsonProperty("reason") String reason) {
    }

    private final SigningKeyService keys;
    private final AdminGuard guard;
    private final SessionService sessions;

    public AdminKeyController(SigningKeyService keys, AdminGuard guard, SessionService sessions) {
        this.keys = keys;
        this.guard = guard;
        this.sessions = sessions;
    }

    @GetMapping
    public Map<String, Object> list() {
        guard.require();
        return Obj.of().with("items", keys.list().stream().map(AdminKeyController::json).toList());
    }

    @PostMapping("/rotate")
    public ResponseEntity<Map<String, Object>> rotate() {
        AccessPrincipal admin = guard.require();
        sessions.requireRecentReauth(admin.sessionId());
        SigningKey k = keys.rotateNow(admin.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(json(k));
    }

    @PostMapping("/{kid}/remove")
    public ResponseEntity<Void> remove(@PathVariable String kid, @RequestBody(required = false) RemoveRequest body) {
        AccessPrincipal admin = guard.require();
        sessions.requireRecentReauth(admin.sessionId());
        boolean emergency = body != null && Boolean.TRUE.equals(body.emergency());
        boolean revokeAll = emergency && body != null && Boolean.TRUE.equals(body.revokeAllSessions());
        keys.remove(kid, emergency, revokeAll, admin.accountId());
        return ResponseEntity.noContent().build();
    }

    private static Map<String, Object> json(SigningKey k) {
        return Obj.of().with("kid", k.kid()).with("status", k.status().name())
                .with("created_at", Time.iso(k.createdAt())).with("publish_at", Time.iso(k.publishAt()))
                .with("activate_at", Time.iso(k.activateAt())).with("retire_at", Time.iso(k.retireAt()))
                .with("remove_at", Time.iso(k.removeAt())).with("removal_reason", k.removalReason());
    }

}
