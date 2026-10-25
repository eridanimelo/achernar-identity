package com.achernar.identity.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateUserRequest(@NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotEmpty @Size(max = 20) List<@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}") String> profiles) {
}
