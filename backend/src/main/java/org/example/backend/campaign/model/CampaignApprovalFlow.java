package org.example.backend.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaign_approval_flows")
public class CampaignApprovalFlow extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, length = 100)
    private String requestedBy;

    private LocalDate requestedDate;

    @Column(nullable = false, length = 100)
    private String approverLoginId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private LocalDate approvedDate;

    @Column(columnDefinition = "TEXT")
    private String approverComment;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approval_sequence", nullable = false)
    private Integer sequence;

    public void approve(String approverComment) {
        this.status = ApprovalStatus.APPROVED;
        this.approverComment = approverComment;
        this.rejectionReason = null;
        this.approvedDate = LocalDate.now();
    }

    public void reject(String rejectionReason) {
        this.status = ApprovalStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.approvedDate = LocalDate.now();
    }

    public void requestRevision(String approverComment) {
        this.status = ApprovalStatus.REVISION_NEEDED;
        this.approverComment = approverComment;
        this.approvedDate = LocalDate.now();
    }
}
