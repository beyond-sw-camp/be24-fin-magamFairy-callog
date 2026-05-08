package org.example.backend.kpi.dto;

import org.example.backend.kpi.model.CampaignKpiContribution;
import org.example.backend.kpi.model.OrganizationKpi;
import org.example.backend.organization.model.Organization;

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
        LocalDateTime updatedAt,
        String ownerOrgType,
        String ownerOrgName,
        String periodCode
) {
    public static CampaignKpiContributionDto from(CampaignKpiContribution c) {
        OrganizationKpi orgKpi = c.getTargetOrgKpi();
        Organization owner = orgKpi != null ? orgKpi.getOwner() : null;
        return new CampaignKpiContributionDto(
                c.getIdx(),
                c.getCampaign() != null ? c.getCampaign().getIdx() : null,
                c.getCampaign() != null ? c.getCampaign().getName() : null,
                orgKpi != null ? orgKpi.getIdx() : null,
                orgKpi != null ? orgKpi.getName() : null,
                orgKpi != null ? orgKpi.getUnit() : null,
                orgKpi != null ? orgKpi.getTargetValue() : null,
                c.getCommittedValue(),
                c.getActualValue(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                owner != null && owner.getType() != null ? owner.getType().name() : null,
                owner != null ? owner.getName() : null,
                orgKpi != null ? orgKpi.getPeriodCode() : null
        );
    }
}
