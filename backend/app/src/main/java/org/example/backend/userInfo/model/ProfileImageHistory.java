package org.example.backend.userInfo.model;

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
public class ProfileImageHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_type", nullable = false, length = 20)
    private ProfileImageHistoryType historyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileImageSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_log_idx")
    private ProfileImageGenerationLog generationLog;

    @Column(length = 1000)
    private String prompt;
}
