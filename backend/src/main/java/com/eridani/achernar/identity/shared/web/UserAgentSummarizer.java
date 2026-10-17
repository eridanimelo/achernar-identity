package com.eridani.achernar.identity.shared.web;

import java.util.Locale;

/** Resumo legível do User-Agent ("Chrome em macOS") sem armazenar nada além do original truncado. */
public final class UserAgentSummarizer {

    private UserAgentSummarizer() {
    }

    public static String summarize(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        String browser;
        if (ua.contains("edg/") || ua.contains("edge/")) {
            browser = "Edge";
        } else if (ua.contains("opr/") || ua.contains("opera")) {
            browser = "Opera";
        } else if (ua.contains("firefox/")) {
            browser = "Firefox";
        } else if (ua.contains("chrome/") || ua.contains("crios/")) {
            browser = "Chrome";
        } else if (ua.contains("safari/")) {
            browser = "Safari";
        } else if (ua.contains("curl/")) {
            browser = "curl";
        } else {
            browser = "Navegador desconhecido";
        }
        String os;
        if (ua.contains("windows")) {
            os = "Windows";
        } else if (ua.contains("android")) {
            os = "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            os = "iOS";
        } else if (ua.contains("mac os") || ua.contains("macintosh")) {
            os = "macOS";
        } else if (ua.contains("linux") || ua.contains("x11")) {
            os = "Linux";
        } else {
            os = null;
        }
        return os == null ? browser : browser + " em " + os;
    }
}
