package org.example.backend.campaign.model;

import lombok.Builder;
import org.example.backend.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

public class CampaignMemberDto {

    @Builder
    public record Res(
            Long idx,
            Long userIdx,
            String userId,
            String name,
            String email,
            String companyName,
            String department,
            String globalRole,
            CampaignMemberRole campaignRole,
            Long organizationIdx,
            LocalDateTime joinedAt
    ) {
        public static Res from(CampaignMember entity) {
            User u = entity.getUser();
            return Res.builder()
                    .idx(entity.getIdx())
                    .userIdx(u.getIdx())
                    .userId(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .companyName(u.getCompanyName())
                    .department(u.getDepartment())
                    .globalRole(u.getRole())
                    .campaignRole(entity.getCampaignRole())
                    .organizationIdx(u.getOrganization() != null ? u.getOrganization().getIdx() : null)
                    .joinedAt(entity.getJoinedAt())
                    .build();
        }
    }

    @Builder
    public record ListRes(
            List<Res> members,
            Res me,
            boolean organizationIsPm
    ) {}

    public record AddTeamReq(List<Long> userIdxList) {}

    public record InvitePartnerReq(Long userIdx) {}

    public record UpdateRoleReq(CampaignMemberRole campaignRole) {}

    @Builder
    public record InvitationRes(
            Long idx,
            Long campaignIdx,
            String campaignName,
            Long inviterIdx,
            String inviterName,
            Long inviteeIdx,
            String inviteeName,
            Long inviteeOrganizationIdx,
            String inviteeOrganizationName,
            CampaignInvitationStatus status,
            LocalDateTime createdAt,
            LocalDateTime respondedAt
    ) {
        public static InvitationRes from(CampaignInvitation entity) {
            User inviter = entity.getInviter();
            User invitee = entity.getInvitee();

            return InvitationRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .campaignName(entity.getCampaign().getName())
                    .inviterIdx(inviter != null ? inviter.getIdx() : null)
                    .inviterName(inviter != null ? inviter.getName() : null)
                    .inviteeIdx(invitee != null ? invitee.getIdx() : null)
                    .inviteeName(invitee != null ? invitee.getName() : null)
                    .inviteeOrganizationIdx(entity.getInviteeOrganization() != null
                            ? entity.getInviteeOrganization().getIdx()
                            : null)
                    .inviteeOrganizationName(entity.getInviteeOrganization() != null
                            ? entity.getInviteeOrganization().getName()
                            : null)
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                    .respondedAt(entity.getRespondedAt())
                    .build();
        }
    }

    public record ParticipantRes(Long idx, String organizationName) {
        public static ParticipantRes from(CampaignParticipant entity) {
            return new ParticipantRes(
                    entity.getIdx(),
                    entity.getOrganization() != null ? entity.getOrganization().getName() : null
            );
        }
    }

    @Builder
    public record CandidateRes(
            Long userIdx,
            String userId,
            String name,
            String email,
            String companyName,
            String department,
            String globalRole,
            Long organizationIdx,
            String organizationName
    ) {}
}
