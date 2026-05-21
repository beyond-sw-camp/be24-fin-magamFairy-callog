package org.example.backend.teamboard.model;

import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.user.model.User;

import java.time.LocalDateTime;

public class TaskDto {

    public record ReqTask(
            String name,
            Long participantId,
            LocalDateTime startDate,
            LocalDateTime dueDate,
            TaskType taskType,
            TaskStatus status,
            Long taskPartId,
            Long milestoneId,
            Long assigneeId,
            TaskPriority priority,
            String memo
    ) {
        public Task toEntity(
                CampaignParticipant participant,
                TaskParts taskPart,
                MileStones milestone,
                User assignee
        ) {
            return Task.builder()
                    .name(name)
                    .participant(participant)
                    .startDate(startDate)
                    .dueDate(dueDate)
                    .taskType(taskType)
                    .status(status != null ? status : TaskStatus.BACKLOG)
                    .taskPart(taskPart)
                    .milestone(milestone)
                    .assignee(assignee)
                    .priority(priority != null ? priority : TaskPriority.MEDIUM)
                    .memo(memo)
                    .build();
        }
    }

    public record ResTask(
            Long idx,
            String name,
            Long participantIdx,
            LocalDateTime startDate,
            LocalDateTime dueDate,
            TaskType taskType,
            TaskStatus status,
            Long taskPartIdx,
            String taskPartName,
            Long milestoneIdx,
            String milestoneName,
            Long assigneeIdx,
            String assigneeName,
            TaskPriority priority,
            String memo,
            LocalDateTime createdAt
    ) {
        public static ResTask from(Task entity) {
            return new ResTask(
                    entity.getIdx(),
                    entity.getName(),
                    entity.getParticipant() != null ? entity.getParticipant().getIdx() : null,
                    entity.getStartDate(),
                    entity.getDueDate(),
                    entity.getTaskType(),
                    entity.getStatus(),
                    entity.getTaskPart() != null ? entity.getTaskPart().getIdx() : null,
                    entity.getTaskPart() != null ? entity.getTaskPart().getName() : null,
                    entity.getMilestone() != null ? entity.getMilestone().getIdx() : null,
                    entity.getMilestone() != null ? entity.getMilestone().getName() : null,
                    entity.getAssignee() != null ? entity.getAssignee().getIdx() : null,
                    entity.getAssignee() != null ? entity.getAssignee().getName() : null,
                    entity.getPriority(),
                    entity.getMemo(),
                    entity.getCreatedAt()
            );
        }
    }

    public record ResList(
            Long idx,
            String name,
            TaskStatus status,
            TaskPriority priority,
            LocalDateTime startDate,
            LocalDateTime dueDate,
            String milestoneName,
            String taskPartName,
            String assigneeName,
            Long assigneeIdx,
            Long participantIdx,
            String companyName,
            Long campaignIdx
    ) {
        public static ResList from(Task entity) {
            String companyName = null;
            if (entity.getParticipant() != null && entity.getParticipant().getOrganization() != null) {
                companyName = entity.getParticipant().getOrganization().getName();
            }
            // 1급화: 직접 campaign 우선, 없으면 업무파트 경유
            Long campaignIdx = entity.getCampaign() != null
                    ? entity.getCampaign().getIdx()
                    : (entity.getTaskPart() != null && entity.getTaskPart().getCampaign() != null
                            ? entity.getTaskPart().getCampaign().getIdx() : null);
            return new ResList(
                    entity.getIdx(),
                    entity.getName(),
                    entity.getStatus(),
                    entity.getPriority(),
                    entity.getStartDate(),
                    entity.getDueDate(),
                    entity.getMilestone() != null ? entity.getMilestone().getName() : null,
                    entity.getTaskPart() != null ? entity.getTaskPart().getName() : null,
                    entity.getAssignee() != null ? entity.getAssignee().getName() : null,
                    entity.getAssignee() != null ? entity.getAssignee().getIdx() : null,
                    entity.getParticipant() != null ? entity.getParticipant().getIdx() : null,
                    companyName,
                    campaignIdx
            );
        }
    }

    public record ReqStatus(
            TaskStatus status
    ) {}
}
