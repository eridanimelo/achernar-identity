package com.eridani.achernar.identity.setup;

import com.eridani.achernar.identity.shared.json.Obj;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FT-01: API-FT01-01 e API-FT01-02. */
@RestController
@RequestMapping("/setup")
public class SetupController {

    public record CreateRequest(@JsonProperty("bootstrap_token") String bootstrapToken,
                                @JsonProperty("email") String email, @JsonProperty("password") String password,
                                @JsonProperty("locale") String locale) {
    }

    private final SetupService service;

    public SetupController(SetupService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        SetupService.Status s = service.status();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Obj.of().with("setup_required", s.setupRequired()).with("setup_available", s.setupAvailable()));
    }

    @PostMapping("/admin")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateRequest body) {
        SetupService.Created c = service.create(body.bootstrapToken(), body.email(), body.password(), body.locale());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(Obj.of().with("account_id", c.accountId().toString()).with("email", c.email()));
    }
}
