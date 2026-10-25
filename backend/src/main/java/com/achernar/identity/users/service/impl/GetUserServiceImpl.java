package com.achernar.identity.users.service.impl;

import com.achernar.identity.users.entity.UserView;
import com.achernar.identity.users.exception.UserNotFoundException;
import com.achernar.identity.users.repository.UserRepository;
import com.achernar.identity.users.service.GetUserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserServiceImpl implements GetUserService {
    private final UserRepository users;

    public GetUserServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public UserView get(UUID id) {
        return UserView.from(users.byId(id).filter(user -> !user.status().equals("DELETED"))
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado.")));
    }
}
