package com.achernar.identity.auth.dto;

public record IssuedToken(String accessToken, int expiresIn) {
}
