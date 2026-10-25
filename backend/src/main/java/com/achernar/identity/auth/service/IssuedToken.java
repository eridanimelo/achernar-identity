package com.achernar.identity.auth.service;

public record IssuedToken(String accessToken, int expiresIn) {
}
