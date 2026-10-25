package com.achernar.identity.profiles.domain.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() {
        super("Perfil não encontrado.");
    }
}
