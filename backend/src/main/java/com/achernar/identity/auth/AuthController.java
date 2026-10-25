package com.achernar.identity.auth;

import com.achernar.identity.security.TokenService;
import com.achernar.identity.users.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Register(@NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(min = 12, max = 128) String password) {
    }

    public record Login(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {
    }

    public record EmailInput(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record TokenInput(@NotBlank @Size(min = 43, max = 43) String token) {
    }

    public record Reset(@NotBlank @Size(min = 43, max = 43) String token,
            @NotBlank @Size(min = 12, max = 128) String password) {
    }

    private final AuthService auth;
    private final TokenService tokens;
    private final UserLookup users;
    private final boolean secure;

    public AuthController(AuthService auth, TokenService tokens, UserLookup users,
            @Value("${app.secure-cookie}") boolean secure) {
        this.auth = auth;
        this.tokens = tokens;
        this.users = users;
        this.secure = secure;
    }

    @PostMapping("/register")
    ResponseEntity<?> register(@Valid @RequestBody Register body) {
        auth.register(body.name(), body.email(), body.password());
        return generic();
    }

    @PostMapping("/login")
    UserDto login(@Valid @RequestBody Login body, HttpServletResponse res) {
        var user = auth.login(body.email(), body.password());
        var entity = users.byId(user.id()).orElseThrow();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie(tokens.issue(entity), 900).toString());
        return user;
    }

    @PostMapping("/resend")
    ResponseEntity<?> resend(@Valid @RequestBody EmailInput body) {
        auth.requestLink(body.email(), "VERIFY");
        return generic();
    }

    @PostMapping("/forgot-password")
    ResponseEntity<?> forgot(@Valid @RequestBody EmailInput body) {
        auth.requestLink(body.email(), "RESET");
        return generic();
    }

    @PostMapping("/verify")
    Map<String, String> verify(@Valid @RequestBody TokenInput body) {
        auth.consume(body.token(), "VERIFY", null);
        return Map.of("message", "E-mail confirmado. Você já pode entrar.");
    }

    @PostMapping("/reset-password")
    Map<String, String> reset(@Valid @RequestBody Reset body) {
        auth.consume(body.token(), "RESET", body.password());
        return Map.of("message", "Senha redefinida. Entre com a nova senha.");
    }

    @PostMapping("/activate")
    Map<String, String> activate(@Valid @RequestBody Reset body) {
        auth.consume(body.token(), "INVITE", body.password());
        return Map.of("message", "Conta ativada. Entre com sua nova senha.");
    }

    @PostMapping("/token")
    Map<String, Object> token(@Valid @RequestBody Login body) {
        var user = auth.login(body.email(), body.password());
        var entity = users.byId(user.id()).orElseThrow();
        return Map.of("accessToken", tokens.issue(entity), "tokenType", "Bearer", "expiresIn", 900);
    }

    @GetMapping("/session")
    Map<String, Object> session(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("sub", jwt.getSubject(), "profiles", jwt.getClaimAsStringList("roles"), "active", true,
                "expiresAt", jwt.getExpiresAt().toString());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt, HttpServletResponse res) {
        auth.logout(UUID.fromString(jwt.getSubject()));
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie cookie(String value, long seconds) {
        return ResponseCookie.from("IDENTITY_SESSION", value).httpOnly(true).secure(secure).sameSite("Strict").path("/")
                .maxAge(Duration.ofSeconds(seconds)).build();
    }

    private ResponseEntity<?> generic() {
        return ResponseEntity.accepted().body(
                Map.of("message", "Se os dados permitirem a operação, você receberá um e-mail com as instruções."));
    }
}
