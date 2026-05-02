package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberDto;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.common.security.Roles;
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
    private final UserRepository userRepository;

    // ========== 조회 ==========

    public CampaignMemberDto.ListRes listMembers(Long campaignId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember meMember = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireMember(meMember);

        List<CampaignMember> all = memberRepository.findAllByCampaignIdx(campaignId);
        List<CampaignMember> visible = filterVisibleMembers(all, meMember, caller);
        List<CampaignMemberDto.Res> resList = visible.stream()
                .map(CampaignMemberDto.Res::from)
                .toList();

        boolean isPm = isPmOrganization(campaignId, caller);

        return CampaignMemberDto.ListRes.builder()
                .members(resList)
                .me(CampaignMemberDto.Res.from(meMember))
                .organizationIsPm(isPm)
                .build();
    }

    private List<CampaignMember> filterVisibleMembers(
            List<CampaignMember> all, CampaignMember me, User caller) {
        switch (me.getCampaignRole()) {
            case GENERAL_MANAGER:
                return all;
            case MANAGER:
                return all.stream().filter(m -> {
                    CampaignMemberRole r = m.getCampaignRole();
                    if (r == CampaignMemberRole.GENERAL_MANAGER || r == CampaignMemberRole.MANAGER) {
                        return true;
                    }
                    return sameCompanyAndDepartment(caller, m.getUser());
                }).toList();
            case USER:
            default:
                return all.stream().filter(m ->
                        sameCompanyAndDepartment(caller, m.getUser())
                ).toList();
        }
    }

    private boolean sameCompanyAndDepartment(User a, User b) {
        String aCompany = normalize(a.getCompanyName());
        String aDept = normalize(a.getDepartment());
        String bCompany = normalize(b.getCompanyName());
        String bDept = normalize(b.getDepartment());
        return !aCompany.isEmpty() && !aDept.isEmpty()
                && aCompany.equals(bCompany) && aDept.equals(bDept);
    }

    private boolean isPmOrganization(Long campaignId, User caller) {
        Organization org = caller.getOrganization();
        if (org == null) return false;
        return participantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaignId, org.getIdx(), CampaignRole.PM);
    }

    // ========== 팀원 추가 + 후보 ==========

    @Transactional
    public List<CampaignMemberDto.Res> addTeamMembers(
            Long campaignId, String callerLoginId, List<Long> userIdxList) {
        if (userIdxList == null || userIdxList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userIdxList is required.");
        }
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireCampaignManager(me);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));

        List<CampaignMember> created = new ArrayList<>();
        for (Long uidx : userIdxList) {
            User target = userRepository.findById(uidx)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found: " + uidx));

            if (Roles.ADMIN.equals(target.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ADMIN 사용자는 추가할 수 없습니다.");
            }
            validateAddCandidate(me, caller, target);

            if (memberRepository.existsByCampaignIdxAndUserIdx(campaignId, target.getIdx())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 참여 중인 사용자입니다: " + target.getId());
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
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "이미 참여 중인 사용자입니다: " + target.getId(), e);
            }
        }
        return created.stream().map(CampaignMemberDto.Res::from).toList();
    }

    private void validateAddCandidate(CampaignMember me, User caller, User target) {
        if (me.getCampaignRole() == CampaignMemberRole.MANAGER) {
            if (!sameCompanyAndDepartment(caller, target)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "MANAGER는 자기 회사·부서의 사용자만 추가할 수 있습니다.");
            }
            String role = target.getRole();
            if (!Roles.USER.equals(role) && !Roles.MANAGER.equals(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "USER 또는 MANAGER 사용자만 추가할 수 있습니다.");
            }
        } else if (me.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER) {
            Organization callerOrg = caller.getOrganization();
            Organization targetOrg = target.getOrganization();
            if (callerOrg == null || targetOrg == null
                    || !Objects.equals(callerOrg.getIdx(), targetOrg.getIdx())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "같은 조직의 사용자만 추가할 수 있습니다.");
            }
            String role = target.getRole();
            if (!Roles.USER.equals(role) && !Roles.MANAGER.equals(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "USER 또는 MANAGER 사용자만 추가할 수 있습니다.");
            }
        }
    }

    private static CampaignMemberRole globalRoleToCampaignRole(String globalRole) {
        if (Roles.GENERAL_MANAGER.equals(globalRole)) return CampaignMemberRole.GENERAL_MANAGER;
        if (Roles.MANAGER.equals(globalRole)) return CampaignMemberRole.MANAGER;
        return CampaignMemberRole.USER;
    }

    public List<CampaignMemberDto.CandidateRes> listTeamCandidates(Long campaignId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireCampaignManager(me);

        List<User> pool = me.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER
                ? userRepository.findAllByOrganizationIdx(caller.getOrganization() != null
                    ? caller.getOrganization().getIdx() : -1L)
                : userRepository.findAllByCompanyNameAndDepartment(
                    normalize(caller.getCompanyName()), normalize(caller.getDepartment()));

        Set<Long> existingUserIdx = memberRepository.findAllByCampaignIdx(campaignId)
                .stream()
                .map(m -> m.getUser().getIdx())
                .collect(Collectors.toSet());

        return pool.stream()
                .filter(u -> !Roles.ADMIN.equals(u.getRole()))
                .filter(u -> !Roles.GENERAL_MANAGER.equals(u.getRole()))
                .filter(u -> !existingUserIdx.contains(u.getIdx()))
                .map(this::toCandidate)
                .toList();
    }

    private CampaignMemberDto.CandidateRes toCandidate(User u) {
        Organization org = u.getOrganization();
        return CampaignMemberDto.CandidateRes.builder()
                .userIdx(u.getIdx())
                .userId(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .companyName(u.getCompanyName())
                .department(u.getDepartment())
                .globalRole(u.getRole())
                .organizationIdx(org != null ? org.getIdx() : null)
                .organizationName(org != null ? org.getName() : null)
                .build();
    }

    // ========== 협력사 GM 초대 + 후보 ==========

    @Transactional
    public CampaignMemberDto.Res invitePartnerGm(Long campaignId, String callerLoginId, Long targetUserIdx) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireCampaignManager(me);

        if (!isPmOrganization(campaignId, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PM사 소속만 협력사를 초대할 수 있습니다.");
        }

        User target = userRepository.findById(targetUserIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found."));

        if (!Roles.GENERAL_MANAGER.equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "협력사 초대 대상은 GM만 가능합니다.");
        }

        Organization callerOrg = caller.getOrganization();
        Organization targetOrg = target.getOrganization();
        if (callerOrg == null || targetOrg == null
                || Objects.equals(callerOrg.getIdx(), targetOrg.getIdx())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 조직의 GM이어야 합니다.");
        }

        if (memberRepository.existsByCampaignIdxAndUserIdx(campaignId, target.getIdx())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 참여 중인 사용자입니다.");
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));

        if (!participantRepository.existsByCampaignIdxAndOrganizationIdx(campaignId, targetOrg.getIdx())) {
            CampaignParticipant cp = CampaignParticipant.builder()
                    .campaign(campaign)
                    .organization(targetOrg)
                    .campaignRole(CampaignRole.PARTNER)
                    .build();
            participantRepository.save(cp);
        }

        CampaignMember newMember = CampaignMember.builder()
                .campaign(campaign)
                .user(target)
                .campaignRole(CampaignMemberRole.GENERAL_MANAGER)
                .joinedAt(LocalDateTime.now())
                .build();
        return CampaignMemberDto.Res.from(memberRepository.save(newMember));
    }

    public List<CampaignMemberDto.CandidateRes> listPartnerGmCandidates(Long campaignId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireCampaignManager(me);

        if (!isPmOrganization(campaignId, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PM사 소속만 협력사 후보를 조회할 수 있습니다.");
        }

        Long callerOrgIdx = caller.getOrganization() != null ? caller.getOrganization().getIdx() : -1L;

        Set<Long> existingUserIdx = memberRepository.findAllByCampaignIdx(campaignId)
                .stream()
                .map(m -> m.getUser().getIdx())
                .collect(Collectors.toSet());

        return userRepository.findAllByRole(Roles.GENERAL_MANAGER).stream()
                .filter(u -> u.getOrganization() != null
                        && !u.getOrganization().getIdx().equals(callerOrgIdx))
                .filter(u -> !existingUserIdx.contains(u.getIdx()))
                .map(this::toCandidate)
                .toList();
    }

    // ========== 승격/강등 + 추방 ==========

    @Transactional
    public CampaignMemberDto.Res updateMemberRole(
            Long campaignId, String callerLoginId, Long memberId, CampaignMemberRole nextRole) {
        if (nextRole != CampaignMemberRole.USER && nextRole != CampaignMemberRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "campaignRole은 USER 또는 MANAGER만 가능합니다.");
        }
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireCampaignGM(me);

        CampaignMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));

        if (!Objects.equals(target.getCampaign().getIdx(), campaignId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member does not belong to this campaign.");
        }

        if (target.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GM은 변경 대상이 아닙니다.");
        }

        CampaignMemberGuard.requireSameDepartment(caller, target.getUser());

        target.setCampaignRole(nextRole);
        return CampaignMemberDto.Res.from(target);
    }

    @Transactional
    public void removeMember(Long campaignId, String callerLoginId, Long memberId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireCampaignManager(me);

        CampaignMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));

        if (!Objects.equals(target.getCampaign().getIdx(), campaignId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member does not belong to this campaign.");
        }

        if (Objects.equals(target.getUser().getIdx(), caller.getIdx())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신은 추방할 수 없습니다.");
        }

        if (me.getCampaignRole() == CampaignMemberRole.MANAGER) {
            if (target.getCampaignRole() != CampaignMemberRole.USER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MANAGER는 USER만 추방할 수 있습니다.");
            }
            CampaignMemberGuard.requireSameDepartment(caller, target.getUser());
        }

        memberRepository.delete(target);
    }

    // ========== Helper ==========

    private User findUser(String loginId) {
        return userRepository.findUserById(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
