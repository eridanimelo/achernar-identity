package com.achernar.identity.auth.service;

import com.achernar.identity.auth.dto.LoginResult;

public interface LoginService {
    LoginResult login(String email, String password);
}
