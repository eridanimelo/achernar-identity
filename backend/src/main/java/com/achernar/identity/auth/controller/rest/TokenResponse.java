package com.achernar.identity.auth.controller.rest;

import com.achernar.identity.auth.service.IssuedToken;

public record TokenResponse(String accessToken, String tokenType, int expiresIn) {
    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.accessToken(), "Bearer", token.expiresIn());
    }
}
