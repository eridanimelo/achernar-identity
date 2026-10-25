package com.achernar.identity.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetRequest(@NotBlank @Size(min = 43, max = 43) String token,
        @NotBlank @Size(min = 12, max = 128) String password) {
}
