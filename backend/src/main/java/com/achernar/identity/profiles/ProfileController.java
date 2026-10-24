package com.achernar.identity.profiles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/profiles")
public class ProfileController {
    public record Create(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,49}")String code,@NotBlank @Size(min=2,max=100)String name,@NotNull @Size(max=500)String description){}
    public record Update(@NotBlank @Size(min=2,max=100)String name,@NotNull @Size(max=500)String description,@NotNull Boolean active){}
    private final ProfileService profiles;
    public ProfileController(ProfileService profiles){this.profiles=profiles;}
    @GetMapping ProfileService.Page list(@RequestParam(defaultValue="") @Size(max=100)String q,@RequestParam(required=false)Boolean active,@RequestParam(defaultValue="0") @Min(0) @Max(1000000)int page,@RequestParam(defaultValue="20") @Min(1) @Max(100)int size){return profiles.list(q,active,page,size);}
    @GetMapping("/{id}") ProfileDto get(@PathVariable UUID id){return profiles.get(id);}
    @PostMapping ResponseEntity<ProfileDto> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create body){var profile=profiles.create(UUID.fromString(jwt.getSubject()),body.code(),body.name(),body.description());return ResponseEntity.status(HttpStatus.CREATED).body(profile);}
    @PatchMapping("/{id}") ProfileDto update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Update body){return profiles.update(UUID.fromString(jwt.getSubject()),id,body.name(),body.description(),body.active());}
    @DeleteMapping("/{id}") ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){profiles.delete(UUID.fromString(jwt.getSubject()),id);return ResponseEntity.noContent().build();}
}
