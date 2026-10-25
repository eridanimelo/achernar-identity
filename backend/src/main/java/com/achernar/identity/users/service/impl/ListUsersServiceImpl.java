package com.achernar.identity.users.service.impl;

import com.achernar.identity.users.dto.UserPage;
import com.achernar.identity.users.repository.UserRepository;
import com.achernar.identity.users.service.ListUsersService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersServiceImpl implements ListUsersService {
    private final UserRepository users;

    public ListUsersServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public UserPage list(String query, int page, int size) {
        long total = users.count(query);
        return new UserPage(users.list(query, page, size), page, size, total, (total + size - 1) / size);
    }
}
