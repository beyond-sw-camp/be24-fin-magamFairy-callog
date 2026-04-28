package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BaseException;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.model.UserDto;
import org.example.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
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
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String MANAGER_ROLE = "ROLE_MANAGER";
    private static final String USER_ROLE = "ROLE_USER";
    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final int TEMPORARY_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public UserDto.CreateUserRes createUser(UserDto.CreateUserReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        String creatorRole = resolveCreatorRole(authentication);
        String targetRole = resolveCreatableRole(dto.role(), creatorRole);
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
                .role(targetRole)
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

    private String resolveCreatorRole(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("creator authentication is required.");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_ROLE.equals(authority.getAuthority()));
        if (isAdmin) {
            return ADMIN_ROLE;
        }

        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGER_ROLE.equals(authority.getAuthority()));
        if (isManager) {
            return MANAGER_ROLE;
        }

        throw new IllegalArgumentException("계정을 생성할 권한이 없습니다.");
    }

    private String resolveCreatableRole(String requestedRole, String creatorRole) {
        String targetRole = normalizeRole(requestedRole);

        if (targetRole == null) {
            targetRole = USER_ROLE;
        }

        if (ADMIN_ROLE.equals(creatorRole) && (MANAGER_ROLE.equals(targetRole) || USER_ROLE.equals(targetRole))) {
            return targetRole;
        }

        if (MANAGER_ROLE.equals(creatorRole) && USER_ROLE.equals(targetRole)) {
            return USER_ROLE;
        }

        throw new IllegalArgumentException("생성할 수 없는 권한입니다.");
    }

    private String normalizeRole(String role) {
        String normalizedRole = normalizeOptional(role);
        if (normalizedRole == null) {
            return null;
        }

        String upperRole = normalizedRole.toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(upperRole) || ADMIN_ROLE.equals(upperRole)) {
            return ADMIN_ROLE;
        }
        if ("MANAGER".equals(upperRole) || MANAGER_ROLE.equals(upperRole)) {
            return MANAGER_ROLE;
        }
        if ("USER".equals(upperRole) || USER_ROLE.equals(upperRole)) {
            return USER_ROLE;
        }

        throw new IllegalArgumentException("지원하지 않는 권한입니다.");
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
