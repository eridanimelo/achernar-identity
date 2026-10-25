package com.achernar.identity.auth.service;

import org.springframework.stereotype.Service;

@Service
public class ResendVerificationService {
    private final AuthActions actions;

    public ResendVerificationService(AuthActions actions) {
        this.actions = actions;
    }

    public void resend(String email) {
        actions.requestLink(email, "VERIFY");
    }
}
