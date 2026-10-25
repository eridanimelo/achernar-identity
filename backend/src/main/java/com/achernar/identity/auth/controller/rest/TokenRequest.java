package com.achernar.identity.auth.controller.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRequest(@NotBlank @Size(min = 43, max = 43) String token) {
}
