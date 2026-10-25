package com.achernar.identity.users.service;

import com.achernar.identity.users.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersService {
    private final UserRepository users;

    public ListUsersService(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserPage list(String query, int page, int size) {
        long total = users.count(query);
        return new UserPage(users.list(query, page, size), page, size, total, (total + size - 1) / size);
    }
}
