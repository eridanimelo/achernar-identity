package com.achernar.identity.profiles.service;

import java.util.List;
import java.util.UUID;

public interface ProfileResolver {
    List<UUID> resolve(List<String> codes);
}
