package com.eridani.achernar.identity.mail;

/** Catálogo de modelos de e-mail (emails.md §4). O texto vive em {@code templates/mail/}. */
public enum MailTemplate {
    TPL_01("TPL-01"), TPL_02("TPL-02"), TPL_03("TPL-03"), TPL_04("TPL-04"), TPL_05("TPL-05"), TPL_06("TPL-06"),
    TPL_07("TPL-07"), TPL_08("TPL-08"), TPL_09("TPL-09"), TPL_10("TPL-10"), TPL_11("TPL-11");

    private final String id;

    MailTemplate(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static MailTemplate fromId(String id) {
        for (MailTemplate t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Modelo de e-mail desconhecido: " + id);
    }
}
