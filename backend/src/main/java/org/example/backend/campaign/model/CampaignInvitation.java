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
@Table(name = "campaign_invitations")
public class CampaignInvitation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, length = 120)
    private String invitee;

    @Column(nullable = false, length = 100)
    private String invitedBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignRole role = CampaignRole.PARTNER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitationStatus status = InvitationStatus.PENDING;

    private LocalDate expiryDate;

    private LocalDate respondedDate;

    @Column(length = 100)
    private String respondedBy;

    @Column(columnDefinition = "TEXT")
    private String responseMessage;

    @Column(nullable = false, unique = true, length = 80)
    private String invitationToken;

    public void accept(String respondedBy) {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedBy = respondedBy;
        this.respondedDate = LocalDate.now();
        this.responseMessage = null;
    }

    public void reject(String respondedBy, String responseMessage) {
        this.status = InvitationStatus.REJECTED;
        this.respondedBy = respondedBy;
        this.respondedDate = LocalDate.now();
        this.responseMessage = responseMessage;
    }

    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
        this.respondedDate = LocalDate.now();
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }
}
