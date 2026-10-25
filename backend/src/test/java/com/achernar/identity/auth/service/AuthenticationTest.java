package com.achernar.identity.auth.service;

import com.achernar.identity.auth.domain.repository.ActionRepository;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.shared.mail.MailQueue;
import com.achernar.identity.shared.security.TokenService;
import com.achernar.identity.users.domain.model.UserEntity;
import com.achernar.identity.users.domain.model.UserNames;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthenticationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActionRepository actions = mock(ActionRepository.class);
    private final MailQueue mail = mock(MailQueue.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final Audit audit = mock(Audit.class);
    private final TokenService tokens = mock(TokenService.class);
    private AuthActions authActions;
    private LoginService login;

    @BeforeEach
    void setup() {
        when(passwords.encode(anyString())).thenReturn("encoded");
        authActions = new AuthActions(users, actions, mail, passwords, audit, "http://localhost");
        login = new LoginService(users, passwords, audit, tokens);
    }

    @Test
    void normalizesEmailAndHashesToken() {
        assertEquals("a@example.com", AuthActions.normalize(" A@EXAMPLE.COM "));
        assertEquals(64, AuthActions.hash("token").length());
        assertNotEquals("token", AuthActions.hash("token"));
    }

    @Test
    void invalidCredentialsNeverRevealAccount() {
        when(users.byEmail(anyString())).thenReturn(Optional.empty());
        when(passwords.matches(anyString(), anyString())).thenReturn(false);
        assertThrows(ApiException.class, () -> login.login("a@example.com", "password"));
        verifyNoInteractions(audit);
    }

    @Test
    void unverifiedCannotLogin() {
        var user = new UserEntity(UUID.randomUUID(), "Pessoa", "a@example.com", "encoded", "USER", "ACTIVE", false, 0,
                Instant.now());
        when(users.byEmail(anyString())).thenReturn(Optional.of(user));
        when(users.lock(user.id())).thenReturn(Optional.of(user));
        when(passwords.matches(anyString(), anyString())).thenReturn(true);
        var error = assertThrows(ApiException.class, () -> login.login(user.email(), "password"));
        assertEquals(403, error.status().value());
        verifyNoInteractions(audit);
    }

    @Test
    void invalidActionDoesNotChangePassword() {
        when(actions.find(anyString(), anyString())).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> authActions.consume("bogus", "RESET", "newpassword"));
        verifyNoInteractions(users);
    }

    @Test
    void trimmedNameStillRespectsMinimum() {
        assertThrows(ApiException.class, () -> UserNames.normalize(" a "));
        assertEquals("Ana", UserNames.normalize(" Ana "));
    }
}
