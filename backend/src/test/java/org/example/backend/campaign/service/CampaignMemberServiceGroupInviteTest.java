package org.example.backend.campaign.service;

import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignInvitation;
import org.example.backend.campaign.model.CampaignInvitationStatus;
import org.example.backend.campaign.model.CampaignInvitationType;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignInvitationRepository;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.Roles;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignMemberServiceGroupInviteTest {

    @Mock CampaignMemberRepository memberRepository;
    @Mock CampaignParticipantRepository participantRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock CampaignInvitationRepository invitationRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    @InjectMocks CampaignMemberService service;

    Organization pmOrg;
    Organization partnerOrg;
    User pmGm;
    User partnerGm;
    User partnerUser;
    Campaign campaign;
    CampaignMember pmMember;

    @BeforeEach
    void setUp() {
        pmOrg = Organization.builder().idx(1L).name("PMORG").build();
        partnerOrg = Organization.builder().idx(2L).name("A협력사").build();

        pmGm = User.builder().idx(10L).id("pm").name("PM리더").role(Roles.GENERAL_MANAGER)
                .accountStatus(UserAccountStatus.ACTIVE).organization(pmOrg).build();
        partnerGm = User.builder().idx(20L).id("pgm").name("협력사리더").email("p@p.io")
                .role(Roles.GENERAL_MANAGER).accountStatus(UserAccountStatus.ACTIVE).organization(partnerOrg).build();
        partnerUser = User.builder().idx(21L).id("pu").name("협력사일반").email("u@p.io")
                .role(Roles.USER).accountStatus(UserAccountStatus.ACTIVE).organization(partnerOrg).build();

        campaign = Campaign.builder().idx(100L).name("Q3캠페인").build();

        pmMember = CampaignMember.builder()
                .campaign(campaign)
                .user(pmGm)
                .campaignRole(CampaignMemberRole.GENERAL_MANAGER)
                .build();
    }

    private void stubAuthAsPm() {
        when(userRepository.findUserById("pm")).thenReturn(Optional.of(pmGm));
        when(memberRepository.findByCampaignIdxAndUserIdx(100L, 10L)).thenReturn(Optional.of(pmMember));
        when(participantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(100L, 1L, CampaignRole.PM))
                .thenReturn(true);
        when(campaignRepository.findById(100L)).thenReturn(Optional.of(campaign));
    }

    @Test
    void invitePartnerGroup_정상_생성_및_알림() {
        stubAuthAsPm();
        when(invitationRepository.existsByCampaign_IdxAndInviteeOrganization_IdxAndStatus(100L, 2L, CampaignInvitationStatus.PENDING))
                .thenReturn(false);
        when(userRepository.findAllByOrganization_IdxAndAccountStatus(2L, UserAccountStatus.ACTIVE))
                .thenReturn(List.of(partnerGm, partnerUser));
        when(memberRepository.findAllByCampaignIdx(100L)).thenReturn(List.of(pmMember));
        when(invitationRepository.saveAndFlush(any(CampaignInvitation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var res = service.invitePartnerGroup(100L, "pm", 2L);

        assertThat(res.type()).isEqualTo(CampaignInvitationType.GROUP);
        assertThat(res.inviteeOrganizationIdx()).isEqualTo(2L);
        verify(notificationService).notifyCampaignGroupInvitation(any(CampaignInvitation.class), eq(2));
    }

    @Test
    void invitePartnerGroup_자기조직_400() {
        stubAuthAsPm();
        assertThatThrownBy(() -> service.invitePartnerGroup(100L, "pm", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("다른 조직");
    }

    @Test
    void invitePartnerGroup_PENDING존재_409() {
        stubAuthAsPm();
        when(invitationRepository.existsByCampaign_IdxAndInviteeOrganization_IdxAndStatus(100L, 2L, CampaignInvitationStatus.PENDING))
                .thenReturn(true);
        assertThatThrownBy(() -> service.invitePartnerGroup(100L, "pm", 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 발송");
    }

    @Test
    void invitePartnerGroup_GM없음_400() {
        stubAuthAsPm();
        when(invitationRepository.existsByCampaign_IdxAndInviteeOrganization_IdxAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(userRepository.findAllByOrganization_IdxAndAccountStatus(2L, UserAccountStatus.ACTIVE))
                .thenReturn(List.of(partnerUser)); // GM 없음
        assertThatThrownBy(() -> service.invitePartnerGroup(100L, "pm", 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GM");
    }

    @Test
    void invitePartnerGroup_전원이미참여_400() {
        stubAuthAsPm();
        when(invitationRepository.existsByCampaign_IdxAndInviteeOrganization_IdxAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(userRepository.findAllByOrganization_IdxAndAccountStatus(2L, UserAccountStatus.ACTIVE))
                .thenReturn(List.of(partnerGm, partnerUser));
        CampaignMember m1 = CampaignMember.builder().campaign(campaign).user(partnerGm)
                .campaignRole(CampaignMemberRole.GENERAL_MANAGER).build();
        CampaignMember m2 = CampaignMember.builder().campaign(campaign).user(partnerUser)
                .campaignRole(CampaignMemberRole.USER).build();
        when(memberRepository.findAllByCampaignIdx(100L)).thenReturn(List.of(pmMember, m1, m2));

        assertThatThrownBy(() -> service.invitePartnerGroup(100L, "pm", 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 전원");
    }

    @Test
    void acceptInvitation_GROUP_일괄합류() {
        when(userRepository.findUserById("pgm")).thenReturn(Optional.of(partnerGm));

        CampaignInvitation invitation = CampaignInvitation.builder()
                .idx(500L)
                .campaign(campaign)
                .inviter(pmGm)
                .invitee(partnerGm)
                .inviteeOrganization(partnerOrg)
                .status(CampaignInvitationStatus.PENDING)
                .type(CampaignInvitationType.GROUP)
                .build();
        when(invitationRepository.findByIdxAndCampaign_Idx(500L, 100L)).thenReturn(Optional.of(invitation));
        when(participantRepository.existsByCampaignIdxAndOrganizationIdx(100L, 2L)).thenReturn(true);
        when(userRepository.findAllByOrganization_IdxAndAccountStatus(2L, UserAccountStatus.ACTIVE))
                .thenReturn(List.of(partnerGm, partnerUser));
        when(memberRepository.findUserIdxByCampaignIdxAndUserIdxIn(eq(100L), any())).thenReturn(Set.of());
        when(memberRepository.save(any(CampaignMember.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.acceptInvitation(100L, 500L, "pgm");

        assertThat(res.joinedCount()).isEqualTo(2);
        verify(memberRepository, org.mockito.Mockito.times(2)).save(any(CampaignMember.class));
        assertThat(invitation.getStatus()).isEqualTo(CampaignInvitationStatus.ACCEPTED);
    }

    @Test
    void acceptInvitation_GROUP_일부이미멤버() {
        when(userRepository.findUserById("pgm")).thenReturn(Optional.of(partnerGm));
        CampaignInvitation invitation = CampaignInvitation.builder()
                .idx(501L).campaign(campaign).inviter(pmGm).invitee(partnerGm)
                .inviteeOrganization(partnerOrg).status(CampaignInvitationStatus.PENDING)
                .type(CampaignInvitationType.GROUP).build();
        when(invitationRepository.findByIdxAndCampaign_Idx(501L, 100L)).thenReturn(Optional.of(invitation));
        when(participantRepository.existsByCampaignIdxAndOrganizationIdx(100L, 2L)).thenReturn(true);
        when(userRepository.findAllByOrganization_IdxAndAccountStatus(2L, UserAccountStatus.ACTIVE))
                .thenReturn(List.of(partnerGm, partnerUser));
        when(memberRepository.findUserIdxByCampaignIdxAndUserIdxIn(eq(100L), any()))
                .thenReturn(Set.of(21L)); // partnerUser 이미 멤버

        var res = service.acceptInvitation(100L, 501L, "pgm");

        assertThat(res.joinedCount()).isEqualTo(1); // partnerGm만 신규
    }

    @Test
    void acceptInvitation_INDIVIDUAL_단일합류_기존동작_유지() {
        when(userRepository.findUserById("pgm")).thenReturn(Optional.of(partnerGm));
        CampaignInvitation invitation = CampaignInvitation.builder()
                .idx(502L).campaign(campaign).inviter(pmGm).invitee(partnerGm)
                .inviteeOrganization(partnerOrg).status(CampaignInvitationStatus.PENDING)
                .type(CampaignInvitationType.INDIVIDUAL).build();
        when(invitationRepository.findByIdxAndCampaign_Idx(502L, 100L)).thenReturn(Optional.of(invitation));
        when(participantRepository.existsByCampaignIdxAndOrganizationIdx(100L, 2L)).thenReturn(true);
        when(memberRepository.existsByCampaignIdxAndUserIdx(100L, 20L)).thenReturn(false);

        var res = service.acceptInvitation(100L, 502L, "pgm");

        assertThat(res.joinedCount()).isEqualTo(1);
        assertThat(res.type()).isEqualTo(CampaignInvitationType.INDIVIDUAL);
    }

    @Test
    void rejectInvitation_GROUP_REJECTED_상태_변경_및_멤버변화없음() {
        when(userRepository.findUserById("pgm")).thenReturn(Optional.of(partnerGm));
        CampaignInvitation invitation = CampaignInvitation.builder()
                .idx(503L).campaign(campaign).inviter(pmGm).invitee(partnerGm)
                .inviteeOrganization(partnerOrg).status(CampaignInvitationStatus.PENDING)
                .type(CampaignInvitationType.GROUP).build();
        when(invitationRepository.findByIdxAndCampaign_Idx(503L, 100L)).thenReturn(Optional.of(invitation));

        var res = service.rejectInvitation(100L, 503L, "pgm");

        assertThat(res.status()).isEqualTo(CampaignInvitationStatus.REJECTED);
        assertThat(invitation.getStatus()).isEqualTo(CampaignInvitationStatus.REJECTED);
        org.mockito.Mockito.verify(memberRepository, org.mockito.Mockito.never()).save(any(CampaignMember.class));
    }
}
