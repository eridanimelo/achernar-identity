package com.achernar.identity.profiles.controller.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}") String code,
        @NotBlank @Size(min = 2, max = 100) String name, @NotNull @Size(max = 500) String description) {
}
