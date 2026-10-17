package com.eridani.achernar.identity.shared.crypto;

import java.security.MessageDigest;

/** Comparações de segredo em tempo constante (SEC-33), sem vazar o comprimento. */
public final class ConstantTime {

    private ConstantTime() {
    }

    public static boolean equals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(Sha256.digest(a), Sha256.digest(b));
    }
}
