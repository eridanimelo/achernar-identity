package com.eridani.achernar.identity.session;

import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.security.CurrentUser;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.Time;
import com.eridani.achernar.identity.shared.web.UserAgentSummarizer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FT-06: API-FT06-01…03 (Bearer). */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> list() {
        AccessPrincipal p = CurrentUser.require();
        List<Map<String, Object>> items = service.list(p.accountId()).stream().map(v -> {
            Session s = v.session();
            return (Map<String, Object>) Obj.of().with("id", s.id().toString()).with("client_id", v.clientId())
                    .with("application_name", v.applicationName()).with("created_at", Time.iso(s.createdAt()))
                    .with("last_used_at", Time.iso(s.lastUsedAt())).with("ip_approx", s.ip())
                    .with("user_agent_summary", UserAgentSummarizer.summarize(s.userAgent()))
                    .with("amr", s.amr()).with("current", s.id().equals(p.sessionId()));
        }).toList();
        return Obj.of().with("items", items);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        service.revokeOwn(CurrentUser.require().accountId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> endAll(@RequestParam(name = "keep_current", defaultValue = "false") boolean keepCurrent) {
        AccessPrincipal p = CurrentUser.require();
        service.endAll(p.accountId(), p.sessionId(), keepCurrent);
        return ResponseEntity.noContent().build();
    }
}
