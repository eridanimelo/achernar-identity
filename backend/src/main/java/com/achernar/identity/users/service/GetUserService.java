package com.achernar.identity.users.service;

import com.achernar.identity.users.entity.UserView;
import java.util.UUID;

public interface GetUserService {
    UserView get(UUID id);
}
