package org.example.backend.reference.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "reference_items")
public class Reference extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false)
    private String ownerLoginId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(columnDefinition = "TEXT")
    private String thumbnail;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "reference_tag", joinColumns = @JoinColumn(name = "reference_idx"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    private String channel;

    private String objective;

    private String status;

    private LocalDate referenceDate;

    public void update(
            String type,
            String title,
            String url,
            String thumbnail,
            String description,
            List<String> tags,
            String channel,
            String objective,
            String status,
            LocalDate referenceDate
    ) {
        this.type = type;
        this.title = title;
        this.url = url;
        this.thumbnail = thumbnail;
        this.description = description;
        this.tags.clear();
        this.tags.addAll(tags);
        this.channel = channel;
        this.objective = objective;
        this.status = status;
        this.referenceDate = referenceDate;
    }
}
