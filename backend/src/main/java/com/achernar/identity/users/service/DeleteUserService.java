package com.achernar.identity.users.service;

import java.util.UUID;

public interface DeleteUserService {
    void delete(UUID actor, UUID target);
}
