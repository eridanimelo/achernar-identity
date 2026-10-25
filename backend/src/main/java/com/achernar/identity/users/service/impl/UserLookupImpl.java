package com.achernar.identity.users.service.impl;

import com.achernar.identity.users.entity.UserEntity;
import com.achernar.identity.users.repository.UserRepository;
import com.achernar.identity.users.service.UserLookup;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class UserLookupImpl implements UserLookup {
    private final UserRepository users;

    public UserLookupImpl(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public Optional<UserEntity> byId(UUID id) {
        return users.byId(id);
    }
}
