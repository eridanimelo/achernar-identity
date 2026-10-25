package com.achernar.identity.account.controller.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameAccountRequest(@NotBlank @Size(min = 2, max = 100) String name) {
}
