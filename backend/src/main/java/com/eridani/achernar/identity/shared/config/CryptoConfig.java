package com.eridani.achernar.identity.shared.config;

import com.eridani.achernar.identity.shared.crypto.MasterKeyCipher;
import com.eridani.achernar.identity.shared.web.TrustedProxyResolver;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoConfig.class);

    @Bean
    MasterKeyCipher masterKeyCipher(AchernarProperties props) {
        if (PropertiesValidator.masterKeyOk(props.masterKey())) {
            return MasterKeyCipher.fromBase64(props.masterKey());
        }
        if (props.devInsecureKey()) {
            LOG.warn("ACHERNAR_DEV_INSECURE_KEY=true: usando chave-mestra fixa e INSEGURA (somente desenvolvimento)");
            return MasterKeyCipher.devInsecure();
        }
        throw new IllegalStateException("ACHERNAR_MASTER_KEY ausente (>= 32 bytes em base64). Gere com: openssl rand -base64 32");
    }

    /** Relógio injetável (D-42): UTC. Testes substituem por relógio controlável. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TrustedProxyResolver trustedProxyResolver(AchernarProperties props) {
        return new TrustedProxyResolver(props.trustedProxies());
    }
}
