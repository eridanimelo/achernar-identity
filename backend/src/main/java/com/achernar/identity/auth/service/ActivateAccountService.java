package com.achernar.identity.auth.service;

public interface ActivateAccountService {
    void activate(String token, String password);
}
