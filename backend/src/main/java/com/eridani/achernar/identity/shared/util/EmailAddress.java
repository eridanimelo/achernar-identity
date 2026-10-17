package com.eridani.achernar.identity.shared.util;

import java.net.IDN;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validação e normalização de e-mail (D-14): aparar → NFC → minúsculas ({@code Locale.ROOT}) → domínio em ASCII (IDN).
 * Parte local restrita a ASCII imprimível sem aspas/comentários. Sem dobra de pontos nem remoção de {@code +tag}.
 */
public final class EmailAddress {

    public static final int MAX_LENGTH = 254;
    private static final Pattern LOCAL = Pattern.compile("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]{1,64}$");
    private static final Pattern DOMAIN_LABEL = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");

    public record Normalized(String display, String normalized) {
        public String localPart() {
            return normalized.substring(0, normalized.indexOf('@'));
        }

        public String domain() {
            return normalized.substring(normalized.indexOf('@') + 1);
        }
    }

    private EmailAddress() {
    }

    public static Optional<Normalized> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String display = input.strip();
        if (display.isEmpty() || display.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        String nfc = Normalizer.normalize(display, Normalizer.Form.NFC);
        int at = nfc.indexOf('@');
        if (at <= 0 || at != nfc.lastIndexOf('@') || at == nfc.length() - 1) {
            return Optional.empty();
        }
        String local = nfc.substring(0, at);
        String domainRaw = nfc.substring(at + 1);
        if (!LOCAL.matcher(local).matches() || local.startsWith(".") || local.endsWith(".") || local.contains("..")) {
            return Optional.empty();
        }
        String ascii;
        try {
            ascii = IDN.toASCII(domainRaw.toLowerCase(Locale.ROOT), IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (ascii.length() > 253 || !ascii.contains(".")) {
            return Optional.empty();
        }
        for (String label : ascii.split("\\.", -1)) {
            if (!DOMAIN_LABEL.matcher(label).matches()) {
                return Optional.empty();
            }
        }
        String normalized = local.toLowerCase(Locale.ROOT) + "@" + ascii;
        if (normalized.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        return Optional.of(new Normalized(display, normalized));
    }

    /** Máscara para logs/exibição: {@code u***@d***.com} (SEC-31). */
    public static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        String head = domain.isEmpty() ? "" : domain.substring(0, 1);
        String tail = dot >= 0 ? domain.substring(dot) : "";
        return local.charAt(0) + "***@" + head + "***" + tail;
    }
}
