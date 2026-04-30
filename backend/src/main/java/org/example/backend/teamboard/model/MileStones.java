package org.example.backend.teamboard.model;


import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.Campaign;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "MileStones")
public class MileStones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void update(
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String description,
            Integer sortOrder
    ) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }
}
