package com.achernar.identity.auth.dto;

import com.achernar.identity.auth.dto.SessionView;
import java.util.List;

public record SessionResponse(String sub, List<String> profiles, boolean active, String expiresAt) {
    public static SessionResponse from(SessionView session) {
        return new SessionResponse(session.sub(), session.profiles(), session.active(), session.expiresAt());
    }
}
