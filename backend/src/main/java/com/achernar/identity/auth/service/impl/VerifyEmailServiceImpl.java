package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.auth.service.VerifyEmailService;
import org.springframework.stereotype.Service;

@Service
public class VerifyEmailServiceImpl implements VerifyEmailService {
    private final AuthActions actions;

    public VerifyEmailServiceImpl(AuthActions actions) {
        this.actions = actions;
    }

    @Override
    public void verify(String token) {
        actions.consume(token, "VERIFY", null);
    }
}
