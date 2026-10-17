package com.eridani.achernar.identity.credential;

import com.eridani.achernar.identity.shared.error.FieldError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Política de senhas NIST (RN-PWD-01, D-15): comprimento em caracteres Unicode (NFKC), lista de comuns,
 * parte local/domínio do e-mail (≥ 4 caracteres) e só espaços. Sem regras de composição nem expiração.
 */
public final class PasswordPolicy {

    private final int minLength;
    private final int maxLength;
    private final Set<String> common;

    public PasswordPolicy(int minLength, int maxLength, Set<String> common) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.common = common;
    }

    public static PasswordPolicy withBundledList(int minLength, int maxLength) {
        return new PasswordPolicy(minLength, maxLength, loadBundled());
    }

    public static String normalize(String password) {
        return password == null ? null : Normalizer.normalize(password, Normalizer.Form.NFKC);
    }

    /**
     * @param emailNormalized e-mail normalizado do titular (pode ser nulo)
     * @return lista de violações (códigos {@code BLANK}, {@code TOO_SHORT}, {@code TOO_LONG}, {@code COMMON},
     *         {@code CONTAINS_EMAIL}); vazia se a senha é aceitável
     */
    public List<FieldError> check(String password, String emailNormalized) {
        List<FieldError> errors = new ArrayList<>();
        String p = normalize(password == null ? "" : password);
        if (p.isBlank()) {
            errors.add(FieldError.of("password", "BLANK"));
            return errors;
        }
        int length = p.codePointCount(0, p.length());
        if (length < minLength) {
            errors.add(FieldError.of("password", "TOO_SHORT"));
        }
        if (length > maxLength) {
            errors.add(FieldError.of("password", "TOO_LONG"));
        }
        String lower = p.toLowerCase(Locale.ROOT);
        if (common.contains(lower)) {
            errors.add(FieldError.of("password", "COMMON"));
        }
        if (emailNormalized != null && containsEmailPart(lower, emailNormalized)) {
            errors.add(FieldError.of("password", "CONTAINS_EMAIL"));
        }
        return errors;
    }

    public int minLength() {
        return minLength;
    }

    public int maxLength() {
        return maxLength;
    }

    static boolean containsEmailPart(String lowerPassword, String emailNormalized) {
        int at = emailNormalized.indexOf('@');
        if (at <= 0) {
            return false;
        }
        String local = emailNormalized.substring(0, at);
        String domain = emailNormalized.substring(at + 1);
        if (local.length() >= 4 && lowerPassword.contains(local)) {
            return true;
        }
        if (domain.length() >= 4 && lowerPassword.contains(domain)) {
            return true;
        }
        String domainName = domain.contains(".") ? domain.substring(0, domain.indexOf('.')) : domain;
        return domainName.length() >= 4 && lowerPassword.contains(domainName);
    }

    private static Set<String> loadBundled() {
        Set<String> set = new HashSet<>(1 << 16);
        try (InputStream in = PasswordPolicy.class.getResourceAsStream("/common-passwords.txt")) {
            if (in == null) {
                throw new IllegalStateException("common-passwords.txt ausente do classpath");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.strip().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) {
                    set.add(t);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler common-passwords.txt", e);
        }
        return set;
    }
}
