package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.auth.service.ResendVerificationService;
import org.springframework.stereotype.Service;

@Service
public class ResendVerificationServiceImpl implements ResendVerificationService {
    private final AuthActions actions;

    public ResendVerificationServiceImpl(AuthActions actions) {
        this.actions = actions;
    }

    @Override
    public void resend(String email) {
        actions.requestLink(email, "VERIFY");
    }
}
