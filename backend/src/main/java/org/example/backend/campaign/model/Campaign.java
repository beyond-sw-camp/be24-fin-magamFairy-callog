package org.example.backend.campaign.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaign")
public class Campaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, length = 100)
    private String ownerLoginId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_tag", joinColumns = @JoinColumn(name = "campaign_idx"))
    @Column(name = "tag", length = 80)
    private List<String> tags = new ArrayList<>();

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_partner", joinColumns = @JoinColumn(name = "campaign_idx"))
    @Column(name = "partner", length = 120)
    private List<String> partners = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(columnDefinition = "TEXT")
    private String mainMessage;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "draft";

    @Column(nullable = false, length = 20)
    private String initials;

    @Column(nullable = false, length = 20)
    private String color;

    public void updateDetails(
            String name,
            String purpose,
            List<String> tags,
            LocalDate startDate,
            LocalDate endDate,
            List<String> partners,
            String goals,
            String mainMessage,
            String initials,
            String color
    ) {
        this.name = name;
        this.purpose = purpose;
        this.tags.clear();
        this.tags.addAll(tags);
        this.startDate = startDate;
        this.endDate = endDate;
        this.partners.clear();
        this.partners.addAll(partners);
        this.goals = goals;
        this.mainMessage = mainMessage;
        this.initials = initials;
        this.color = color;
    }

    public void updatePartners(List<String> partners) {
        this.partners.clear();
        this.partners.addAll(partners);
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}
