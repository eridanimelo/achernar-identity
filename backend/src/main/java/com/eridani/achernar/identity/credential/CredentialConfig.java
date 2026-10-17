package com.eridani.achernar.identity.credential;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CredentialConfig {

    @Bean
    PasswordPolicy passwordPolicy(AchernarProperties props) {
        return PasswordPolicy.withBundledList(props.password().minLength(), props.password().maxLength());
    }
}
