package org.example.backend.teamboard.model;


import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.user.model.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)  // 이거 추가
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "Task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, length = 200)
    private String name;

    // 참여사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private CampaignParticipant participant;

    // 마감일
    private LocalDateTime dueDate;

    // 업무 유형
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskType taskType;

    // 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.BACKLOG;

    // 업무 파트
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_part_id")
    private TaskParts taskPart;

    // 마일스톤
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private MileStones milestone;

    // 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    // 우선순위
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;


    // 업무 메모
    @Column(columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


    public void update(
            String name,
            CampaignParticipant participant,
            LocalDateTime dueDate,
            TaskType taskType,
            TaskStatus status,
            TaskParts taskParts,
            MileStones mileStones,
            User assignee,
            TaskPriority priority,
            String memo
    ) {
        this.name = name;
        this.participant = participant;
        this.dueDate = dueDate;
        this.taskType = taskType;
        this.status = status;
        this.taskPart = taskParts;
        this.milestone = mileStones;
        this.assignee = assignee;
        this.priority = priority;
        this.memo = memo;
    }
}
