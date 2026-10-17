package com.eridani.achernar.identity.shared.web;

/** Sujeito de limitação por IP (IP real resolvido por P-47). */
public final class Ips {

    private Ips() {
    }

    public static String subject() {
        String ip = RequestInfoHolder.get().ip();
        return ip == null ? "unknown" : ip;
    }
}
