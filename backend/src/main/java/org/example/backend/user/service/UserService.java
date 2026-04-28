package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BaseException;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.model.UserDto;
import org.example.backend.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private static final String LOGIN_ID_PREFIX = "CALLOG";
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final int TEMPORARY_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public UserDto.CreateUserRes createUserByAdmin(UserDto.CreateUserReq dto) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        String teamCode = requireText(dto.teamCode(), "teamCode");
        String name = requireText(dto.name(), "name");
        String loginId = createUniqueLoginId(teamCode, name);
        String email = normalizeOptional(dto.email());

        if (email != null && userRepository.existsByEmail(email)) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_DUPLICATE_EMAIL);
        }

        String temporaryPassword = generateTemporaryPassword();
        User user = User.builder()
                .loginId(loginId)
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(temporaryPassword))
                .enable(true)
                .role(resolveRole(dto.role()))
                .passwordResetRequired(true)
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();

        return UserDto.CreateUserRes.from(userRepository.save(user), temporaryPassword);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = normalizeOptional(username);
        if (normalizedUsername == null) {
            throw new UsernameNotFoundException("user not found");
        }

        User user = userRepository.findByLoginId(normalizedUsername)
                .or(() -> userRepository.findByEmail(normalizedUsername))
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        return AuthUserDetails.from(user);
    }

    private String createUniqueLoginId(String teamCode, String name) {
        String baseLoginId = String.join("_", LOGIN_ID_PREFIX, normalizeIdentifier(teamCode), normalizeIdentifier(name));
        String candidate = baseLoginId;
        int suffix = 2;

        while (userRepository.existsByLoginId(candidate)) {
            candidate = baseLoginId + suffix;
            suffix += 1;
        }

        return candidate;
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index += 1) {
            int randomIndex = secureRandom.nextInt(PASSWORD_CHARACTERS.length());
            password.append(PASSWORD_CHARACTERS.charAt(randomIndex));
        }
        return password.toString();
    }

    private String resolveRole(String role) {
        String normalizedRole = normalizeOptional(role);
        if (normalizedRole == null) {
            return DEFAULT_ROLE;
        }

        String upperRole = normalizedRole.toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(upperRole)) {
            return "ROLE_ADMIN";
        }
        if ("USER".equals(upperRole)) {
            return DEFAULT_ROLE;
        }
        if ("ROLE_ADMIN".equals(upperRole) || "ROLE_USER".equals(upperRole)) {
            return upperRole;
        }

        return DEFAULT_ROLE;
    }

    private String normalizeIdentifier(String value) {
        return requireText(value, "identifier").replaceAll("\\s+", "");
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
