package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.registry.ClientAuthenticator;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.security.CurrentUser;
import com.eridani.achernar.identity.shared.json.Obj;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** FT-05: API-FT05-02 e API-FT05-03. */
@RestController
public class EmailChangeController {

    public record ChangeRequest(@JsonProperty("new_email") String newEmail, @JsonProperty("client_id") String clientId) {
    }

    public record ConfirmRequest(@JsonProperty("client_id") String clientId, @JsonProperty("token") String token) {
    }

    private final ClientAuthenticator clients;
    private final EmailChangeService service;

    public EmailChangeController(ClientAuthenticator clients, EmailChangeService service) {
        this.clients = clients;
        this.service = service;
    }

    @PostMapping("/api/v1/account/email-change")
    public ResponseEntity<Map<String, Object>> request(@RequestBody ChangeRequest body) {
        service.request(CurrentUser.require(), body.newEmail(), body.clientId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Obj.of().with("status", "confirmation_email_sent_if_applicable"));
    }

    @PostMapping("/auth/email-change/confirm")
    public ResponseEntity<Void> confirm(HttpServletRequest request, @RequestBody ConfirmRequest body) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        service.confirm(client, body.token());
        return ResponseEntity.noContent().build();
    }
}
