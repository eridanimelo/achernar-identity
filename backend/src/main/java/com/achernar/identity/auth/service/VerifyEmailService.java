package com.achernar.identity.auth.service;

public interface VerifyEmailService {
    void verify(String token);
}
