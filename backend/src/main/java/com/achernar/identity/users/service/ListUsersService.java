package com.achernar.identity.users.service;

import com.achernar.identity.users.dto.UserPage;

public interface ListUsersService {
    UserPage list(String query, int page, int size);
}
