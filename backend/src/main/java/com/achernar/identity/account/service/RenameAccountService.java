package com.achernar.identity.account.service;

import com.achernar.identity.users.entity.UserView;
import java.util.UUID;

public interface RenameAccountService {
    UserView rename(UUID id, String name);
}
