package com.achernar.identity.users.service;

import com.achernar.identity.users.domain.model.UserView;
import java.util.List;

public record UserPage(List<UserView> items, int page, int size, long total, long totalPages) {
}
