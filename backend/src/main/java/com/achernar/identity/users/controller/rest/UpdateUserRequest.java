package com.achernar.identity.users.controller.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateUserRequest(@Size(min = 2, max = 100) String name,
        @Email @Size(min = 3, max = 254) String email,
        @Size(min = 1, max = 20) List<@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}") String> profiles,
        @Pattern(regexp = "USER|ADMIN") String role, @Pattern(regexp = "ACTIVE|BLOCKED") String status) {
}
