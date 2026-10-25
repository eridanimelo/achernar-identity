package com.achernar.identity.shared.config;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.users.repository.UserRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwords, @Value("${app.admin-email}") String email,
            @Value("${app.admin-password}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank())
            return;
        if (!email.matches("[^ @]+@[^ @]+\\.[^ @]+") || password.length() < 12 || password.length() > 128)
            throw new IllegalStateException(
                    "ADMIN_EMAIL e ADMIN_PASSWORD devem ser válidos; senha entre 12 e 128 caracteres.");
        users.create(UUID.randomUUID(), "Administrador", AuthActions.normalize(email), passwords.encode(password),
                "ADMIN", true);
    }
}
