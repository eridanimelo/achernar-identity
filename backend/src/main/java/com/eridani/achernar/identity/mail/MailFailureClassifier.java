package com.eridani.achernar.identity.mail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Distingue falha permanente de destinatário (5xx) de falhas temporárias (4xx, timeout, conexão). */
public final class MailFailureClassifier {

    private static final Pattern CODE = Pattern.compile("^\\s*(?:\\d{3}[ -])?(5\\d\\d)\\b");

    private MailFailureClassifier() {
    }

    public static boolean isPermanent(Throwable error) {
        int depth = 0;
        for (Throwable t = error; t != null && depth < 10; t = t.getCause(), depth++) {
            String name = t.getClass().getSimpleName();
            if (name.equals("AddressException") || name.equals("SMTPAddressFailedException")
                    || name.equals("InvalidAddressException")) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && (name.startsWith("SMTP") || name.contains("SendFailed"))) {
                Matcher m = CODE.matcher(msg);
                if (m.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Mensagem de erro sem dados pessoais nem token (RN-EML-05): apenas a classe e um código. */
    public static String safeError(Throwable error) {
        String base = error.getClass().getSimpleName();
        String msg = error.getMessage();
        if (msg != null) {
            Matcher m = Pattern.compile("\\b([45]\\d\\d)\\b").matcher(msg);
            if (m.find()) {
                return base + " " + m.group(1);
            }
        }
        return base;
    }
}
