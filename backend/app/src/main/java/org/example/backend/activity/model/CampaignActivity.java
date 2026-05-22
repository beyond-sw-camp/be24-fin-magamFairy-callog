package org.example.backend.activity.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.user.model.User;

import java.time.LocalDateTime;

/**
 * 캠페인별 활동 로그 (대시보드 Zone1 P1 "최근 활동" 피드용).
 * 업무/상태 변경 시 1행 기록(write). 피드는 캠페인별 최근순 조회.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaign_activity",
        indexes = @Index(name = "idx_campaign_activity_campaign", columnList = "campaign_id"))
public class CampaignActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    // 활동 주체 (시스템 자동 기록 등은 null 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    // TASK_CREATE / TASK_DONE / TASK_UPDATE / STATUS_CHANGE 등
    @Column(nullable = false, length = 40)
    private String type;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
