package com.achernar.identity.users.controller;

import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.dto.CreateUserRequest;
import com.achernar.identity.users.dto.UpdateUserRequest;
import com.achernar.identity.users.dto.UserPageResponse;
import com.achernar.identity.users.dto.UserResponse;
import com.achernar.identity.users.service.CreateUserService;
import com.achernar.identity.users.service.DeleteUserService;
import com.achernar.identity.users.service.GetUserService;
import com.achernar.identity.users.service.ListUsersService;
import com.achernar.identity.users.service.UpdateUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {
    private final ListUsersService lists;
    private final GetUserService reads;
    private final CreateUserService creates;
    private final UpdateUserService updates;
    private final DeleteUserService deletes;

    public UserController(ListUsersService lists, GetUserService reads, CreateUserService creates,
            UpdateUserService updates, DeleteUserService deletes) {
        this.lists = lists;
        this.reads = reads;
        this.creates = creates;
        this.updates = updates;
        this.deletes = deletes;
    }

    @GetMapping
    UserPageResponse list(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return UserPageResponse.from(lists.list(q, page, size));
    }

    @GetMapping("/{id}")
    UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(reads.get(id));
    }

    @PostMapping
    ResponseEntity<UserResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(creates.create(UUID.fromString(jwt.getSubject()), body.name(), body.email(),
                        body.profiles())));
    }

    @PatchMapping("/{id}")
    UserResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest body) {
        if (body.role() == null && body.status() == null && body.name() == null && body.email() == null
                && body.profiles() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe ao menos um campo para alterar.");
        return UserResponse.from(updates.update(UUID.fromString(jwt.getSubject()), id, body.name(), body.email(),
                body.profiles(), body.role(), body.status()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        deletes.delete(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}
