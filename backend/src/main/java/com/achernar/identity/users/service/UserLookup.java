package com.achernar.identity.users.service;

import com.achernar.identity.users.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;

public interface UserLookup {
    Optional<UserEntity> byId(UUID id);
}
