package com.achernar.identity.users.service;

import com.achernar.identity.users.entity.UserEntity;
import java.util.List;
import java.util.UUID;

public interface UserAccessService {
    void lockAdmin(UUID actor);

    void revokeProfile(UUID id);

    UserEntity lockTarget(UUID actor, UUID target);

    void protect(UserEntity user, List<String> codes, String status, boolean newEmail);
}
