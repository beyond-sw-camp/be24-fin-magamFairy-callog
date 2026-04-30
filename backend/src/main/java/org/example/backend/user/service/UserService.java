package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BaseException;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.service.OrganizationService;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.model.UserDto;
import org.example.backend.user.repository.RefreshTokenRepository;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.userInfo.userProfile.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String MANAGER_ROLE = "ROLE_MANAGER";
    private static final String USER_ROLE = "ROLE_USER";
    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final int TEMPORARY_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserProfileService userProfileService;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public UserDto.CreateUserRes createUser(UserDto.CreateUserReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        String creatorRole = resolveCreatorRole(authentication);
        String targetRole = resolveCreatableRole(dto.role(), creatorRole);
        String name = requireText(dto.name(), "name");
        String email = normalizeOptional(dto.email());

        String companyName;
        String department;
        Organization userOrganization = null;

        if (MANAGER_ROLE.equals(creatorRole)) {
            User manager = resolveAuthenticatedUser(authentication);
            companyName = manager.getCompanyName() != null
                    ? manager.getCompanyName()
                    : requireText(dto.companyName(), "companyName");
            department = manager.getDepartment() != null
                    ? manager.getDepartment()
                    : requireText(dto.department(), "department");
            userOrganization = manager.getOrganization();
        } else {
            companyName = requireText(dto.companyName(), "companyName");
            department = requireText(dto.department(), "department");
        }

        String id = createUniqueId(companyName, department, name);

        if (email != null && userRepository.existsByEmail(email)) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_DUPLICATE_EMAIL);
        }

        String temporaryPassword = generateTemporaryPassword();
        User user = User.builder()
                .id(id)
                .email(email)
                .name(name)
                .companyName(companyName)
                .department(department)
                .password(passwordEncoder.encode(temporaryPassword))
                .enable(true)
                .role(targetRole)
                .accountStatus(UserAccountStatus.ACTIVE)
                .organization(userOrganization)
                .build();

        User savedUser = userRepository.save(user);
        userProfileService.ensureProfile(savedUser);

        return UserDto.CreateUserRes.from(savedUser, temporaryPassword);
    }

    @Transactional
    public UserDto.PartnerSignupRes partnerSignup(UserDto.PartnerSignupReq dto) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        String companyName = requireText(dto.companyName(), "companyName");
        String department = requireText(dto.department(), "department");
        String name = requireText(dto.name(), "name");
        String email = requireText(dto.email(), "email");

        if (userRepository.existsByEmail(email)) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_DUPLICATE_EMAIL);
        }

        Organization org = organizationService.createPartnerOrganization(companyName);
        String id = createUniqueId(companyName, department, name);
        String temporaryPassword = generateTemporaryPassword();

        User user = User.builder()
                .id(id)
                .email(email)
                .name(name)
                .companyName(companyName)
                .department(department)
                .password(passwordEncoder.encode(temporaryPassword))
                .enable(true)
                .role(MANAGER_ROLE)
                .accountStatus(UserAccountStatus.ACTIVE)
                .organization(org)
                .build();

        User savedUser = userRepository.save(user);
        userProfileService.ensureProfile(savedUser);

        return UserDto.PartnerSignupRes.builder()
                .id(savedUser.getId())
                .companyName(companyName)
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .password(temporaryPassword)
                .build();
    }

    @Transactional
    public UserDto.ResetPasswordRes resetPassword(UserDto.ResetPasswordReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        User actor = resolveAuthenticatedUser(authentication);
        String creatorRole = resolveCreatorRole(authentication);
        String id = requireText(dto.id(), "id");
        User target = findUserByIdOrEmail(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        validateManageTarget(actor, creatorRole, target);

        String temporaryPassword = generateTemporaryPassword();
        target.setPassword(passwordEncoder.encode(temporaryPassword));

        return UserDto.ResetPasswordRes.builder()
                .id(target.getId())
                .password(temporaryPassword)
                .build();
    }

    @Transactional
    public UserDto.DeleteUserRes deleteUser(UserDto.DeleteUserReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        User actor = resolveAuthenticatedUser(authentication);
        String creatorRole = resolveCreatorRole(authentication);
        String id = requireText(dto.id(), "id");
        User target = findUserByIdOrEmail(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        validateManageTarget(actor, creatorRole, target);

        target.setEnable(false);
        target.setAccountStatus(UserAccountStatus.INACTIVE);
        refreshTokenRepository.deleteByUserId(target.getId());

        return UserDto.DeleteUserRes.from(target);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = normalizeOptional(username);
        if (normalizedUsername == null) {
            throw new UsernameNotFoundException("user not found");
        }

        User user = findUserByIdOrEmail(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        return AuthUserDetails.from(user);
    }

    private String createUniqueId(String companyName, String department, String name) {
        String baseId = String.join("_", normalizeIdentifier(companyName), normalizeIdentifier(department), normalizeIdentifier(name));
        String candidate = baseId;
        int suffix = 2;

        while (userRepository.existsUserById(candidate)) {
            candidate = baseId + suffix;
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

        throw new IllegalArgumentException("계정을 관리할 권한이 없습니다.");
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("creator authentication is required.");
        }

        return findUserByIdOrEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("creator user not found."));
    }

    private void validateManageTarget(User actor, String creatorRole, User target) {
        if (actor.getIdx() != null && actor.getIdx().equals(target.getIdx())) {
            throw new IllegalArgumentException("자기 자신의 계정은 처리할 수 없습니다.");
        }

        String targetRole = normalizeRole(target.getRole());
        if (ADMIN_ROLE.equals(creatorRole)) {
            if (MANAGER_ROLE.equals(targetRole) || USER_ROLE.equals(targetRole)) {
                return;
            }

            throw new IllegalArgumentException("해당 계정을 관리할 권한이 없습니다.");
        }

        if (MANAGER_ROLE.equals(creatorRole)) {
            if (!USER_ROLE.equals(targetRole)) {
                throw new IllegalArgumentException("MANAGER는 USER 계정만 관리할 수 있습니다.");
            }

            if (!sameText(actor.getCompanyName(), target.getCompanyName())
                    || !sameText(actor.getDepartment(), target.getDepartment())) {
                throw new IllegalArgumentException("같은 회사와 같은 부서의 사용자만 관리할 수 있습니다.");
            }

            return;
        }

        throw new IllegalArgumentException("해당 계정을 관리할 권한이 없습니다.");
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

    private Optional<User> findUserByIdOrEmail(String idOrEmail) {
        return userRepository.findUserById(idOrEmail)
                .or(() -> userRepository.findByEmail(idOrEmail));
    }

    private boolean sameText(String first, String second) {
        String normalizedFirst = normalizeOptional(first);
        String normalizedSecond = normalizeOptional(second);

        return normalizedFirst != null
                && normalizedSecond != null
                && normalizedFirst.equalsIgnoreCase(normalizedSecond);
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
