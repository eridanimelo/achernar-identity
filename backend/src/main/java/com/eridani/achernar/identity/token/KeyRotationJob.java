package com.eridani.achernar.identity.token;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** UC-071: garante a chave inicial na subida e aplica o plano de rotação a cada hora (idempotente). */
@Component
@Order(5)
public class KeyRotationJob implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(KeyRotationJob.class);

    private final SigningKeyService service;

    public KeyRotationJob(SigningKeyService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        service.ensureActiveKey();
    }

    @Scheduled(cron = "${achernar.jobs.key-rotation-cron:0 0 * * * *}", zone = "UTC")
    public void rotate() {
        try {
            List<String> done = service.runScheduled().performed();
            if (!done.isEmpty()) {
                LOG.info("Rotação de chaves: {}", done);
            }
        } catch (RuntimeException e) {
            LOG.error("Falha no job de rotação de chaves: {}", e.getClass().getSimpleName());
        }
    }
}
