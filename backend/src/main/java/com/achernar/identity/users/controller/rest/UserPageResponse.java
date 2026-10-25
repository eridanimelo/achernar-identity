package com.achernar.identity.users.controller.rest;

import com.achernar.identity.users.service.UserPage;
import java.util.List;

public record UserPageResponse(List<UserResponse> items, int page, int size, long total, long totalPages) {
    public static UserPageResponse from(UserPage users) {
        return new UserPageResponse(users.items().stream().map(UserResponse::from).toList(), users.page(),
                users.size(), users.total(), users.totalPages());
    }
}
