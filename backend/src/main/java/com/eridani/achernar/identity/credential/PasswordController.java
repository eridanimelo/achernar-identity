package com.eridani.achernar.identity.credential;

import com.eridani.achernar.identity.registry.ClientAuthenticator;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.security.CurrentUser;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.Time;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** FT-04: API-FT04-01…04. */
@RestController
public class PasswordController {

    public record ForgotRequest(@JsonProperty("client_id") String clientId, @JsonProperty("email") String email) {
    }

    public record ResetRequest(@JsonProperty("client_id") String clientId, @JsonProperty("token") String token,
                               @JsonProperty("new_password") String newPassword) {
    }

    public record ReauthRequest(@JsonProperty("password") String password) {
    }

    public record ChangeRequest(@JsonProperty("current_password") String currentPassword,
                                @JsonProperty("new_password") String newPassword) {
    }

    private final ClientAuthenticator clients;
    private final PasswordService service;

    public PasswordController(ClientAuthenticator clients, PasswordService service) {
        this.clients = clients;
        this.service = service;
    }

    @PostMapping("/auth/password/forgot")
    public ResponseEntity<Map<String, Object>> forgot(HttpServletRequest request, @RequestBody ForgotRequest body) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        service.forgot(client, body.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Obj.of().with("status", "reset_email_sent_if_applicable"));
    }

    @PostMapping("/auth/password/reset")
    public ResponseEntity<Void> reset(HttpServletRequest request, @RequestBody ResetRequest body) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        service.reset(client, body.token(), body.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/account/reauth")
    public Map<String, Object> reauth(@RequestBody ReauthRequest body) {
        AccessPrincipal who = CurrentUser.require();
        Instant until = service.reauthenticate(who, body.password());
        return Obj.of().with("reauthenticated_until", Time.iso(until));
    }

    @PostMapping("/api/v1/account/password")
    public ResponseEntity<Void> change(@RequestBody ChangeRequest body) {
        service.change(CurrentUser.require(), body.currentPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
