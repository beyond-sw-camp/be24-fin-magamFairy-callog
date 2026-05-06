package org.example.backend.userInfo.userProfile.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.User;
import org.example.backend.userInfo.userProfile.model.ProfileImageGenerationLog;
import org.example.backend.userInfo.userProfile.repository.ProfileImageGenerationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileImageGenerationLogService {
    private final ProfileImageGenerationLogRepository profileImageGenerationLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProfileImageGenerationLog createRequested(User user, String prompt, Integer requestedSize, String model) {
        return profileImageGenerationLogRepository.save(ProfileImageGenerationLog.builder()
                .user(user)
                .prompt(prompt)
                .requestedSize(requestedSize)
                .model(model)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(Long logIdx, String objectKey) {
        profileImageGenerationLogRepository.findById(logIdx).ifPresent(log -> {
            log.markSucceeded(objectKey);
            profileImageGenerationLogRepository.save(log);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long logIdx, String message) {
        profileImageGenerationLogRepository.findById(logIdx).ifPresent(log -> {
            log.markFailed(truncate(message));
            profileImageGenerationLogRepository.save(log);
        });
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Profile image generation failed.";
        }

        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}
