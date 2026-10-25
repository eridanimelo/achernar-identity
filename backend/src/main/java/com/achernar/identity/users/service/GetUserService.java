package com.achernar.identity.users.service;

import com.achernar.identity.users.domain.exception.UserNotFoundException;
import com.achernar.identity.users.domain.model.UserView;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserService {
    private final UserRepository users;

    public GetUserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserView get(UUID id) {
        return UserView.from(users.byId(id).filter(user -> !user.status().equals("DELETED"))
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado.")));
    }
}
