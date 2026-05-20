package org.example.backend.user.model;


import lombok.RequiredArgsConstructor;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.service.OrganizationService;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.userInfo.service.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Administrator implements ApplicationRunner {
    private static final String GENERAL_MANAGER_ROLE = "ROLE_GENERAL_MANAGER";
    private static final String HQ_GM_ID = "hqgm@callog.com";
    private static final String HQ_GM_EMAIL = "hqgm@callog.com";
    private static final String HQ_GM_NAME = "HQ General Manager";
    private static final String HQ_GM_DEPARTMENT = "HQ";
    private static final String PARTNER_GM_ID = "partner@callog.com";
    private static final String PARTNER_GM_EMAIL = "partner@callog.com";
    private static final String PARTNER_GM_NAME = "Partner General Manager";
    private static final String PARTNER_GM_COMPANY_NAME = "Callog Partner";
    private static final String PARTNER_GM_DEPARTMENT = "Partnership";
    private static final String TEST_GM_PASSWORD = "Qwer1234!";

    @Value("${admin.id}") private String ADMIN_ID;
    @Value("${admin.email}") private String ADMIN_EMAIL;
    @Value("${admin.name}") private String ADMIN_NAME;
    @Value("${admin.role}") private String ADMIN_ROLE;
    @Value("${admin.password}") private String ADMIN_PASSWORD;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationService organizationService;
    private final UserProfileService userProfileService;

    @Override
    public void run(ApplicationArguments args) {
        Organization headquarters = organizationService.ensureHeadquarters();
        saveSeedUser(
                ADMIN_ID,
                ADMIN_EMAIL,
                ADMIN_NAME,
                ADMIN_ROLE,
                ADMIN_PASSWORD,
                headquarters,
                null,
                null
        );

        User savedHqGeneralManager = saveSeedUser(
                HQ_GM_ID,
                HQ_GM_EMAIL,
                HQ_GM_NAME,
                GENERAL_MANAGER_ROLE,
                TEST_GM_PASSWORD,
                headquarters,
                OrganizationService.HQ_NAME,
                HQ_GM_DEPARTMENT
        );
        organizationService.assignGeneralManager(headquarters, savedHqGeneralManager);

        Organization partnerOrganization = organizationService.ensureExternalPartnerOrganization(PARTNER_GM_COMPANY_NAME);
        User savedPartnerGeneralManager = saveSeedUser(
                PARTNER_GM_ID,
                PARTNER_GM_EMAIL,
                PARTNER_GM_NAME,
                GENERAL_MANAGER_ROLE,
                TEST_GM_PASSWORD,
                partnerOrganization,
                PARTNER_GM_COMPANY_NAME,
                PARTNER_GM_DEPARTMENT
        );
        organizationService.assignGeneralManager(partnerOrganization, savedPartnerGeneralManager);
    }

    private User saveSeedUser(
            String id,
            String email,
            String name,
            String role,
            String password,
            Organization organization,
            String companyName,
            String department
    ) {
        User user = userRepository.findUserById(id)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> User.builder()
                        .id(id)
                        .email(email)
                        .name(name)
                        .enable(true)
                        .role(role)
                        .accountStatus(UserAccountStatus.ACTIVE)
                        .organization(organization)
                        .build());

        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        if (companyName != null) {
            user.setCompanyName(companyName);
        }
        if (department != null) {
            user.setDepartment(department);
        }
        user.setEnable(true);
        user.setRole(role);
        user.setOrganization(organization);
        user.setAccountStatus(UserAccountStatus.ACTIVE);

        if (shouldResetPassword(user.getPassword(), password)) {
            user.setPassword(passwordEncoder.encode(password));
        }

        User savedUser = userRepository.save(user);
        userProfileService.ensureProfile(savedUser);
        return savedUser;
    }

    private boolean shouldResetPassword(String storedPassword, String expectedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return true;
        }

        try {
            return !passwordEncoder.matches(expectedPassword, storedPassword);
        } catch (IllegalArgumentException exception) {
            // Legacy plain-text or unknown encoded passwords should be replaced
            // with the configured seed password on startup.
            return true;
        }
    }
}
