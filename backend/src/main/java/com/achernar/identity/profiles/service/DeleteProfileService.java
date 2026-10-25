package com.achernar.identity.profiles.service;

import java.util.UUID;

public interface DeleteProfileService {
    void delete(UUID actor, UUID id);
}
