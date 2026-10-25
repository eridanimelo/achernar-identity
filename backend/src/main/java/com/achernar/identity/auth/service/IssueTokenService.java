package com.achernar.identity.auth.service;

import org.springframework.stereotype.Service;

@Service
public class IssueTokenService {
    private final LoginService login;

    public IssueTokenService(LoginService login) {
        this.login = login;
    }

    public IssuedToken token(String email, String password) {
        var result = login.login(email, password);
        return new IssuedToken(result.accessToken(), 900);
    }
}
