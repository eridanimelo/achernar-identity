package com.achernar.identity.account.service.impl;

import com.achernar.identity.account.service.ReadAccountService;
import com.achernar.identity.users.entity.UserView;
import com.achernar.identity.users.exception.UserNotFoundException;
import com.achernar.identity.users.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadAccountServiceImpl implements ReadAccountService {
    private final UserRepository users;

    public ReadAccountServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public UserView read(UUID id) {
        return UserView.from(users.byId(id).orElseThrow(() -> new UserNotFoundException("Conta não encontrada.")));
    }
}
