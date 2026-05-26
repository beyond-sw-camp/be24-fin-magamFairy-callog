package org.example.backend.adcheck.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_check_outbox_event")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdCheckOutboxEvent extends BaseEntity {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, length = 80)
    private String aggregateId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer attemptCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime publishedAt;

    public static AdCheckOutboxEvent pending(String aggregateId, String eventType, String payload) {
        return AdCheckOutboxEvent.builder()
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(STATUS_PENDING)
                .attemptCount(0)
                .build();
    }

    public void markPublished() {
        this.status = STATUS_PUBLISHED;
        this.errorMessage = null;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed(String message) {
        this.status = STATUS_FAILED;
        this.errorMessage = message;
        this.attemptCount = (attemptCount == null ? 0 : attemptCount) + 1;
    }
}
