package org.example.backend.campaign.service;

import org.example.backend.campaign.model.ApprovalStatus;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignApprovalFlow;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.model.CampaignInvitation;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.model.InvitationStatus;
import org.example.backend.campaign.model.ParticipantStatus;
import org.example.backend.campaign.repository.CampaignAnalyticsRepository;
import org.example.backend.campaign.repository.CampaignApprovalFlowRepository;
import org.example.backend.campaign.repository.CampaignCommentRepository;
import org.example.backend.campaign.repository.CampaignContentRepository;
import org.example.backend.campaign.repository.CampaignInvitationRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.campaign.repository.CampaignVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {
    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignInvitationRepository invitationRepository;

    @Mock
    private CampaignParticipantRepository participantRepository;

    @Mock
    private CampaignApprovalFlowRepository approvalFlowRepository;

    @Mock
    private CampaignVersionRepository versionRepository;

    @Mock
    private CampaignCommentRepository commentRepository;

    @Mock
    private CampaignContentRepository contentRepository;

    @Mock
    private CampaignAnalyticsRepository analyticsRepository;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    void updateStatusAcceptsFrontendStatusValues() {
        Campaign campaign = campaign(1L, "owner", "Launch", "draft");
        when(campaignRepository.findByIdxAndOwnerLoginId(1L, "owner")).thenReturn(Optional.of(campaign));

        CampaignDto.Res response = campaignService.updateStatus(
                "owner",
                1L,
                new CampaignDto.StatusReq("partner_done")
        );

        assertThat(response.status()).isEqualTo("partner_done");
        assertThat(campaign.getStatus()).isEqualTo("partner_done");
    }

    @Test
    void updateStatusRejectsUnsupportedValues() {
        Campaign campaign = campaign(1L, "owner", "Launch", "draft");
        when(campaignRepository.findByIdxAndOwnerLoginId(1L, "owner")).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.updateStatus(
                "owner",
                1L,
                new CampaignDto.StatusReq("archived")
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported campaign status");
    }

    @Test
    void getCampaignsFiltersByStatusKeywordAndDateRange() {
        Campaign matched = Campaign.builder()
                .idx(1L)
                .ownerLoginId("owner")
                .name("Spring Launch")
                .purpose("Launch season")
                .tags(List.of("VIP", "SNS"))
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .partners(List.of("Partner A"))
                .goals("Reach")
                .mainMessage("Main")
                .status("live")
                .initials("SL")
                .color("#8B5CF6")
                .build();
        Campaign wrongStatus = campaign(2L, "owner", "Spring Draft", "draft");
        Campaign wrongKeyword = campaign(3L, "owner", "Summer", "live");
        when(campaignRepository.findAllByOwnerLoginIdOrderByIdxDesc("owner"))
                .thenReturn(List.of(matched, wrongStatus, wrongKeyword));

        List<CampaignDto.Res> responses = campaignService.getCampaigns(
                "owner",
                "live",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                List.of("VIP"),
                "launch"
        );

        assertThat(responses).extracting(CampaignDto.Res::idx).containsExactly(1L);
    }

    @Test
    void invitePartnersCreatesPendingInvitationRecordsAndKeepsLegacyPartners() {
        Campaign campaign = campaign(1L, "owner", "Launch", "draft");
        when(campaignRepository.findByIdxAndOwnerLoginId(1L, "owner")).thenReturn(Optional.of(campaign));
        when(invitationRepository.save(any(CampaignInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignDto.InvitationListRes response = campaignService.invitePartners(
                "owner",
                1L,
                new CampaignDto.PartnerInviteReq(List.of("Partner A", "Partner B"), CampaignRole.PARTNER)
        );

        ArgumentCaptor<CampaignInvitation> captor = ArgumentCaptor.forClass(CampaignInvitation.class);
        verify(invitationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(CampaignInvitation::getInvitee)
                .containsExactly("Partner A", "Partner B");
        assertThat(captor.getAllValues()).extracting(CampaignInvitation::getStatus)
                .containsOnly(InvitationStatus.PENDING);
        assertThat(campaign.getPartners()).containsExactly("Partner A", "Partner B");
        assertThat(response.invitations()).hasSize(2);
    }

    @Test
    void acceptInvitationCreatesParticipant() {
        Campaign campaign = campaign(1L, "owner", "Launch", "draft");
        CampaignInvitation invitation = CampaignInvitation.builder()
                .idx(10L)
                .campaign(campaign)
                .invitee("partner@example.com")
                .invitedBy("owner")
                .role(CampaignRole.PARTNER)
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDate.now().plusDays(1))
                .invitationToken("token")
                .build();
        when(invitationRepository.findByIdxAndCampaignIdx(10L, 1L)).thenReturn(Optional.of(invitation));
        when(participantRepository.findByCampaignIdxAndUserLoginId(1L, "partner"))
                .thenReturn(Optional.empty());
        when(participantRepository.save(any(CampaignParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignDto.InvitationRes response = campaignService.acceptInvitation("partner", 1L, 10L);

        ArgumentCaptor<CampaignParticipant> captor = ArgumentCaptor.forClass(CampaignParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(response.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(captor.getValue().getCampaign()).isEqualTo(campaign);
        assertThat(captor.getValue().getUserLoginId()).isEqualTo("partner");
        assertThat(captor.getValue().getParticipantStatus()).isEqualTo(ParticipantStatus.ACTIVE);
    }

    @Test
    void approveApprovalRequestMovesCampaignLive() {
        Campaign campaign = campaign(1L, "owner", "Launch", "review");
        CampaignApprovalFlow flow = CampaignApprovalFlow.builder()
                .idx(20L)
                .campaign(campaign)
                .requestedBy("owner")
                .approverLoginId("approver")
                .status(ApprovalStatus.PENDING)
                .sequence(1)
                .build();
        when(approvalFlowRepository.findByIdxAndCampaignIdx(20L, 1L)).thenReturn(Optional.of(flow));

        CampaignDto.ApprovalRes response = campaignService.approveApproval(
                "approver",
                1L,
                20L,
                new CampaignDto.ApprovalDecisionReq("approved")
        );

        assertThat(response.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(campaign.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(campaign.getStatus()).isEqualTo("live");
    }

    @Test
    void cloneCampaignCreatesDraftCopy() {
        Campaign source = Campaign.builder()
                .idx(1L)
                .ownerLoginId("owner")
                .name("Original")
                .purpose("Purpose")
                .tags(List.of("VIP"))
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .partners(List.of("Partner A"))
                .goals("Goals")
                .mainMessage("Message")
                .status("completed")
                .initials("OR")
                .color("#8B5CF6")
                .build();
        when(campaignRepository.findByIdxAndOwnerLoginId(1L, "owner")).thenReturn(Optional.of(source));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignDto.Res response = campaignService.cloneCampaign(
                "owner",
                1L,
                new CampaignDto.CloneReq("Copy", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))
        );

        ArgumentCaptor<Campaign> captor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        assertThat(response.name()).isEqualTo("Copy");
        assertThat(captor.getValue().getStatus()).isEqualTo("draft");
        assertThat(captor.getValue().getTemplateSourceId()).isEqualTo("1");
        assertThat(captor.getValue().getTags()).containsExactly("VIP");
    }

    private static Campaign campaign(Long idx, String ownerLoginId, String name, String status) {
        return Campaign.builder()
                .idx(idx)
                .ownerLoginId(ownerLoginId)
                .name(name)
                .purpose("")
                .tags(List.of())
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .partners(List.of())
                .goals("")
                .mainMessage("")
                .status(status)
                .initials("CP")
                .color("#8B5CF6")
                .build();
    }
}
