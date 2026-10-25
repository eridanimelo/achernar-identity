package com.achernar.identity.account.controller.rest;

import com.achernar.identity.account.service.ReadAccountService;
import com.achernar.identity.account.service.RenameAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class AccountController {
    private final ReadAccountService reads;
    private final RenameAccountService renames;

    public AccountController(ReadAccountService reads, RenameAccountService renames) {
        this.reads = reads;
        this.renames = renames;
    }

    @GetMapping
    AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(reads.read(UUID.fromString(jwt.getSubject())));
    }

    @PatchMapping
    AccountResponse rename(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RenameAccountRequest body) {
        return AccountResponse.from(renames.rename(UUID.fromString(jwt.getSubject()), body.name()));
    }
}
