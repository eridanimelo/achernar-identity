package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.service.ActivateAccountService;
import com.achernar.identity.auth.service.AuthActions;
import org.springframework.stereotype.Service;

@Service
public class ActivateAccountServiceImpl implements ActivateAccountService {
    private final AuthActions actions;

    public ActivateAccountServiceImpl(AuthActions actions) {
        this.actions = actions;
    }

    @Override
    public void activate(String token, String password) {
        actions.consume(token, "INVITE", password);
    }
}
