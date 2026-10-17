package com.eridani.achernar.identity.admin;

import com.eridani.achernar.identity.security.AdminGuard;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FT-12: API-FT12-01. */
@RestController
@RequestMapping("/api/v1/admin/security-events")
public class SecurityEventController {

    private final AdminGuard guard;
    private final SecurityEventQuery query;

    public SecurityEventController(AdminGuard guard, SecurityEventQuery query) {
        this.guard = guard;
        this.query = query;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "type", required = false) List<String> type,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "outcome", required = false) String outcome,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor) {
        guard.require();
        SecurityEventQuery.Criteria c = new SecurityEventQuery.Criteria(
                AdminAccountController.instant("from", from), AdminAccountController.instant("to", to), type,
                AdminAccountController.blankToNull(account), AdminAccountController.blankToNull(clientId),
                AdminAccountController.blankToNull(outcome), AdminAccountController.blankToNull(severity), limit,
                AdminAccountController.blankToNull(cursor));
        return AdminAccountController.noStore(query.search(c));
    }
}
