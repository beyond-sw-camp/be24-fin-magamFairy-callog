package org.example.backend.campaignframe.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.user.model.User;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(
        name = "campaign_frame",
        indexes = {
                @Index(name = "idx_campaign_frame_id", columnList = "id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_frame_owner_id", columnNames = {"owner_idx", "id"})
        }
)
public class CampaignFrame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @Column(nullable = false, length = 100)
    private String id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_idx", nullable = false)
    private User owner;

    @Setter
    @Column(nullable = false, length = 80)
    private String category;

    @Setter
    @Column(nullable = false, length = 40)
    private String version;

    @Setter
    @Column(nullable = false, length = 160)
    private String title;

    @Setter
    @Column(nullable = false)
    private Integer score;

    @Setter
    @Column(nullable = false, length = 40)
    private String status;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String overview;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_frame_required_field", joinColumns = @JoinColumn(name = "frame_idx"))
    @OrderColumn(name = "sort_order")
    @Column(name = "required_field", length = 300)
    private List<String> requiredFields = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_frame_banned_expression", joinColumns = @JoinColumn(name = "frame_idx"))
    @OrderColumn(name = "sort_order")
    @Column(name = "banned_expression", length = 300)
    private List<String> bannedExpressions = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_frame_recommended_expression", joinColumns = @JoinColumn(name = "frame_idx"))
    @OrderColumn(name = "sort_order")
    @Column(name = "recommended_expression", length = 300)
    private List<String> recommendedExpressions = new ArrayList<>();

    @Setter
    @Column(columnDefinition = "TEXT")
    private String toneGuide;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_frame_approval_process", joinColumns = @JoinColumn(name = "frame_idx"))
    @OrderColumn(name = "sort_order")
    @Column(name = "approval_step", length = 200)
    private List<String> approvalProcess = new ArrayList<>();

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private Integer usageCount = 0;

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private Integer passRate = 0;

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private Double avgRevisions = 0.0;

    public void update(CampaignFrameDto.UpsertReq dto) {
        this.category = dto.category();
        this.version = dto.version();
        this.title = dto.title();
        this.score = dto.score();
        this.status = dto.status();
        this.overview = dto.overview();
        replaceList(this.requiredFields, dto.requiredFields());
        replaceList(this.bannedExpressions, dto.bannedExpressions());
        replaceList(this.recommendedExpressions, dto.recommendedExpressions());
        this.toneGuide = dto.toneGuide();
        replaceList(this.approvalProcess, dto.approvalProcess());
        this.usageCount = dto.performance().usageCount();
        this.passRate = dto.performance().passRate();
        this.avgRevisions = dto.performance().avgRevisions();
    }

    private void replaceList(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }
}
