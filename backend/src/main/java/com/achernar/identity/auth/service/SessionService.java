package com.achernar.identity.auth.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    public SessionView session(Jwt jwt) {
        return new SessionView(jwt.getSubject(), jwt.getClaimAsStringList("roles"), true,
                jwt.getExpiresAt().toString());
    }
}
