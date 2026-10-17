package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.web.RequestInfoHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** API-FT09-13: configuração pública mínima da aplicação (RN-APP-12). */
@RestController
public class PublicConfigController {

    private final ApplicationRepository apps;
    private final AchernarProperties props;
    private final RateLimiter limiter;

    public PublicConfigController(ApplicationRepository apps, AchernarProperties props, RateLimiter limiter) {
        this.apps = apps;
        this.props = props;
        this.limiter = limiter;
    }

    @GetMapping("/auth/applications/{clientId}/config")
    public ResponseEntity<Map<String, Object>> config(@PathVariable String clientId) {
        String ip = RequestInfoHolder.get().ip();
        limiter.checkAll(ip == null ? "unknown" : ip, List.of(RateRule.AUTH_IP_MINUTE, RateRule.AUTH_IP_HOUR));
        Application app = apps.findByClientId(clientId).filter(Application::active)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        List<String> providers = new ArrayList<>();
        if (app.googleEnabled() && props.social().googleConfigured()) {
            providers.add("google");
        }
        if (app.facebookEnabled() && props.social().facebookConfigured()) {
            providers.add("facebook");
        }
        Map<String, Object> body = Obj.of().with("name", app.name()).with("type", app.type().name())
                .with("allow_registration", app.allowRegistration()).with("providers", providers);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic()).body(body);
    }
}
