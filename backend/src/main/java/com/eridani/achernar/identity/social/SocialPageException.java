package com.eridani.achernar.identity.social;

/** Erro que exige a página HTML do Identity (nunca redireciona — social-login.md §3/§8). */
public class SocialPageException extends RuntimeException {

    private final int status;

    public SocialPageException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
