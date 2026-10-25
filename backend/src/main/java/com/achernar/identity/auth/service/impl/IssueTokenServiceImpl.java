package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.dto.IssuedToken;
import com.achernar.identity.auth.service.IssueTokenService;
import com.achernar.identity.auth.service.LoginService;
import org.springframework.stereotype.Service;

@Service
public class IssueTokenServiceImpl implements IssueTokenService {
    private final LoginService login;

    public IssueTokenServiceImpl(LoginService login) {
        this.login = login;
    }

    @Override
    public IssuedToken token(String email, String password) {
        var result = login.login(email, password);
        return new IssuedToken(result.accessToken(), 900);
    }
}
