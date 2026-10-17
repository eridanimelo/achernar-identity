package com.eridani.achernar.identity;

import com.eridani.achernar.identity.shared.config.SecretsFromFiles;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AchernarIdentityApplication {

    public static void main(String[] args) {
        SecretsFromFiles.apply(System.getenv(), System::setProperty);
        SpringApplication.run(AchernarIdentityApplication.class, args);
    }
}
