package com.achernar.identity.auth.service;

import org.springframework.stereotype.Service;

@Service
public class ForgotPasswordService {
    private final AuthActions actions;

    public ForgotPasswordService(AuthActions actions) {
        this.actions = actions;
    }

    public void request(String email) {
        actions.requestLink(email, "RESET");
    }
}
