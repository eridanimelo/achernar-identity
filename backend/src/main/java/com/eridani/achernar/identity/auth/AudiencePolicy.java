package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ResourceApiRepository;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/** Política de audiências do token (RN-APP-17, RN-TOK-07): contexto (configuração e banco) + {@link AudienceRules}. */
@Component
public class AudiencePolicy {

    private final AchernarProperties props;
    private final ResourceApiRepository apis;

    public AudiencePolicy(AchernarProperties props, ResourceApiRepository apis) {
        this.props = props;
        this.apis = apis;
    }

    public static List<String> sanitize(List<String> requested) {
        return AudienceRules.sanitize(requested);
    }

    public List<String> forLogin(Application app, List<String> requested) {
        return AudienceRules.forLogin(app, requested, props.uiAudience());
    }

    public List<String> forRefresh(Application app, List<String> sessionAudiences, List<String> requested) {
        return AudienceRules.forRefresh(app, sessionAudiences, requested, props.uiAudience(),
                a -> apis.findByAudience(a).map(x -> x.active()).orElse(false));
    }
}
