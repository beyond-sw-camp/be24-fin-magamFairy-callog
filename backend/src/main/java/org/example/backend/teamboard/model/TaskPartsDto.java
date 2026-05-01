package org.example.backend.teamboard.model;

import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignParticipant;

import java.time.LocalDateTime;

public class TaskPartsDto {

    public record ReqTaskParts(
            String name,
            String reviewFlow,
            TaskPriority taskPriority,
            String dependency,
            String deliverable,
            String description,
            Integer sortOrder
    ) {
        public TaskParts toEntity(Campaign campaign, MileStones mileStones, CampaignParticipant participant) {
            return TaskParts.builder()
                    .campaign(campaign)
                    .mileStones(mileStones)
                    .participant(participant)
                    .name(this.name)
                    .reviewFlow(this.reviewFlow)
                    .taskPriority(this.taskPriority)
                    .dependency(this.dependency)
                    .deliverable(this.deliverable)
                    .description(this.description)
                    .sortOrder(sortOrder != null ? this.sortOrder : 0)
                    .build();
        }
    }

    public record ResTaskParts(
            Long idx,
            String name,
            String reviewFlow,
            TaskPriority taskPriority,
            String dependency,
            String deliverable,
            String description,
            Long milestoneId,
            LocalDateTime createdAt
    ) {
        public static ResTaskParts from(TaskParts entity) {
            return new ResTaskParts(
                    entity.getIdx(),
                    entity.getName(),
                    entity.getReviewFlow(),
                    entity.getTaskPriority(),
                    entity.getDependency(),
                    entity.getDeliverable(),
                    entity.getDescription(),
                    entity.getMileStones() != null ? entity.getMileStones().getIdx() : null,
                    entity.getCreatedAt()
            );
        }
    }

    public record ResList(
            Long idx,
            String name,
            TaskPriority taskPriority,
            Long milestoneIdx
    ) {
        public static ResList from(TaskParts entity) {
            return new ResList(
                    entity.getIdx(),
                    entity.getName(),
                    entity.getTaskPriority(),
                    entity.getMileStones() != null ? entity.getMileStones().getIdx() : null
            );
        }
    }

    public record ReqPriority(
            TaskPriority taskPriority
    ) {}
}
