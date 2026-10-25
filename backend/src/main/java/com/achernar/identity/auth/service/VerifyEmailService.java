package com.achernar.identity.auth.service;

import org.springframework.stereotype.Service;

@Service
public class VerifyEmailService {
    private final AuthActions actions;

    public VerifyEmailService(AuthActions actions) {
        this.actions = actions;
    }

    public void verify(String token) {
        actions.consume(token, "VERIFY", null);
    }
}
