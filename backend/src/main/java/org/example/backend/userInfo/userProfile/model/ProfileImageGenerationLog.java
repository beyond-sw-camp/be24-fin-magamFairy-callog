package org.example.backend.userInfo.userProfile.model;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.user.model.User;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
public class ProfileImageGenerationLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "requested_size", nullable = false)
    private Integer requestedSize;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileImageGenerationStatus status = ProfileImageGenerationStatus.REQUESTED;

    @Column(name = "generated_object_key", length = 512)
    private String generatedObjectKey;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public void markSucceeded(String objectKey) {
        this.status = ProfileImageGenerationStatus.SUCCEEDED;
        this.generatedObjectKey = objectKey;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.status = ProfileImageGenerationStatus.FAILED;
        this.errorMessage = message;
    }
}
