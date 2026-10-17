package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.registry.ClientAuthenticator;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.shared.json.Obj;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FT-02: API-FT02-01…03. */
@RestController
@RequestMapping("/auth")
public class RegistrationController {

    public record RegisterRequest(@JsonProperty("client_id") String clientId, @JsonProperty("email") String email,
                                  @JsonProperty("password") String password, @JsonProperty("locale") String locale) {
    }

    public record VerifyRequest(@JsonProperty("client_id") String clientId, @JsonProperty("token") String token) {
    }

    public record ResendRequest(@JsonProperty("client_id") String clientId, @JsonProperty("email") String email) {
    }

    private final ClientAuthenticator clients;
    private final RegistrationService service;

    public RegistrationController(ClientAuthenticator clients, RegistrationService service) {
        this.clients = clients;
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(HttpServletRequest request, @RequestBody RegisterRequest body,
                                                        @RequestHeader(value = "Accept-Language", required = false)
                                                        String acceptLanguage) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        service.register(client, body.email(), body.password(), body.locale(), acceptLanguage);
        return neutral();
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Void> verify(HttpServletRequest request, @RequestBody VerifyRequest body) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        service.verifyEmail(client, body.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/resend-verification")
    public ResponseEntity<Map<String, Object>> resend(HttpServletRequest request, @RequestBody ResendRequest body) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        service.resend(client, body.email());
        return neutral();
    }

    static ResponseEntity<Map<String, Object>> neutral() {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Obj.of().with("status", "verification_email_sent_if_applicable"));
    }
}
