package com.achernar.identity.account.service;

import com.achernar.identity.users.entity.UserView;
import java.util.UUID;

public interface ReadAccountService {
    UserView read(UUID id);
}
