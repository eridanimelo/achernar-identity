package com.achernar.identity.auth.dto;

import com.achernar.identity.auth.dto.IssuedToken;

public record TokenResponse(String accessToken, String tokenType, int expiresIn) {
    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.accessToken(), "Bearer", token.expiresIn());
    }
}
