package org.example.backend.kpi.dto;

import org.example.backend.kpi.model.CampaignKpiContribution;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignKpiContributionDto(
        Long idx,
        Long campaignId,
        String campaignName,
        Long targetOrgKpiId,
        String targetOrgKpiName,
        String targetOrgKpiUnit,
        BigDecimal targetOrgKpiTargetValue,
        BigDecimal committedValue,
        BigDecimal actualValue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CampaignKpiContributionDto from(CampaignKpiContribution c) {
        return new CampaignKpiContributionDto(
                c.getIdx(),
                c.getCampaign() != null ? c.getCampaign().getIdx() : null,
                c.getCampaign() != null ? c.getCampaign().getName() : null,
                c.getTargetOrgKpi() != null ? c.getTargetOrgKpi().getIdx() : null,
                c.getTargetOrgKpi() != null ? c.getTargetOrgKpi().getName() : null,
                c.getTargetOrgKpi() != null ? c.getTargetOrgKpi().getUnit() : null,
                c.getTargetOrgKpi() != null ? c.getTargetOrgKpi().getTargetValue() : null,
                c.getCommittedValue(),
                c.getActualValue(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
