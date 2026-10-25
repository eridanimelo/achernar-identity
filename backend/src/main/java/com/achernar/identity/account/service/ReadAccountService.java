package com.achernar.identity.account.service;

import com.achernar.identity.users.domain.exception.UserNotFoundException;
import com.achernar.identity.users.domain.model.UserView;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadAccountService {
    private final UserRepository users;

    public ReadAccountService(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserView read(UUID id) {
        return UserView.from(users.byId(id).orElseThrow(() -> new UserNotFoundException("Conta não encontrada.")));
    }
}
