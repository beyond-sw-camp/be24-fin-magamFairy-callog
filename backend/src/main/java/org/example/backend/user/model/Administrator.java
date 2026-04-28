package org.example.backend.user.model;


import lombok.RequiredArgsConstructor;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Administrator implements ApplicationRunner {

    @Value("${admin.login-id}") private String ADMIN_LOGIN_ID;
    @Value("${admin.email}") private String ADMIN_EMAIL;
    @Value("${admin.name}") private String ADMIN_NAME;
    @Value("${admin.role}") private String ADMIN_ROLE;
    @Value("${admin.password}") private String ADMIN_PASSWORD;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        User admin = userRepository.findByLoginId(ADMIN_LOGIN_ID)
                .or(() -> userRepository.findByEmail(ADMIN_EMAIL))
                .orElseGet(() -> User.builder()
                        .loginId(ADMIN_LOGIN_ID)
                        .email(ADMIN_EMAIL)
                        .name(ADMIN_NAME)
                        .enable(true)
                        .role(ADMIN_ROLE)
                        .passwordResetRequired(false)
                        .accountStatus(UserAccountStatus.ACTIVE)
                        .build());

        admin.setLoginId(ADMIN_LOGIN_ID);
        admin.setEmail(ADMIN_EMAIL);
        admin.setName(ADMIN_NAME);
        admin.setEnable(true);
        admin.setRole(ADMIN_ROLE);
        admin.setPasswordResetRequired(false);
        admin.setAccountStatus(UserAccountStatus.ACTIVE);

        if (shouldResetPassword(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        }

        userRepository.save(admin);
    }

    private boolean shouldResetPassword(String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return true;
        }

        try {
            return !passwordEncoder.matches(ADMIN_PASSWORD, storedPassword);
        } catch (IllegalArgumentException exception) {
            // Legacy plain-text or unknown encoded passwords should be replaced
            // with the configured administrator password on startup.
            return true;
        }
    }
}
