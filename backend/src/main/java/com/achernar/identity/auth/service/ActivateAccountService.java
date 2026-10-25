package com.achernar.identity.auth.service;

import org.springframework.stereotype.Service;

@Service
public class ActivateAccountService {
    private final AuthActions actions;

    public ActivateAccountService(AuthActions actions) {
        this.actions = actions;
    }

    public void activate(String token, String password) {
        actions.consume(token, "INVITE", password);
    }
}
