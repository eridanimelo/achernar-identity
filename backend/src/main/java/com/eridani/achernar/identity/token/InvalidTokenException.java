package com.eridani.achernar.identity.token;

/** Access token inválido (assinatura, formato, {@code iss}, {@code exp}, {@code nbf}, {@code alg}, {@code typ}). */
public class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTokenException(String reason) {
        super(reason);
    }
}
