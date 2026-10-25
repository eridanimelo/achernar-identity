package com.achernar.identity.profiles.dto;

import com.achernar.identity.profiles.entity.ProfileView;
import java.util.List;

public record ProfilePage(List<ProfileView> items, int page, int size, long total, long totalPages) {
}
