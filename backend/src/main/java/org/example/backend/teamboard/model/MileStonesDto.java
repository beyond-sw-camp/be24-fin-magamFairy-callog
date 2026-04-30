package org.example.backend.teamboard.model;

import org.example.backend.campaign.model.Campaign;

import java.time.LocalDateTime;

public class MileStonesDto {

    public record ReqMileStones(
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String description,
            Integer sortOrder
    ) {
        public MileStones toEntity(Campaign campaign) {
            return MileStones.builder()
                    .campaign(campaign)
                    .name(this.name)
                    .startDate(this.startDate)
                    .endDate(this.endDate)
                    .description(this.description)
                    .sortOrder(this.sortOrder != null ? this.sortOrder : 0)
                    .build();
        }
    }

    public record ResMileStones(
            Long idx,
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String description,
            Integer sortOrder
    ) {
        public ResMileStones from(MileStones entity) {
            return new ResMileStones(
                    entity.getIdx(),
                    entity.getName(),
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getDescription(),
                    entity.getSortOrder()
            );
        }
    }
    public record ResList(
            Long idx,
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        public static ResList from(MileStones entity) {
            return new ResList(
                    entity.getIdx(),
                    entity.getName(),
                    entity.getStartDate(),
                    entity.getEndDate()
            );
        }
    }
}
