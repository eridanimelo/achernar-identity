package com.eridani.achernar.identity.shared.crypto;

import java.util.Base64;
import java.util.regex.Pattern;

/** PKCE S256 (RFC 7636). */
public final class Pkce {

    private static final Pattern VERIFIER = Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");
    private static final Pattern CHALLENGE = Pattern.compile("^[A-Za-z0-9\\-_]{43,128}$");

    private Pkce() {
    }

    public static String challengeFor(String verifier) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Sha256.digest(verifier));
    }

    public static boolean validVerifier(String verifier) {
        return verifier != null && VERIFIER.matcher(verifier).matches();
    }

    public static boolean validChallenge(String challenge) {
        return challenge != null && CHALLENGE.matcher(challenge).matches();
    }

    public static boolean matches(String verifier, String challenge) {
        return validVerifier(verifier) && challenge != null && ConstantTime.equals(challengeFor(verifier), challenge);
    }
}
