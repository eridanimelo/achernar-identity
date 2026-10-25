package com.achernar.identity.profiles.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() {
        super("Perfil não encontrado.");
    }
}
