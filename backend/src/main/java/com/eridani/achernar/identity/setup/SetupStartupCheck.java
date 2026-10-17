package com.eridani.achernar.identity.setup;

import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** RN-SET-04: sem administrador ativo e sem segredo de implantação ⇒ aviso (e falha de subida em {@code prod}). */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SetupStartupCheck implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SetupStartupCheck.class);

    private final AccountRepository accounts;
    private final AchernarProperties props;
    private final Environment env;

    public SetupStartupCheck(AccountRepository accounts, AchernarProperties props, Environment env) {
        this.accounts = accounts;
        this.props = props;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.hasActiveAdmin()) {
            return;
        }
        boolean configured = props.bootstrapToken() != null && !props.bootstrapToken().isBlank();
        if (configured) {
            LOG.info("Setup disponível: nenhum administrador ativo. Use POST /setup/admin com ACHERNAR_BOOTSTRAP_TOKEN.");
            return;
        }
        String msg = "Nenhum administrador ativo e ACHERNAR_BOOTSTRAP_TOKEN não definido: o setup está indisponível";
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(msg);
        }
        LOG.warn(msg);
    }
}
