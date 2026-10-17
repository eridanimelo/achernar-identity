package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Semeia a aplicação da UI do Identity ({@code achernar-identity-ui}, D-46/RN-SET-05) a partir da configuração
 * (P-61/P-62/P-63). Idempotente; sem credencial fixa.
 */
@Component
@Order(10)
public class UiApplicationSeeder implements ApplicationRunner {

    public static final String UI_CLIENT_ID = "achernar-identity-ui";
    private static final Logger LOG = LoggerFactory.getLogger(UiApplicationSeeder.class);

    private final ApplicationRepository apps;
    private final AchernarProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public UiApplicationSeeder(ApplicationRepository apps, AchernarProperties props, Clock clock,
                               TransactionTemplate tx) {
        this.apps = apps;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> seed());
    }

    void seed() {
        if (apps.findByClientId(UI_CLIENT_ID).isPresent()) {
            return;
        }
        List<String> origins = new ArrayList<>();
        if (props.uiOrigins() != null) {
            for (String o : props.uiOrigins()) {
                if (o != null && !o.isBlank() && !origins.contains(o.strip())) {
                    origins.add(o.strip());
                }
            }
        }
        String base = props.uiBase();
        String uiOrigin = originOf(base);
        if (uiOrigin != null && !origins.contains(uiOrigin)) {
            origins.add(uiOrigin);
        }
        Instant now = clock.instant();
        Application ui = new Application(UUID.randomUUID(), UI_CLIENT_ID, "Achernar Identity", "Interface do Identity",
                ClientType.BROWSER, RegistryStatus.ACTIVE, false, true, props.social().googleConfigured(),
                props.social().facebookConfigured(), null, base + "/verify-email", base + "/reset-password",
                base + "/confirm-email-change", true, now, now, 0, origins,
                List.of(base + "/auth/callback"), List.of());
        apps.insert(ui);
        LOG.info("Aplicação da UI '{}' criada (origens={})", UI_CLIENT_ID, origins);
    }

    static String originOf(String url) {
        try {
            URI u = new URI(url);
            if (u.getScheme() == null || u.getHost() == null) {
                return null;
            }
            return u.getScheme().toLowerCase() + "://" + u.getHost().toLowerCase()
                    + (u.getPort() >= 0 ? ":" + u.getPort() : "");
        } catch (Exception e) {
            return null;
        }
    }
}
