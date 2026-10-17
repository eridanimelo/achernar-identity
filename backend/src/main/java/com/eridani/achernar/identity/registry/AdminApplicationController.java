package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.security.AdminGuard;
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

/** FT-09: API-FT09-01…06 (Bearer + ADMIN). */
@RestController
@RequestMapping("/api/v1/admin/applications")
public class AdminApplicationController {

    private final ApplicationService service;
    private final AdminGuard guard;

    public AdminApplicationController(ApplicationService service, AdminGuard guard) {
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
        return service.list(q, status, page, size, sort).toBody(RegistryJson::summary);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        AccessPrincipal admin = guard.require();
        ApplicationInput in = ApplicationPatch.fromCreate(body);
        ApplicationService.Created created = service.create(in, admin.accountId());
        Map<String, Object> json = RegistryJson.application(created.application(),
                service.secrets(created.application().id()));
        if (created.clientSecret() != null) {
            json.put("client_secret", created.clientSecret());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(json);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        guard.require();
        return RegistryJson.application(service.get(id), service.secrets(id));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> patch(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        AccessPrincipal admin = guard.require();
        Application current = service.get(id);
        long version = ApplicationPatch.version(body);
        ApplicationInput in = ApplicationPatch.merge(current, body);
        Application updated = service.update(id, in, version, admin.accountId());
        return RegistryJson.application(updated, service.secrets(id));
    }

    @PostMapping("/{id}/secrets")
    public ResponseEntity<Map<String, Object>> createSecret(@PathVariable UUID id) {
        AccessPrincipal admin = guard.require();
        ApplicationService.SecretCreated s = service.createSecret(id, admin.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Obj.of().with("secret_id", s.secretId().toString())
                .with("client_secret", s.clientSecret()).with("created_at", Time.iso(s.createdAt())));
    }

    @DeleteMapping("/{id}/secrets/{secretId}")
    public ResponseEntity<Void> revokeSecret(@PathVariable UUID id, @PathVariable UUID secretId) {
        AccessPrincipal admin = guard.require();
        service.revokeSecret(id, secretId, admin.accountId());
        return ResponseEntity.noContent().build();
    }
}
