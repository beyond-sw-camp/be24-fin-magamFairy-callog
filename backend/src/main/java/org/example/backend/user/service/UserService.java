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
import org.example.backend.userInfo.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String GENERAL_MANAGER_ROLE = "ROLE_GENERAL_MANAGER";
    private static final String MANAGER_ROLE = "ROLE_MANAGER";
    private static final String USER_ROLE = "ROLE_USER";
    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final int TEMPORARY_PASSWORD_LENGTH = 10;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ORGANIZATION_TEXT_PATTERN =
            Pattern.compile("^[\\p{L}\\p{N}\\s().,&+\\-_/·]+$");
    private static final Pattern PERSON_NAME_PATTERN =
            Pattern.compile("^[\\p{L}\\s.\\-·]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^01[016789]\\d{7,8}$");

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
        String name = requirePersonName(dto.name());
        String email = normalizeOptionalEmail(dto.email());

        String companyName;
        String department;
        Organization userOrganization = null;

        if (GENERAL_MANAGER_ROLE.equals(creatorRole) || MANAGER_ROLE.equals(creatorRole)) {
            User creator = resolveAuthenticatedUser(authentication);
            companyName = normalizeOptional(creator.getCompanyName()) != null
                    ? normalizeOptional(creator.getCompanyName())
                    : requireCompanyName(dto.companyName());
            department = MANAGER_ROLE.equals(creatorRole) && normalizeOptional(creator.getDepartment()) != null
                    ? normalizeOptional(creator.getDepartment())
                    : requireDepartment(dto.department());
            userOrganization = creator.getOrganization();
            if (userOrganization == null) {
                userOrganization = organizationService.ensureAffiliateOrganization(companyName);
            }
        } else {
            companyName = requireCompanyName(dto.companyName());
            department = requireDepartment(dto.department());
            userOrganization = organizationService.ensureAffiliateOrganization(companyName);
        }

        if (GENERAL_MANAGER_ROLE.equals(targetRole) && userOrganization != null
                && isActiveUser(userOrganization.getGeneralManager())) {
            throw new IllegalArgumentException("이미 해당 조직에 General Manager가 존재합니다.");
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

        if (GENERAL_MANAGER_ROLE.equals(targetRole) && userOrganization != null) {
            userOrganization.setGeneralManager(savedUser);
        }

        return UserDto.CreateUserRes.from(savedUser, temporaryPassword);
    }

    @Transactional
    public UserDto.PartnerSignupRes partnerSignup(UserDto.PartnerSignupReq dto) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        String companyName = requireCompanyName(dto.companyName());
        String department = requireDepartment(dto.department());
        String name = requirePersonName(dto.name());
        String email = requireEmail(dto.email());
        validateOptionalPhone(dto.phone());

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
                .role(GENERAL_MANAGER_ROLE)
                .accountStatus(UserAccountStatus.ACTIVE)
                .organization(org)
                .build();

        User savedUser = userRepository.save(user);
        userProfileService.ensureProfile(savedUser);
        org.setGeneralManager(savedUser);

        return UserDto.PartnerSignupRes.builder()
                .id(savedUser.getId())
                .companyName(companyName)
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .password(temporaryPassword)
                .build();
    }

    @Transactional
    public UserDto.ManageRoleRes manageUserRole(UserDto.ManageRoleReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        User actor = resolveAuthenticatedUser(authentication);
        String creatorRole = resolveCreatorRole(authentication);
        if (!GENERAL_MANAGER_ROLE.equals(creatorRole)) {
            throw new IllegalArgumentException("GENERAL_MANAGER만 권한을 변경할 수 있습니다.");
        }

        String id = requireText(dto.id(), "id");
        String nextRole = normalizeRole(requireText(dto.role(), "role"));
        if (!MANAGER_ROLE.equals(nextRole) && !USER_ROLE.equals(nextRole)) {
            throw new IllegalArgumentException("MANAGER 또는 USER로만 변경할 수 있습니다.");
        }

        User target = findUserByIdOrEmail(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        String previousRole = normalizeRole(target.getRole());

        validateManageTarget(actor, creatorRole, target);

        if (previousRole.equals(nextRole)) {
            throw new IllegalArgumentException("이미 해당 권한입니다.");
        }

        target.setRole(nextRole);

        return UserDto.ManageRoleRes.builder()
                .id(target.getId())
                .name(target.getName())
                .previousRole(previousRole)
                .role(target.getRole())
                .companyName(target.getCompanyName())
                .department(target.getDepartment())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserDto.ManageableUserRes> listManageableUsers(Authentication authentication) {
        User actor = resolveAuthenticatedUser(authentication);
        String creatorRole = resolveCreatorRole(authentication);

        return findManageableUserCandidates(actor, creatorRole).stream()
                .filter(user -> user.getIdx() == null || !user.getIdx().equals(actor.getIdx()))
                .filter(user -> Boolean.TRUE.equals(user.getEnable()))
                .filter(user -> UserAccountStatus.ACTIVE.equals(user.getAccountStatus()))
                .filter(user -> isManageableListTarget(actor, creatorRole, user))
                .sorted(Comparator
                        .comparing((User user) -> sortableText(user.getDepartment()))
                        .thenComparing(user -> sortableText(user.getName()))
                        .thenComparing(user -> sortableText(user.getId())))
                .map(user -> UserDto.ManageableUserRes.from(user, userProfileService.getProfileImageUrl(user)))
                .toList();
    }

    /** 같은 회사(companyName) 의 active 동료. 본인 제외. role 무관 — 캠페인 팀원 후보용. */
    @Transactional(readOnly = true)
    public List<UserDto.ManageableUserRes> listColleagues(Authentication authentication) {
        User actor = resolveAuthenticatedUser(authentication);
        String companyName = actor.getCompanyName();
        if (companyName == null || companyName.isBlank()) {
            return List.of();
        }
        return userRepository.findAllByCompanyNameAndAccountStatus(companyName, UserAccountStatus.ACTIVE).stream()
                .filter(user -> user.getIdx() != null && !user.getIdx().equals(actor.getIdx()))
                .filter(user -> Boolean.TRUE.equals(user.getEnable()))
                .sorted(Comparator
                        .comparing((User user) -> sortableText(user.getDepartment()))
                        .thenComparing(user -> sortableText(user.getName()))
                        .thenComparing(user -> sortableText(user.getId())))
                .map(user -> UserDto.ManageableUserRes.from(user, userProfileService.getProfileImageUrl(user)))
                .toList();
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
    public UserDto.ChangePasswordRes changePassword(UserDto.ChangePasswordReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        User user = resolveAuthenticatedUser(authentication);
        String currentPassword = requireText(dto.currentPassword(), "currentPassword");
        String newPassword = requireText(dto.newPassword(), "newPassword");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("current password does not match.");
        }

        validateNewPassword(currentPassword, newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        refreshTokenRepository.deleteByUserId(user.getId());

        return UserDto.ChangePasswordRes.builder()
                .id(user.getId())
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
        Organization organization = target.getOrganization();
        if (organization != null
                && organization.getGeneralManager() != null
                && target.getIdx() != null
                && target.getIdx().equals(organization.getGeneralManager().getIdx())) {
            organization.setGeneralManager(null);
        }
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

        boolean isGeneralManager = authentication.getAuthorities().stream()
                .anyMatch(authority -> GENERAL_MANAGER_ROLE.equals(authority.getAuthority()));
        if (isGeneralManager) {
            return GENERAL_MANAGER_ROLE;
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
            if (GENERAL_MANAGER_ROLE.equals(targetRole) || MANAGER_ROLE.equals(targetRole) || USER_ROLE.equals(targetRole)) {
                return;
            }

            throw new IllegalArgumentException("해당 계정을 관리할 권한이 없습니다.");
        }

        if (GENERAL_MANAGER_ROLE.equals(creatorRole)) {
            if (!MANAGER_ROLE.equals(targetRole) && !USER_ROLE.equals(targetRole)) {
                throw new IllegalArgumentException("GENERAL_MANAGER는 MANAGER/USER 계정만 관리할 수 있습니다.");
            }

            if (actor.getOrganization() == null || target.getOrganization() == null
                    || !actor.getOrganization().getIdx().equals(target.getOrganization().getIdx())) {
                throw new IllegalArgumentException("같은 조직의 사용자만 관리할 수 있습니다.");
            }

            return;
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

        if (ADMIN_ROLE.equals(creatorRole)) {
            if (MANAGER_ROLE.equals(targetRole)) {
                return GENERAL_MANAGER_ROLE;
            }

            if (GENERAL_MANAGER_ROLE.equals(targetRole) || USER_ROLE.equals(targetRole)) {
                return targetRole;
            }
        }

        if (GENERAL_MANAGER_ROLE.equals(creatorRole)
                && (MANAGER_ROLE.equals(targetRole) || USER_ROLE.equals(targetRole))) {
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
        if ("GENERAL_MANAGER".equals(upperRole)
                || "GENERALMANAGER".equals(upperRole)
                || GENERAL_MANAGER_ROLE.equals(upperRole)
                || "ROLE_GENERALMANAGER".equals(upperRole)) {
            return GENERAL_MANAGER_ROLE;
        }
        if ("MANAGER".equals(upperRole) || MANAGER_ROLE.equals(upperRole)) {
            return MANAGER_ROLE;
        }
        if ("USER".equals(upperRole) || USER_ROLE.equals(upperRole)) {
            return USER_ROLE;
        }

        throw new IllegalArgumentException("지원하지 않는 권한입니다.");
    }

    private List<User> findManageableUserCandidates(User actor, String creatorRole) {
        if (ADMIN_ROLE.equals(creatorRole)) {
            return userRepository.findAll();
        }

        if (GENERAL_MANAGER_ROLE.equals(creatorRole)) {
            Organization organization = actor.getOrganization();
            if (organization == null || organization.getIdx() == null) {
                throw new IllegalArgumentException("소속 조직 정보가 없어 구성원 목록을 조회할 수 없습니다.");
            }

            return userRepository.findAllByOrganizationIdx(organization.getIdx());
        }

        if (MANAGER_ROLE.equals(creatorRole)) {
            return userRepository.findAllByCompanyName(actor.getCompanyName());
        }

        throw new IllegalArgumentException("구성원 목록을 조회할 권한이 없습니다.");
    }

    private boolean isManageableListTarget(User actor, String creatorRole, User user) {
        try {
            String role = normalizeRole(user.getRole());
            if (ADMIN_ROLE.equals(creatorRole)) {
                return GENERAL_MANAGER_ROLE.equals(role) || MANAGER_ROLE.equals(role) || USER_ROLE.equals(role);
            }

            if (GENERAL_MANAGER_ROLE.equals(creatorRole)) {
                return MANAGER_ROLE.equals(role) || USER_ROLE.equals(role);
            }

            if (MANAGER_ROLE.equals(creatorRole)) {
                return USER_ROLE.equals(role)
                        && sameText(actor.getCompanyName(), user.getCompanyName())
                        && sameText(actor.getDepartment(), user.getDepartment());
            }

            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String sortableText(String value) {
        return Optional.ofNullable(value).orElse("");
    }

    private String normalizeIdentifier(String value) {
        return requireText(value, "identifier").replaceAll("\\s+", "");
    }

    private void validateNewPassword(String currentPassword, String newPassword) {
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            throw new IllegalArgumentException("newPassword must be 8 to 20 characters.");
        }

        if (newPassword.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("newPassword must not contain whitespace.");
        }

        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("newPassword must differ from currentPassword.");
        }

        boolean hasUpper = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = newPassword.chars()
                .anyMatch(character -> !Character.isLetterOrDigit(character));

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException(
                    "newPassword must include uppercase, lowercase, digit, and special character."
            );
        }
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

    private String requireCompanyName(String value) {
        return requireStructuredText(value, "회사명", 2, 60, ORGANIZATION_TEXT_PATTERN, true);
    }

    private String requireDepartment(String value) {
        return requireStructuredText(value, "부서명", 2, 40, ORGANIZATION_TEXT_PATTERN, true);
    }

    private String requirePersonName(String value) {
        String name = requireStructuredText(value, "이름", 2, 30, PERSON_NAME_PATTERN, false);
        if (!name.chars().anyMatch(Character::isLetter)) {
            throw new IllegalArgumentException("이름에는 한글 또는 영문을 포함해 주세요.");
        }
        return name;
    }

    private String requireStructuredText(
            String value,
            String fieldName,
            int minLength,
            int maxLength,
            Pattern allowedPattern,
            boolean allowDigitOnly
    ) {
        String normalized = requireText(value, fieldName);
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "은(는) " + minLength + "자 이상 " + maxLength + "자 이하로 입력해 주세요.");
        }

        boolean hasLetter = normalized.chars().anyMatch(Character::isLetter);
        boolean hasDigit = normalized.chars().anyMatch(Character::isDigit);
        if (!hasLetter && (!allowDigitOnly || !hasDigit)) {
            throw new IllegalArgumentException(fieldName + "에는 한글, 영문 또는 숫자를 포함해 주세요.");
        }

        if (!allowedPattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + "에 사용할 수 없는 문자가 포함되어 있습니다.");
        }

        return normalized;
    }

    private String requireEmail(String value) {
        String email = requireText(value, "이메일").toLowerCase(Locale.ROOT);
        validateEmail(email);
        return email;
    }

    private String normalizeOptionalEmail(String value) {
        String email = normalizeOptional(value);
        if (email == null) {
            return null;
        }

        email = email.toLowerCase(Locale.ROOT);
        validateEmail(email);
        return email;
    }

    private void validateEmail(String email) {
        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식으로 입력해 주세요.");
        }
    }

    private void validateOptionalPhone(String value) {
        String phone = normalizeOptional(value);
        if (phone == null) {
            return;
        }

        String digits = phone.replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(digits).matches()) {
            throw new IllegalArgumentException("연락처는 010-1234-5678 형식의 휴대폰 번호로 입력해 주세요.");
        }
    }

    private boolean isActiveUser(User user) {
        return user != null
                && Boolean.TRUE.equals(user.getEnable())
                && UserAccountStatus.ACTIVE.equals(user.getAccountStatus());
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
