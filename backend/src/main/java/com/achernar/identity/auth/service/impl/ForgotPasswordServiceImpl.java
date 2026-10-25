package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.auth.service.ForgotPasswordService;
import org.springframework.stereotype.Service;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {
    private final AuthActions actions;

    public ForgotPasswordServiceImpl(AuthActions actions) {
        this.actions = actions;
    }

    @Override
    public void request(String email) {
        actions.requestLink(email, "RESET");
    }
}
