package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.dto.SessionView;
import com.achernar.identity.auth.service.SessionService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class SessionServiceImpl implements SessionService {
    @Override
    public SessionView session(Jwt jwt) {
        return new SessionView(jwt.getSubject(), jwt.getClaimAsStringList("roles"), true,
                jwt.getExpiresAt().toString());
    }
}
