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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.organization.model.Organization;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaign_participants")
public class CampaignParticipant extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Setter
    @Column(length = 100)
    private String userLoginId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignRole campaignRole = CampaignRole.PARTNER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParticipantStatus participantStatus = ParticipantStatus.ACTIVE;

    private LocalDate joinedDate;

    private LocalDate leftDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public void updateRole(CampaignRole campaignRole) {
        this.campaignRole = campaignRole == null ? CampaignRole.PARTNER : campaignRole;
    }

    public void activate() {
        this.participantStatus = ParticipantStatus.ACTIVE;
        this.leftDate = null;
        if (this.joinedDate == null) {
            this.joinedDate = LocalDate.now();
        }
    }

    public void deactivate() {
        this.participantStatus = ParticipantStatus.INACTIVE;
        this.leftDate = LocalDate.now();
    }
}
