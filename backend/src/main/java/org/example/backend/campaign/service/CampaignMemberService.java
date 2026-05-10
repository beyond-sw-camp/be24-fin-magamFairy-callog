package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignInvitation;
import org.example.backend.campaign.model.CampaignInvitationStatus;
import org.example.backend.campaign.model.CampaignInvitationType;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberDto;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignInvitationRepository;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.common.security.Roles;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignMemberService {

    private final CampaignMemberRepository memberRepository;
    private final CampaignParticipantRepository participantRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<CampaignMemberDto.ParticipantRes> listParticipants(Long campaignId) {
        return participantRepository.findAllByCampaignIdx(campaignId).stream()
                .map(CampaignMemberDto.ParticipantRes::from)
                .toList();
    }

    public CampaignMemberDto.ListRes listMembers(Long campaignId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember meMember = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireMember(meMember);

        List<CampaignMember> all = memberRepository.findAllByCampaignIdx(campaignId);
        List<CampaignMemberDto.Res> resList = filterVisibleMembers(all, meMember, caller).stream()
                .map(CampaignMemberDto.Res::from)
                .toList();

        Long pmOrganizationIdx = participantRepository
                .findFirstByCampaignIdxAndCampaignRole(campaignId, CampaignRole.PM)
                .map(p -> p.getOrganization() != null ? p.getOrganization().getIdx() : null)
                .orElse(null);

        return CampaignMemberDto.ListRes.builder()
                .members(resList)
                .me(CampaignMemberDto.Res.from(meMember))
                .organizationIsPm(isPmOrganization(campaignId, caller))
                .pmOrganizationIdx(pmOrganizationIdx)
                .build();
    }

    @Transactional
    public List<CampaignMemberDto.Res> addTeamMembers(
            Long campaignId,
            String callerLoginId,
            List<Long> userIdxList
    ) {
        if (userIdxList == null || userIdxList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userIdxList is required.");
        }

        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);
        Campaign campaign = findCampaign(campaignId);

        List<CampaignMember> created = new ArrayList<>();
        for (Long userIdx : userIdxList) {
            User target = userRepository.findById(userIdx)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "target user not found."));

            if (Roles.ADMIN.equals(target.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ADMIN 사용자는 추가할 수 없습니다.");
            }
            validateAddCandidate(me, caller, target);

            if (memberRepository.existsByCampaignIdxAndUserIdx(campaignId, target.getIdx())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 참여 중인 사용자입니다.");
            }

            CampaignMember member = CampaignMember.builder()
                    .campaign(campaign)
                    .user(target)
                    .campaignRole(globalRoleToCampaignRole(target.getRole()))
                    .joinedAt(LocalDateTime.now())
                    .build();
            try {
                created.add(memberRepository.saveAndFlush(member));
            } catch (DataIntegrityViolationException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 참여 중인 사용자입니다.", e);
            }
        }

        created.forEach(member -> notificationService.notifyCampaignMemberAdded(
                member.getUser(),
                caller,
                campaign.getName(),
                "/campaigns/" + campaign.getPublicId()
        ));

        return created.stream().map(CampaignMemberDto.Res::from).toList();
    }

    public List<CampaignMemberDto.CandidateRes> listTeamCandidates(Long campaignId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);

        List<User> pool = me.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER
                ? userRepository.findAllByOrganizationIdx(caller.getOrganization() != null
                        ? caller.getOrganization().getIdx()
                        : -1L)
                : userRepository.findAllByCompanyName(normalize(caller.getCompanyName()));
        Set<Long> existingUserIdx = memberRepository.findAllByCampaignIdx(campaignId).stream()
                .map(member -> member.getUser().getIdx())
                .collect(Collectors.toSet());

        return pool.stream()
                .filter(user -> !Roles.ADMIN.equals(user.getRole()))
                .filter(user -> !Roles.GENERAL_MANAGER.equals(user.getRole()))
                .filter(user -> !existingUserIdx.contains(user.getIdx()))
                .map(this::toCandidate)
                .toList();
    }

    @Transactional
    public CampaignMemberDto.InvitationRes invitePartnerGm(Long campaignId, String callerLoginId, Long targetUserIdx) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);

        if (!isPmOrganization(campaignId, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PM 조직만 협력사 GM을 초대할 수 있습니다.");
        }

        User target = userRepository.findById(targetUserIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "target user not found."));

        if (!Roles.GENERAL_MANAGER.equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "협력사 초대 대상은 GM만 가능합니다.");
        }

        Organization callerOrg = caller.getOrganization();
        Organization targetOrg = target.getOrganization();
        if (callerOrg == null || targetOrg == null || Objects.equals(callerOrg.getIdx(), targetOrg.getIdx())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 조직의 GM이어야 합니다.");
        }

        if (memberRepository.existsByCampaignIdxAndUserIdx(campaignId, target.getIdx())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 참여 중인 사용자입니다.");
        }

        if (invitationRepository.existsByCampaign_IdxAndInvitee_IdxAndStatus(
                campaignId,
                target.getIdx(),
                CampaignInvitationStatus.PENDING
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "대기 중인 초대가 이미 있습니다.");
        }

        Campaign campaign = findCampaign(campaignId);
        CampaignInvitation invitation = CampaignInvitation.builder()
                .campaign(campaign)
                .inviter(caller)
                .invitee(target)
                .inviteeOrganization(targetOrg)
                .status(CampaignInvitationStatus.PENDING)
                .build();
        CampaignInvitation saved = invitationRepository.save(invitation);
        notificationService.notifyCampaignInvitation(saved);

        return CampaignMemberDto.InvitationRes.from(saved);
    }

    public List<CampaignMemberDto.CandidateRes> listPartnerGmCandidates(Long campaignId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);

        if (!isPmOrganization(campaignId, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PM 조직만 협력사 후보를 조회할 수 있습니다.");
        }

        Long callerOrgIdx = caller.getOrganization() != null ? caller.getOrganization().getIdx() : -1L;
        Set<Long> existingUserIdx = memberRepository.findAllByCampaignIdx(campaignId).stream()
                .map(member -> member.getUser().getIdx())
                .collect(Collectors.toSet());

        return userRepository.findAllByRole(Roles.GENERAL_MANAGER).stream()
                .filter(user -> user.getOrganization() != null && !user.getOrganization().getIdx().equals(callerOrgIdx))
                .filter(user -> !existingUserIdx.contains(user.getIdx()))
                .map(this::toCandidate)
                .toList();
    }

    public List<CampaignMemberDto.PartnerOrganizationCandidateRes> listPartnerOrganizationCandidates(
            Long campaignId, String callerLoginId
    ) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);

        if (!isPmOrganization(campaignId, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PM 조직만 협력사 후보를 조회할 수 있습니다.");
        }

        Long callerOrgIdx = caller.getOrganization() != null ? caller.getOrganization().getIdx() : -1L;

        // 모든 GM 후보를 조직별로 묶음
        java.util.Map<Long, List<User>> gmsByOrg = userRepository.findAllByRole(Roles.GENERAL_MANAGER).stream()
                .filter(u -> u.getOrganization() != null && !u.getOrganization().getIdx().equals(callerOrgIdx))
                .filter(u -> u.getAccountStatus() == org.example.backend.user.model.UserAccountStatus.ACTIVE)
                .collect(Collectors.groupingBy(u -> u.getOrganization().getIdx()));

        Set<Long> existingUserIdx = memberRepository.findAllByCampaignIdx(campaignId).stream()
                .map(member -> member.getUser().getIdx())
                .collect(Collectors.toSet());

        return gmsByOrg.entrySet().stream()
                .map(entry -> {
                    Long orgIdx = entry.getKey();
                    List<User> gms = entry.getValue();
                    User repGm = gms.stream()
                            .min(java.util.Comparator.comparing(User::getIdx))
                            .orElse(null);
                    if (repGm == null) return null;

                    List<User> activeOrgUsers = userRepository.findAllByOrganization_IdxAndAccountStatus(
                            orgIdx, org.example.backend.user.model.UserAccountStatus.ACTIVE);
                    int totalActive = activeOrgUsers.size();
                    int eligible = (int) activeOrgUsers.stream()
                            .filter(u -> !existingUserIdx.contains(u.getIdx()))
                            .count();

                    if (eligible == 0) return null;

                    Organization org = repGm.getOrganization();
                    return CampaignMemberDto.PartnerOrganizationCandidateRes.builder()
                            .organizationIdx(org.getIdx())
                            .organizationName(org.getName())
                            .totalActiveCount(totalActive)
                            .eligibleCount(eligible)
                            .representativeGm(CampaignMemberDto.PartnerOrganizationCandidateRes.RepresentativeGm.builder()
                                    .userIdx(repGm.getIdx())
                                    .name(repGm.getName())
                                    .email(repGm.getEmail())
                                    .build())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(CampaignMemberDto.PartnerOrganizationCandidateRes::organizationName))
                .toList();
    }

    @Transactional
    public CampaignMemberDto.InvitationRes invitePartnerGroup(
            Long campaignId, String callerLoginId, Long organizationIdx
    ) {
        if (organizationIdx == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizationIdx is required.");
        }

        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);

        if (!isPmOrganization(campaignId, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PM 조직만 협력사 그룹 초대를 보낼 수 있습니다.");
        }

        Organization callerOrg = caller.getOrganization();
        if (callerOrg != null && Objects.equals(callerOrg.getIdx(), organizationIdx)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 조직만 초대할 수 있습니다.");
        }

        if (invitationRepository.existsByCampaign_IdxAndInviteeOrganization_IdxAndStatus(
                campaignId, organizationIdx, CampaignInvitationStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 발송된 초대가 있습니다.");
        }

        // 대상 조직 활성 사용자
        List<User> activeUsers = userRepository.findAllByOrganization_IdxAndAccountStatus(
                organizationIdx, org.example.backend.user.model.UserAccountStatus.ACTIVE);
        if (activeUsers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대 가능한 GM이 없습니다.");
        }

        // 대표 GM = 활성 GM 중 idx 가장 작은 사용자
        User repGm = activeUsers.stream()
                .filter(u -> Roles.GENERAL_MANAGER.equals(u.getRole()))
                .min(java.util.Comparator.comparing(User::getIdx))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대 가능한 GM이 없습니다."));

        // 합류 가능 인원 검증
        Set<Long> existingUserIdx = memberRepository.findAllByCampaignIdx(campaignId).stream()
                .map(m -> m.getUser().getIdx())
                .collect(Collectors.toSet());
        long eligibleCount = activeUsers.stream()
                .filter(u -> !existingUserIdx.contains(u.getIdx()))
                .count();
        if (eligibleCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 전원 참여 중입니다.");
        }

        Organization targetOrg = repGm.getOrganization();
        Campaign campaign = findCampaign(campaignId);
        CampaignInvitation invitation = CampaignInvitation.builder()
                .campaign(campaign)
                .inviter(caller)
                .invitee(repGm)
                .inviteeOrganization(targetOrg)
                .status(CampaignInvitationStatus.PENDING)
                .type(CampaignInvitationType.GROUP)
                .build();
        CampaignInvitation saved;
        try {
            saved = invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 발송된 초대가 있습니다.", e);
        }

        notificationService.notifyCampaignGroupInvitation(saved, (int) eligibleCount);
        return CampaignMemberDto.InvitationRes.from(saved);
    }

    @Transactional
    public CampaignMemberDto.Res updateMemberRole(
            Long campaignId,
            String callerLoginId,
            Long memberId,
            CampaignMemberRole nextRole
    ) {
        if (nextRole != CampaignMemberRole.USER && nextRole != CampaignMemberRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "campaignRole은 USER 또는 MANAGER만 가능합니다.");
        }

        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);
        CampaignMember target = findMemberInCampaign(campaignId, memberId);

        if (target.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GM은 변경 대상이 아닙니다.");
        }

        CampaignMemberGuard.requireSameCompany(caller, target.getUser());
        target.setCampaignRole(nextRole);

        return CampaignMemberDto.Res.from(target);
    }

    @Transactional
    public void removeMember(Long campaignId, String callerLoginId, Long memberId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository.findByCampaignIdxAndUserIdx(campaignId, caller.getIdx()).orElse(null);
        CampaignMemberGuard.requireMember(me);
        CampaignMember target = findMemberInCampaign(campaignId, memberId);

        if (Objects.equals(target.getUser().getIdx(), caller.getIdx())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신은 제외할 수 없습니다.");
        }

        if (me.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER) {
            // PM 조직 GM은 모든 멤버 추방 가능 (기존 정책)
            if (!isPmOrganization(campaignId, caller)) {
                // 그 외 GM은 같은 조직 멤버만
                Organization callerOrg = caller.getOrganization();
                User targetUser = target.getUser();
                Organization targetOrg = targetUser.getOrganization();
                if (callerOrg == null || targetOrg == null
                        || !Objects.equals(callerOrg.getIdx(), targetOrg.getIdx())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "같은 조직 사용자만 추방할 수 있습니다.");
                }
            }
        } else if (me.getCampaignRole() == CampaignMemberRole.MANAGER) {
            if (target.getCampaignRole() != CampaignMemberRole.USER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MANAGER는 USER만 제외할 수 있습니다.");
            }
            CampaignMemberGuard.requireSameCompany(caller, target.getUser());
        }

        memberRepository.delete(target);
    }

    @Transactional
    public CampaignMemberDto.InvitationRes acceptInvitation(Long campaignId, Long invitationId, String callerLoginId) {
        CampaignInvitation invitation = findInvitation(campaignId, invitationId);
        User caller = findUser(callerLoginId);
        requireInvitee(invitation, caller);
        ensurePending(invitation);

        Campaign campaign = invitation.getCampaign();
        Organization inviteeOrganization = invitation.getInviteeOrganization();

        if (inviteeOrganization != null
                && !participantRepository.existsByCampaignIdxAndOrganizationIdx(campaignId, inviteeOrganization.getIdx())) {
            participantRepository.save(CampaignParticipant.builder()
                    .campaign(campaign)
                    .organization(inviteeOrganization)
                    .campaignRole(CampaignRole.PARTNER)
                    .build());
        }

        int joinedCount;
        if (invitation.getType() == CampaignInvitationType.GROUP) {
            joinedCount = joinGroupMembers(campaign, invitation);
        } else {
            joinedCount = joinIndividualMember(campaign, invitation.getInvitee()) ? 1 : 0;
        }

        invitation.accept();
        notificationService.notifyCampaignInvitationDecision(invitation);

        return CampaignMemberDto.InvitationRes.from(invitation, joinedCount, null);
    }

    private boolean joinIndividualMember(Campaign campaign, User invitee) {
        if (memberRepository.existsByCampaignIdxAndUserIdx(campaign.getIdx(), invitee.getIdx())) {
            return false;
        }
        memberRepository.save(CampaignMember.builder()
                .campaign(campaign)
                .user(invitee)
                .campaignRole(CampaignMemberRole.GENERAL_MANAGER)
                .joinedAt(LocalDateTime.now())
                .build());
        return true;
    }

    private int joinGroupMembers(Campaign campaign, CampaignInvitation invitation) {
        Organization inviteeOrg = invitation.getInviteeOrganization();
        if (inviteeOrg == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 조직 정보가 없습니다.");
        }
        Long repGmIdx = invitation.getInvitee().getIdx();

        List<User> activeUsers = userRepository.findAllByOrganization_IdxAndAccountStatus(
                inviteeOrg.getIdx(), org.example.backend.user.model.UserAccountStatus.ACTIVE);
        Set<Long> existing = memberRepository.findUserIdxByCampaignIdxAndUserIdxIn(
                campaign.getIdx(),
                activeUsers.stream().map(User::getIdx).toList());

        int joined = 0;
        LocalDateTime joinedAt = LocalDateTime.now();
        for (User u : activeUsers) {
            if (existing.contains(u.getIdx())) {
                continue;
            }
            CampaignMemberRole role = Objects.equals(u.getIdx(), repGmIdx)
                    ? CampaignMemberRole.GENERAL_MANAGER
                    : CampaignMemberRole.USER;
            memberRepository.save(CampaignMember.builder()
                    .campaign(campaign)
                    .user(u)
                    .campaignRole(role)
                    .joinedAt(joinedAt)
                    .build());
            joined++;
        }
        return joined;
    }

    @Transactional
    public CampaignMemberDto.InvitationRes rejectInvitation(Long campaignId, Long invitationId, String callerLoginId) {
        CampaignInvitation invitation = findInvitation(campaignId, invitationId);
        User caller = findUser(callerLoginId);
        requireInvitee(invitation, caller);
        ensurePending(invitation);

        invitation.reject();
        notificationService.notifyCampaignInvitationDecision(invitation);

        return CampaignMemberDto.InvitationRes.from(invitation);
    }

    private List<CampaignMember> filterVisibleMembers(List<CampaignMember> all, CampaignMember me, User caller) {
        return switch (me.getCampaignRole()) {
            case GENERAL_MANAGER -> all;
            case MANAGER -> all.stream()
                    .filter(member -> {
                        CampaignMemberRole role = member.getCampaignRole();
                        return role == CampaignMemberRole.GENERAL_MANAGER
                                || role == CampaignMemberRole.MANAGER
                                || sameCompany(caller, member.getUser());
                    })
                    .toList();
            case USER -> all.stream()
                    .filter(member -> sameCompany(caller, member.getUser()))
                    .toList();
        };
    }

    private void validateAddCandidate(CampaignMember me, User caller, User target) {
        if (me.getCampaignRole() == CampaignMemberRole.MANAGER) {
            if (!sameCompany(caller, target)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MANAGER는 같은 회사 사용자만 추가할 수 있습니다.");
            }
            if (!Roles.USER.equals(target.getRole()) && !Roles.MANAGER.equals(target.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER 또는 MANAGER 사용자만 추가할 수 있습니다.");
            }
            return;
        }

        if (me.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER) {
            Organization callerOrg = caller.getOrganization();
            Organization targetOrg = target.getOrganization();
            if (callerOrg == null || targetOrg == null || !Objects.equals(callerOrg.getIdx(), targetOrg.getIdx())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "같은 조직 사용자만 추가할 수 있습니다.");
            }
            if (!Roles.USER.equals(target.getRole()) && !Roles.MANAGER.equals(target.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER 또는 MANAGER 사용자만 추가할 수 있습니다.");
            }
        }
    }

    private boolean sameCompany(User a, User b) {
        String aCompany = normalize(a.getCompanyName());
        String bCompany = normalize(b.getCompanyName());

        return !aCompany.isEmpty() && aCompany.equals(bCompany);
    }

    private boolean isPmOrganization(Long campaignId, User caller) {
        Organization organization = caller.getOrganization();
        if (organization == null) {
            return false;
        }

        return participantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaignId,
                organization.getIdx(),
                CampaignRole.PM
        );
    }

    private static CampaignMemberRole globalRoleToCampaignRole(String globalRole) {
        if (Roles.GENERAL_MANAGER.equals(globalRole)) {
            return CampaignMemberRole.GENERAL_MANAGER;
        }
        if (Roles.MANAGER.equals(globalRole)) {
            return CampaignMemberRole.MANAGER;
        }
        return CampaignMemberRole.USER;
    }

    private CampaignMemberDto.CandidateRes toCandidate(User user) {
        Organization organization = user.getOrganization();

        return CampaignMemberDto.CandidateRes.builder()
                .userIdx(user.getIdx())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .department(user.getDepartment())
                .globalRole(user.getRole())
                .organizationIdx(organization != null ? organization.getIdx() : null)
                .organizationName(organization != null ? organization.getName() : null)
                .build();
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "campaign not found."));
    }

    private CampaignMember findMemberInCampaign(Long campaignId, Long memberId) {
        CampaignMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found."));

        if (!Objects.equals(member.getCampaign().getIdx(), campaignId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "member does not belong to this campaign.");
        }

        return member;
    }

    private CampaignInvitation findInvitation(Long campaignId, Long invitationId) {
        return invitationRepository.findByIdxAndCampaign_Idx(invitationId, campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "invitation not found."));
    }

    private void requireInvitee(CampaignInvitation invitation, User caller) {
        if (!Objects.equals(invitation.getInvitee().getIdx(), caller.getIdx())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only invited user can respond.");
        }
    }

    private void ensurePending(CampaignInvitation invitation) {
        if (invitation.getStatus() != CampaignInvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invitation is already closed.");
        }
    }

    private User findUser(String loginId) {
        return userRepository.findUserById(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found."));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
