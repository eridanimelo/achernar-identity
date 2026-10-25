package com.achernar.identity.auth.service;

public interface ResetPasswordService {
    void reset(String token, String password);
}
