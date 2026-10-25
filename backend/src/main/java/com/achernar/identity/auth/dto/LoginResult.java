package com.achernar.identity.auth.dto;

import com.achernar.identity.users.entity.UserView;

public record LoginResult(UserView user, String accessToken) {
}
