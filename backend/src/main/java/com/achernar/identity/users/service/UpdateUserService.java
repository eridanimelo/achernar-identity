package com.achernar.identity.users.service;

import com.achernar.identity.users.entity.UserView;
import java.util.List;
import java.util.UUID;

public interface UpdateUserService {
    UserView update(UUID actor, UUID target, String name, String email, List<String> codes, String role, String status);
}
