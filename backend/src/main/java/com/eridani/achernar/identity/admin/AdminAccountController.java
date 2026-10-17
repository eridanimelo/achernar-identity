package com.eridani.achernar.identity.admin;

import com.eridani.achernar.identity.security.AdminGuard;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.util.Time;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FT-11: API-FT11-01…06. Todas exigem ADMIN verificado no banco. */
@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    public record DisableRequest(@JsonProperty("reason") String reason) {
    }

    public record RoleRequest(@JsonProperty("role") String role, @JsonProperty("version") Long version) {
    }

    public record AnonymizeRequest(@JsonProperty("confirm_email") String confirmEmail) {
    }

    private final AdminGuard guard;
    private final AccountAdminService service;

    public AdminAccountController(AdminGuard guard, AccountAdminService service) {
        this.guard = guard;
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(value = "q", required = false) String q,
                                                    @RequestParam(value = "status", required = false) String status,
                                                    @RequestParam(value = "role", required = false) String role,
                                                    @RequestParam(value = "method", required = false) String method,
                                                    @RequestParam(value = "created_from", required = false) String from,
                                                    @RequestParam(value = "created_to", required = false) String to,
                                                    @RequestParam(value = "page", required = false) Integer page,
                                                    @RequestParam(value = "size", required = false) Integer size,
                                                    @RequestParam(value = "sort", required = false) String sort) {
        guard.require();
        AdminAccountRepository.Filter f = new AdminAccountRepository.Filter(blankToNull(q), blankToNull(status),
                blankToNull(role), blankToNull(method), instant("created_from", from), instant("created_to", to));
        return noStore(service.list(f, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable("id") UUID id) {
        guard.require();
        return noStore(service.detail(id));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable("id") UUID id,
                                        @RequestBody(required = false) DisableRequest body) {
        service.disable(guard.require(), id, body == null ? null : blankToNull(body.reason()));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable("id") UUID id) {
        service.enable(guard.require(), id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Map<String, Object>> role(@PathVariable("id") UUID id, @RequestBody RoleRequest body) {
        return noStore(service.changeRole(guard.require(), id, body.role(), body.version()));
    }

    @PostMapping("/{id}/anonymize")
    public ResponseEntity<Void> anonymize(@PathVariable("id") UUID id, @RequestBody AnonymizeRequest body) {
        service.anonymize(guard.require(), id, body.confirmEmail());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    static Instant instant(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.validation(field, "INVALID_FORMAT");
        }
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
