package org.example.backend.campaign.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 캘린더 페이지 일괄 조회용 DTO.
 * 기존 N+1 (캠페인 N개 × intro/milestones 각각 호출) 을 1회 호출로 압축.
 */
public class CalendarEventsDto {

    public record Res(
            List<CampaignItem> campaigns,
            List<DeadlineItem> deadlines,
            List<MilestoneItem> milestones
    ) {}

    /** 캠페인 기간 (start ~ end). idx + publicId 둘 다 노출 — controller는 publicId, 식별자는 idx. */
    public record CampaignItem(
            Long idx,
            String publicId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String ownerName,
            String color,
            String icon,
            String myCampaignRole,
            boolean organizationIsPm
    ) {}

    /** 모집 마감 — CampaignIntro.recruitDeadline 단일 시점. */
    public record DeadlineItem(
            Long campaignIdx,
            String campaignPublicId,
            String campaignName,
            LocalDateTime recruitDeadline
    ) {}

    /** 마일스톤 — start ~ end (start 또는 end 둘 중 하나는 있어야 함). */
    public record MilestoneItem(
            Long idx,
            Long campaignIdx,
            String campaignPublicId,
            String campaignName,
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {}
}
