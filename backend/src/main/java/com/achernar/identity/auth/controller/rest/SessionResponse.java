package com.achernar.identity.auth.controller.rest;

import com.achernar.identity.auth.service.SessionView;
import java.util.List;

public record SessionResponse(String sub, List<String> profiles, boolean active, String expiresAt) {
    public static SessionResponse from(SessionView session) {
        return new SessionResponse(session.sub(), session.profiles(), session.active(), session.expiresAt());
    }
}
