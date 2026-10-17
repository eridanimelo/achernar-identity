package com.eridani.achernar.identity.shared.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Convenção {@code *_FILE} (deployment-operations §4): se {@code ACHERNAR_X_FILE} existir e {@code ACHERNAR_X}
 * não estiver definida, o valor vem do arquivo (conteúdo aparado). Executado antes do Spring subir, publicando
 * o valor como propriedade do sistema (visível para {@code ${ACHERNAR_X:}} em application.yml).
 * Nunca registra o conteúdo.
 */
public final class SecretsFromFiles {

    private static final String SUFFIX = "_FILE";
    private static final String PREFIX = "ACHERNAR_";

    private SecretsFromFiles() {
    }

    public static void apply(Map<String, String> env, BiConsumer<String, String> sink) {
        for (Map.Entry<String, String> e : env.entrySet()) {
            String name = e.getKey();
            if (!name.startsWith(PREFIX) || !name.endsWith(SUFFIX) || e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            String target = name.substring(0, name.length() - SUFFIX.length());
            String current = env.get(target);
            if (current != null && !current.isBlank()) {
                continue;
            }
            try {
                String content = Files.readString(Path.of(e.getValue().trim()), StandardCharsets.UTF_8).trim();
                sink.accept(target, content);
            } catch (IOException | RuntimeException ex) {
                throw new IllegalStateException("Não foi possível ler o arquivo de segredo referenciado por " + name, ex);
            }
        }
    }
}
