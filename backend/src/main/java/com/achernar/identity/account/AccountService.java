package com.achernar.identity.account;

import com.achernar.identity.users.UserDto;
import java.util.UUID;

public interface AccountService {
    UserDto me(UUID id);

    UserDto rename(UUID id, String name);
}
