package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.ResourceApi;
import com.eridani.achernar.identity.registry.ResourceApiService;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.json.MiniJson;
import com.eridani.achernar.identity.shared.web.BasicAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** API-FT10-06: {@code POST /api/v1/tokens/introspect} (Basic {@code audience:segredo}). */
@RestController
public class IntrospectionController {

    private final ResourceApiService apis;
    private final IntrospectionService service;
    private final RateLimiter limiter;
    private final SecurityEventRecorder events;

    public IntrospectionController(ResourceApiService apis, IntrospectionService service, RateLimiter limiter,
                                   SecurityEventRecorder events) {
        this.apis = apis;
        this.service = service;
        this.limiter = limiter;
        this.events = events;
    }

    @PostMapping("/api/v1/tokens/introspect")
    public ResponseEntity<Map<String, Object>> introspect(HttpServletRequest request) throws IOException {
        BasicAuth.Credentials credentials = BasicAuth.parse(request.getHeader("Authorization"))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CLIENT));
        limiter.check(RateRule.INTROSPECT_API, credentials.id());
        ResourceApi api;
        try {
            api = apis.authenticate(credentials.id(), credentials.secret());
        } catch (ApiException e) {
            events.recordIndependent(AuditEvent.of(EventType.INTROSPECTION_AUTH_FAILED)
                    .detail("audience", credentials.id().length() > 200 ? null : credentials.id()));
            throw e;
        }
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            throw ApiException.validation("token", "REQUIRED");
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").contentType(MediaType.APPLICATION_JSON)
                .body(service.introspect(api, token));
    }

    private static String extractToken(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("application/json")) {
            byte[] body = request.getInputStream().readAllBytes();
            Object token = MiniJson.parseObject(new String(body, StandardCharsets.UTF_8)).get("token");
            return token instanceof String s ? s : null;
        }
        return request.getParameter("token");
    }
}
