package com.achernar.identity.users.service;

import com.achernar.identity.users.entity.UserView;
import java.util.List;
import java.util.UUID;

public interface CreateUserService {
    UserView create(UUID actor, String name, String email, List<String> codes);
}
