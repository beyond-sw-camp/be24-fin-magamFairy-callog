package org.example.backend.teamboard.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignParticipant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "TaskParts")
public class TaskParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private MileStones mileStones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_team_id")
    private CampaignParticipant participant;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String reviewFlow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority taskPriority = TaskPriority.MEDIUM;

    @Column(length = 300)
    private String dependency;

    @Column(length = 300)
    private String deliverable;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private Integer sortOrder = 0;

    public void update(
            MileStones milestone,
            CampaignParticipant ownerTeam,
            String name,
            String reviewFlow,
            TaskPriority priority,
            String dependency,
            String deliverable,
            String description
    ) {
        this.mileStones = milestone;
        this.participant = ownerTeam;
        this.name = name;
        this.reviewFlow = reviewFlow;
        this.taskPriority = priority;
        this.dependency = dependency;
        this.deliverable = deliverable;
        this.description = description;
    }

    public void changeMilestone(MileStones milestone) {
        this.mileStones = milestone;
    }
}
