package org.example.backend.teamboard.model;


import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.Campaign;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
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
}
