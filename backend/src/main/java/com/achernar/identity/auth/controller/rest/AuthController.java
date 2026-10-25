package com.achernar.identity.auth.controller.rest;

import com.achernar.identity.auth.service.ActivateAccountService;
import com.achernar.identity.auth.service.ForgotPasswordService;
import com.achernar.identity.auth.service.IssueTokenService;
import com.achernar.identity.auth.service.LoginService;
import com.achernar.identity.auth.service.LogoutService;
import com.achernar.identity.auth.service.RegisterService;
import com.achernar.identity.auth.service.ResendVerificationService;
import com.achernar.identity.auth.service.ResetPasswordService;
import com.achernar.identity.auth.service.SessionService;
import com.achernar.identity.auth.service.VerifyEmailService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegisterService registrations;
    private final LoginService logins;
    private final IssueTokenService tokenIssuer;
    private final ResendVerificationService resends;
    private final ForgotPasswordService forgotPasswords;
    private final VerifyEmailService verifications;
    private final ResetPasswordService passwordResets;
    private final ActivateAccountService activations;
    private final SessionService sessions;
    private final LogoutService logouts;
    private final boolean secure;

    public AuthController(RegisterService registrations, LoginService logins, IssueTokenService tokenIssuer,
            ResendVerificationService resends, ForgotPasswordService forgotPasswords, VerifyEmailService verifications,
            ResetPasswordService passwordResets, ActivateAccountService activations, SessionService sessions,
            LogoutService logouts, @Value("${app.secure-cookie}") boolean secure) {
        this.registrations = registrations;
        this.logins = logins;
        this.tokenIssuer = tokenIssuer;
        this.resends = resends;
        this.forgotPasswords = forgotPasswords;
        this.verifications = verifications;
        this.passwordResets = passwordResets;
        this.activations = activations;
        this.sessions = sessions;
        this.logouts = logouts;
        this.secure = secure;
    }

    @PostMapping("/register")
    ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest body) {
        registrations.register(body.name(), body.email(), body.password());
        return generic();
    }

    @PostMapping("/login")
    AuthUserResponse login(@Valid @RequestBody LoginRequest body, HttpServletResponse res) {
        var result = logins.login(body.email(), body.password());
        res.addHeader(HttpHeaders.SET_COOKIE, cookie(result.accessToken(), 900).toString());
        return AuthUserResponse.from(result.user());
    }

    @PostMapping("/resend")
    ResponseEntity<MessageResponse> resend(@Valid @RequestBody EmailRequest body) {
        resends.resend(body.email());
        return generic();
    }

    @PostMapping("/forgot-password")
    ResponseEntity<MessageResponse> forgot(@Valid @RequestBody EmailRequest body) {
        forgotPasswords.request(body.email());
        return generic();
    }

    @PostMapping("/verify")
    MessageResponse verify(@Valid @RequestBody TokenRequest body) {
        verifications.verify(body.token());
        return new MessageResponse("E-mail confirmado. Você já pode entrar.");
    }

    @PostMapping("/reset-password")
    MessageResponse reset(@Valid @RequestBody ResetRequest body) {
        passwordResets.reset(body.token(), body.password());
        return new MessageResponse("Senha redefinida. Entre com a nova senha.");
    }

    @PostMapping("/activate")
    MessageResponse activate(@Valid @RequestBody ResetRequest body) {
        activations.activate(body.token(), body.password());
        return new MessageResponse("Conta ativada. Entre com sua nova senha.");
    }

    @PostMapping("/token")
    TokenResponse token(@Valid @RequestBody LoginRequest body) {
        return TokenResponse.from(tokenIssuer.token(body.email(), body.password()));
    }

    @GetMapping("/session")
    SessionResponse session(@AuthenticationPrincipal Jwt jwt) {
        return SessionResponse.from(sessions.session(jwt));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt, HttpServletResponse res) {
        logouts.logout(UUID.fromString(jwt.getSubject()));
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie cookie(String value, long seconds) {
        return ResponseCookie.from("IDENTITY_SESSION", value).httpOnly(true).secure(secure).sameSite("Strict").path("/")
                .maxAge(Duration.ofSeconds(seconds)).build();
    }

    private ResponseEntity<MessageResponse> generic() {
        return ResponseEntity.accepted().body(new MessageResponse(
                "Se os dados permitirem a operação, você receberá um e-mail com as instruções."));
    }
}
