package com.achernar.identity.profiles.controller.rest;

import com.achernar.identity.profiles.service.ProfilePage;
import java.util.List;

public record ProfilePageResponse(List<ProfileResponse> items, int page, int size, long total, long totalPages) {
    public static ProfilePageResponse from(ProfilePage profiles) {
        return new ProfilePageResponse(profiles.items().stream().map(ProfileResponse::from).toList(), profiles.page(),
                profiles.size(), profiles.total(), profiles.totalPages());
    }
}
