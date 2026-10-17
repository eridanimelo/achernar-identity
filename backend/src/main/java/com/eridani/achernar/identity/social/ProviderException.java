package com.eridani.achernar.identity.social;

/** Falha ao falar com o provedor: {@code unavailable} (rede/timeout) ou resposta/token inválido. */
public class ProviderException extends RuntimeException {

    private final boolean unavailable;

    public ProviderException(String message, boolean unavailable, Throwable cause) {
        super(message, cause);
        this.unavailable = unavailable;
    }

    public boolean unavailable() {
        return unavailable;
    }

    public static ProviderException invalid(String message) {
        return new ProviderException(message, false, null);
    }

    public static ProviderException unavailable(String message, Throwable cause) {
        return new ProviderException(message, true, cause);
    }
}
