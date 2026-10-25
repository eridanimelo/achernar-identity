package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.domain.model.ProfileView;
import java.util.List;

public record ProfilePage(List<ProfileView> items, int page, int size, long total, long totalPages) {
}
