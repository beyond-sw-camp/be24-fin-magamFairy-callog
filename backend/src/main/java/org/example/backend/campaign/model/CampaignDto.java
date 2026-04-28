package org.example.backend.campaign.model;

import lombok.Builder;

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
            String mainMessage,
            String color
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
            String mainMessage,
            String status,
            String initials,
            String color,
            Date createdAt,
            Date updatedAt
    ) {
        public static Res from(Campaign entity) {
            return Res.builder()
                    .id(String.valueOf(entity.getIdx()))
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .purpose(entity.getPurpose())
                    .tags(List.copyOf(entity.getTags()))
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .period(formatPeriod(entity.getStartDate(), entity.getEndDate()))
                    .partners(List.copyOf(entity.getPartners()))
                    .goals(entity.getGoals())
                    .mainMessage(entity.getMainMessage())
                    .status(entity.getStatus())
                    .initials(entity.getInitials())
                    .color(entity.getColor())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
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
