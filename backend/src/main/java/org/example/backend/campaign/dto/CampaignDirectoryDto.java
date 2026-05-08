package org.example.backend.campaign.dto;

import org.example.backend.campaign.model.Campaign;
import org.example.backend.organization.model.Organization;

import java.time.LocalDate;
import java.util.List;

/**
 * 캠페인 디렉토리(`/campaigns/browse`)용 슬림 DTO.
 * 카드 노출에 필요한 최소 필드만 — 검색·필터 결과 다수 반환 대응.
 */
public record CampaignDirectoryDto(
        Long idx,
        String name,
        String purpose,
        String status,
        String initials,
        String color,
        String icon,
        String thumbnailUrl,
        LocalDate startDate,
        LocalDate endDate,
        List<String> tags,
        Long ownerOrgId,
        String ownerOrgName,
        String ownerOrgType
) {
    public static CampaignDirectoryDto from(Campaign c, Organization ownerOrg, String thumbnailUrl) {
        return new CampaignDirectoryDto(
                c.getIdx(),
                c.getName(),
                c.getPurpose(),
                c.getStatus(),
                c.getInitials(),
                c.getColor(),
                c.getIcon(),
                thumbnailUrl,
                c.getStartDate(),
                c.getEndDate(),
                c.getTags() == null ? List.of() : List.copyOf(c.getTags()),
                ownerOrg != null ? ownerOrg.getIdx() : null,
                ownerOrg != null ? ownerOrg.getName() : null,
                ownerOrg != null && ownerOrg.getType() != null ? ownerOrg.getType().name() : null
        );
    }
}
