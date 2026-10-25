package com.achernar.identity.profiles.controller.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@NotBlank @Size(min = 2, max = 100) String name,
        @NotNull @Size(max = 500) String description, @NotNull Boolean active) {
}
