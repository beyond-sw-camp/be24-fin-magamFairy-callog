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
