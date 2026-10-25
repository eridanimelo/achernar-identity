package com.achernar.identity.users.dto;

import com.achernar.identity.users.entity.UserView;
import java.util.List;

public record UserPage(List<UserView> items, int page, int size, long total, long totalPages) {
}
