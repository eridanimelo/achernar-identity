package com.achernar.identity.auth.service;

import com.achernar.identity.auth.dto.SessionView;
import org.springframework.security.oauth2.jwt.Jwt;

public interface SessionService {
    SessionView session(Jwt jwt);
}
