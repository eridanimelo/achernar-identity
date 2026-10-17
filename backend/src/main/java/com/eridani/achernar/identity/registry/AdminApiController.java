package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.security.AdminGuard;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.Time;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FT-09: API-FT09-07…12 (Bearer + ADMIN). */
@RestController
@RequestMapping("/api/v1/admin/apis")
public class AdminApiController {

    private final ResourceApiService service;
    private final AdminGuard guard;

    public AdminApiController(ResourceApiService service, AdminGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size,
                                    @RequestParam(required = false) String sort) {
        guard.require();
        return service.list(q, status, page, size, sort).toBody(a -> RegistryJson.api(a, null));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        AccessPrincipal admin = guard.require();
        ResourceApiService.Created c = service.create(string(body, "audience"), string(body, "name"),
                string(body, "description"), admin.accountId());
        Map<String, Object> json = RegistryJson.api(c.api(), null);
        json.put("credential", Obj.of().with("credential_id", c.credentialId().toString()).with("secret", c.secret()));
        return ResponseEntity.status(HttpStatus.CREATED).body(json);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        guard.require();
        return RegistryJson.api(service.get(id), service.credentials(id));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> patch(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        AccessPrincipal admin = guard.require();
        ResourceApi updated = service.update(id, body, admin.accountId());
        return RegistryJson.api(updated, service.credentials(id));
    }

    @PostMapping("/{id}/credentials")
    public ResponseEntity<Map<String, Object>> createCredential(@PathVariable UUID id) {
        AccessPrincipal admin = guard.require();
        ResourceApiService.CredentialCreated c = service.createCredential(id, admin.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Obj.of().with("credential_id", c.credentialId().toString())
                .with("secret", c.secret()).with("created_at", Time.iso(c.createdAt())));
    }

    @DeleteMapping("/{id}/credentials/{credentialId}")
    public ResponseEntity<Void> revokeCredential(@PathVariable UUID id, @PathVariable UUID credentialId) {
        AccessPrincipal admin = guard.require();
        service.revokeCredential(id, credentialId, admin.accountId());
        return ResponseEntity.noContent().build();
    }

    private static String string(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof String s) {
            return s;
        }
        throw new ApiException(ErrorCode.VALIDATION_ERROR,
                java.util.List.of(com.eridani.achernar.identity.shared.error.FieldError.of(key, "INVALID_TYPE")));
    }
}
