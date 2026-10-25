package com.achernar.identity.auth;

import com.achernar.identity.mail.MailQueue;
import com.achernar.identity.shared.*;
import com.achernar.identity.users.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActionRepository actions = mock(ActionRepository.class);
    private final MailQueue mail = mock(MailQueue.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final Audit audit = mock(Audit.class);
    private AuthServiceImpl service;

    @BeforeEach
    void setup() {
        when(passwords.encode(anyString())).thenReturn("encoded");
        service = new AuthServiceImpl(users, actions, mail, passwords, audit, "http://localhost");
    }

    @Test
    void normalizesEmailAndHashesToken() {
        assertEquals("a@example.com", AuthServiceImpl.normalize(" A@EXAMPLE.COM "));
        assertEquals(64, AuthServiceImpl.hash("token").length());
        assertNotEquals("token", AuthServiceImpl.hash("token"));
    }

    @Test
    void invalidCredentialsNeverRevealAccount() {
        when(users.byEmail(anyString())).thenReturn(Optional.empty());
        when(passwords.matches(anyString(), anyString())).thenReturn(false);
        assertThrows(ApiException.class, () -> service.login("a@example.com", "password"));
        verifyNoInteractions(audit);
    }

    @Test
    void unverifiedCannotLogin() {
        var user = new UserEntity(UUID.randomUUID(), "Pessoa", "a@example.com", "encoded", "USER", "ACTIVE", false, 0,
                Instant.now());
        when(users.byEmail(anyString())).thenReturn(Optional.of(user));
        when(users.lock(user.id())).thenReturn(Optional.of(user));
        when(passwords.matches(anyString(), anyString())).thenReturn(true);
        var error = assertThrows(ApiException.class, () -> service.login(user.email(), "password"));
        assertEquals(403, error.status().value());
        verifyNoInteractions(audit);
    }

    @Test
    void invalidActionDoesNotChangePassword() {
        when(actions.find(anyString(), anyString())).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.consume("bogus", "RESET", "newpassword"));
        verifyNoInteractions(users);
    }

    @Test
    void trimmedNameStillRespectsMinimum() {
        assertThrows(ApiException.class, () -> UserNames.normalize(" a "));
        assertEquals("Ana", UserNames.normalize(" Ana "));
    }
}
