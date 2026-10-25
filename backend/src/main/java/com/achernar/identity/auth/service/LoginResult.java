package com.achernar.identity.auth.service;

import com.achernar.identity.users.domain.model.UserView;

public record LoginResult(UserView user, String accessToken) {
}
