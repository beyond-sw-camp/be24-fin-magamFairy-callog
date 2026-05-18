package org.example.backend.campaign.model;

import lombok.Builder;
import org.example.backend.kpi.dto.CreateContributionRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class CampaignDto {
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public record UpsertReq(
            String name,
            String purpose,
            List<String> tags,
            LocalDate startDate,
            LocalDate endDate,
            List<String> partners,
            String goals,
            List<CreateContributionRequest> contributions,
            String assetName,
            String assetDescription,
            String primaryGoal,
            List<String> campaignMethods,
            String maxCost,
            String minRevenue,
            List<Long> ownerUserIdxs,
            String color,
            String icon
    ) {
    }

    public record PartnerInviteReq(List<String> partners) {
    }

    public record StatusReq(String status) {
    }

    @Builder
    public record Res(
            String id,
            Long idx,
            String name,
            String purpose,
            List<String> tags,
            LocalDate startDate,
            LocalDate endDate,
            String period,
            List<String> partners,
            String goals,
            String assetName,
            String assetDescription,
            String primaryGoal,
            List<String> campaignMethods,
            String maxCost,
            String minRevenue,
            String status,
            String initials,
            String icon,
            String color,
            Date createdAt,
            Date updatedAt,
            CampaignMemberRole myCampaignRole,
            boolean organizationIsPm,
            Integer totalTaskCount        // Dashboard Z1/P1 진행률 카드의 분모. 조회 컨텍스트에서 채워지면 값, 아니면 null.
    ) {
        public static Res from(Campaign entity) {
            return from(entity, null, false, null);
        }

        public static Res from(Campaign entity, CampaignMemberRole myCampaignRole, boolean organizationIsPm) {
            return from(entity, myCampaignRole, organizationIsPm, null);
        }

        public static Res from(Campaign entity, CampaignMemberRole myCampaignRole, boolean organizationIsPm,
                               Integer totalTaskCount) {
            return Res.builder()
                    .id(entity.getPublicId() != null ? entity.getPublicId() : String.valueOf(entity.getIdx()))
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .purpose(entity.getPurpose())
                    .tags(List.copyOf(entity.getTags()))
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .period(formatPeriod(entity.getStartDate(), entity.getEndDate()))
                    .partners(List.copyOf(entity.getPartners()))
                    .goals(entity.getGoals())
                    .assetName(entity.getAssetName())
                    .assetDescription(entity.getAssetDescription())
                    .primaryGoal(entity.getPrimaryGoal())
                    .campaignMethods(List.copyOf(entity.getCampaignMethods()))
                    .maxCost(entity.getMaxCost())
                    .minRevenue(entity.getMinRevenue())
                    .status(entity.getStatus())
                    .initials(entity.getInitials())
                    .icon(entity.getIcon())
                    .color(entity.getColor())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .myCampaignRole(myCampaignRole)
                    .organizationIsPm(organizationIsPm)
                    .totalTaskCount(totalTaskCount)
                    .build();
        }
    }

    private static String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "";
        }

        String start = startDate == null ? "" : startDate.format(PERIOD_FORMATTER);
        String end = endDate == null ? "" : endDate.format(PERIOD_FORMATTER);
        return start + " - " + end;
    }
}
