package com.achernar.identity.profiles.controller;

import com.achernar.identity.profiles.dto.CreateProfileRequest;
import com.achernar.identity.profiles.dto.ProfilePageResponse;
import com.achernar.identity.profiles.dto.ProfileResponse;
import com.achernar.identity.profiles.dto.UpdateProfileRequest;
import com.achernar.identity.profiles.service.CreateProfileService;
import com.achernar.identity.profiles.service.DeleteProfileService;
import com.achernar.identity.profiles.service.GetProfileService;
import com.achernar.identity.profiles.service.ListProfilesService;
import com.achernar.identity.profiles.service.UpdateProfileService;
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
@RequestMapping("/api/admin/profiles")
public class ProfileController {
    private final ListProfilesService lists;
    private final GetProfileService reads;
    private final CreateProfileService creates;
    private final UpdateProfileService updates;
    private final DeleteProfileService deletes;

    public ProfileController(ListProfilesService lists, GetProfileService reads, CreateProfileService creates,
            UpdateProfileService updates, DeleteProfileService deletes) {
        this.lists = lists;
        this.reads = reads;
        this.creates = creates;
        this.updates = updates;
        this.deletes = deletes;
    }

    @GetMapping
    ProfilePageResponse list(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ProfilePageResponse.from(lists.list(q, active, page, size));
    }

    @GetMapping("/{id}")
    ProfileResponse get(@PathVariable UUID id) {
        return ProfileResponse.from(reads.get(id));
    }

    @PostMapping
    ResponseEntity<ProfileResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProfileRequest body) {
        var profile = creates.create(UUID.fromString(jwt.getSubject()), body.code(), body.name(), body.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProfileResponse.from(profile));
    }

    @PatchMapping("/{id}")
    ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest body) {
        return ProfileResponse.from(updates.update(UUID.fromString(jwt.getSubject()), id, body.name(),
                body.description(), body.active()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        deletes.delete(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}
