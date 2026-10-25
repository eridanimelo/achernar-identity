package com.achernar.identity.auth.dto;

import java.util.List;

public record SessionView(String sub, List<String> profiles, boolean active, String expiresAt) {
}
