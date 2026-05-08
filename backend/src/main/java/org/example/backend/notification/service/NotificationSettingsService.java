package org.example.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.security.Roles;
import org.example.backend.notification.model.NotificationAdminPolicy;
import org.example.backend.notification.model.NotificationDto;
import org.example.backend.notification.model.NotificationSetting;
import org.example.backend.notification.repository.NotificationAdminPolicyRepository;
import org.example.backend.notification.repository.NotificationSettingRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.repository.OrganizationRepository;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingsService {
    private final NotificationSettingRepository settingRepository;
    private final NotificationAdminPolicyRepository adminPolicyRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public NotificationDto.SettingRes getSetting(AuthUserDetails authUser) {
        User user = findUser(authUser);
        NotificationSetting setting = settingRepository.findByUser_Idx(user.getIdx())
                .orElseGet(() -> settingRepository.save(NotificationSetting.defaultFor(user)));

        return NotificationDto.SettingRes.from(setting);
    }

    @Transactional
    public NotificationDto.SettingRes updateSetting(AuthUserDetails authUser, NotificationDto.SettingReq request) {
        User user = findUser(authUser);
        NotificationSetting setting = settingRepository.findByUser_Idx(user.getIdx())
                .orElseGet(() -> settingRepository.save(NotificationSetting.defaultFor(user)));

        setting.update(request);
        return NotificationDto.SettingRes.from(setting);
    }

    public NotificationDto.AdminPolicyRes listAdminPolicies(AuthUserDetails authUser) {
        User user = requirePolicyManager(authUser);
        Long organizationIdx = requireOrganization(user).getIdx();

        List<NotificationDto.AdminPolicyItem> policies = adminPolicyRepository
                .findAllByOrganization_IdxOrderByRoleNameAscNotificationTypeAsc(organizationIdx)
                .stream()
                .map(NotificationDto.AdminPolicyItem::from)
                .sorted(Comparator.comparing(NotificationDto.AdminPolicyItem::roleName)
                        .thenComparing(item -> item.notificationType().name()))
                .toList();

        return new NotificationDto.AdminPolicyRes(policies);
    }

    @Transactional
    public NotificationDto.AdminPolicyRes updateAdminPolicies(
            AuthUserDetails authUser,
            NotificationDto.AdminPolicyReq request
    ) {
        User user = requirePolicyManager(authUser);
        Organization defaultOrganization = requireOrganization(user);

        if (request != null && request.policies() != null) {
            request.policies().stream()
                    .filter(item -> item != null && item.notificationType() != null)
                    .forEach(item -> upsertPolicy(user, defaultOrganization, item));
        }

        return listAdminPolicies(authUser);
    }

    private void upsertPolicy(User user, Organization defaultOrganization, NotificationDto.AdminPolicyItem item) {
        Organization organization = item.organizationIdx() == null
                ? defaultOrganization
                : organizationRepository.findById(item.organizationIdx())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "organization not found."));
        if (!Roles.ADMIN.equals(normalizeRoleName(user.getRole()))
                && !organization.getIdx().equals(defaultOrganization.getIdx())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "cannot update other organization policy.");
        }
        String roleName = normalizeRoleName(item.roleName());

        NotificationAdminPolicy policy = adminPolicyRepository
                .findByOrganization_IdxAndRoleNameAndNotificationType(
                        organization.getIdx(),
                        roleName,
                        item.notificationType()
                )
                .orElseGet(() -> adminPolicyRepository.save(NotificationAdminPolicy.builder()
                        .organization(organization)
                        .roleName(roleName)
                        .notificationType(item.notificationType())
                        .enabled(true)
                        .build()));

        policy.updateEnabled(item.enabled());
    }

    private User findUser(AuthUserDetails authUser) {
        if (authUser == null || authUser.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user is required.");
        }

        return userRepository.findById(authUser.getIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found."));
    }

    private User requirePolicyManager(AuthUserDetails authUser) {
        User user = findUser(authUser);
        String role = normalizeRoleName(user.getRole());
        boolean isAdmin = Roles.ADMIN.equals(role);
        boolean isGeneralManager = Roles.GENERAL_MANAGER.equals(role);
        boolean isOrganizationOwner = user.getOrganization() != null
                && user.getOrganization().getGeneralManager() != null
                && user.getOrganization().getGeneralManager().getIdx().equals(user.getIdx());

        if (!isAdmin && !isGeneralManager && !isOrganizationOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "notification policy permission is required.");
        }

        return user;
    }

    private Organization requireOrganization(User user) {
        if (user.getOrganization() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organization is required.");
        }

        return user.getOrganization();
    }

    private String normalizeRoleName(String roleName) {
        return roleName == null || roleName.isBlank()
                ? NotificationAdminPolicy.ROLE_ALL
                : roleName.trim().toUpperCase();
    }
}
