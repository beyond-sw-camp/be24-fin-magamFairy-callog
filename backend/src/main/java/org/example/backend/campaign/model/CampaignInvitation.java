package org.example.backend.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "campaign_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_campaign_invitation_pending",
                columnNames = {"campaign_idx", "invitee_idx", "status"}
        ),
        indexes = {
                @Index(name = "idx_campaign_invitation_invitee", columnList = "invitee_idx,status"),
                @Index(name = "idx_campaign_invitation_campaign", columnList = "campaign_idx,status")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CampaignInvitation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_idx", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_idx", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_idx", nullable = false)
    private User invitee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_organization_idx")
    private Organization inviteeOrganization;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignInvitationStatus status = CampaignInvitationStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignInvitationType type = CampaignInvitationType.INDIVIDUAL;

    private LocalDateTime respondedAt;

    public void accept() {
        this.status = CampaignInvitationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = CampaignInvitationStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }
}
