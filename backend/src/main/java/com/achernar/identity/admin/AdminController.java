package com.achernar.identity.admin;

import com.achernar.identity.shared.ApiException;
import com.achernar.identity.users.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {
    public record Create(@NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotEmpty @Size(max = 20) List<@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}") String> profiles) {
    }

    public record Change(@Size(min = 2, max = 100) String name, @Email @Size(min = 3, max = 254) String email,
            @Size(min = 1, max = 20) List<@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}") String> profiles,
            @Pattern(regexp = "USER|ADMIN") String role, @Pattern(regexp = "ACTIVE|BLOCKED") String status) {
    }

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    AdminService.Page list(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return admin.list(q, page, size);
    }

    @GetMapping("/{id}")
    UserDto get(@PathVariable UUID id) {
        return admin.get(id);
    }

    @PostMapping
    ResponseEntity<UserDto> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Create body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(admin.create(UUID.fromString(jwt.getSubject()), body.name(), body.email(), body.profiles()));
    }

    @PatchMapping("/{id}")
    UserDto change(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody Change body) {
        if (body.role() == null && body.status() == null && body.name() == null && body.email() == null
                && body.profiles() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe ao menos um campo para alterar.");
        return admin.edit(UUID.fromString(jwt.getSubject()), id, body.name(), body.email(), body.profiles(),
                body.role(), body.status());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        admin.delete(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}
