package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.auth.service.ResetPasswordService;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordServiceImpl implements ResetPasswordService {
    private final AuthActions actions;

    public ResetPasswordServiceImpl(AuthActions actions) {
        this.actions = actions;
    }

    @Override
    public void reset(String token, String password) {
        actions.consume(token, "RESET", password);
    }
}
