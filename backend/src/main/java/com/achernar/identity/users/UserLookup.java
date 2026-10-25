package com.achernar.identity.users;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class UserLookup {
    private final UserRepository users;

    public UserLookup(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<UserEntity> byId(UUID id) {
        return users.byId(id);
    }
}
