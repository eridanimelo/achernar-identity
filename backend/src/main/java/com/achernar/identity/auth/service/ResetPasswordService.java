package com.achernar.identity.auth.service;

import org.springframework.stereotype.Service;

@Service
public class ResetPasswordService {
    private final AuthActions actions;

    public ResetPasswordService(AuthActions actions) {
        this.actions = actions;
    }

    public void reset(String token, String password) {
        actions.consume(token, "RESET", password);
    }
}
