package com.achernar.identity.account;

import com.achernar.identity.users.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class AccountController {
    public record Rename(@NotBlank @Size(min = 2, max = 100) String name) {
    }

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return accounts.me(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping
    UserDto rename(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Rename body) {
        return accounts.rename(UUID.fromString(jwt.getSubject()), body.name());
    }
}
