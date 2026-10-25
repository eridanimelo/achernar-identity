package com.achernar.identity.auth.service;

import com.achernar.identity.auth.dto.IssuedToken;

public interface IssueTokenService {
    IssuedToken token(String email, String password);
}
