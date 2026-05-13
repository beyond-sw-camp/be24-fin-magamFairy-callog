package org.example.backend.userInfo.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.userInfo.model.ProfileImageGenerationLog;
import org.example.backend.userInfo.model.ProfileImageHistory;
import org.example.backend.userInfo.model.ProfileImageHistoryType;
import org.example.backend.userInfo.model.ProfileImageSource;
import org.example.backend.userInfo.model.UserProfile;
import org.example.backend.userInfo.model.UserProfileDto;
import org.example.backend.userInfo.repository.ProfileImageGenerationLogRepository;
import org.example.backend.userInfo.repository.ProfileImageHistoryRepository;
import org.example.backend.userInfo.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private static final int MAX_HISTORY_COUNT = 3;
    private static final int PROFILE_IMAGE_SIZE = 1024;
    private static final int MAX_GENERATION_PROMPT_LENGTH = 200;
    private static final String GENERATED_PROFILE_IMAGE_CONTENT_TYPE = "image/png";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileImageHistoryRepository profileImageHistoryRepository;
    private final ProfileImageGenerationLogRepository profileImageGenerationLogRepository;
    private final ProfileImageStorageService profileImageStorageService;
    private final ProfileImageGenerationLogService profileImageGenerationLogService;
    private final OpenAiProfileImageClient openAiProfileImageClient;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public void ensureProfilesForExistingUsers() {
        userRepository.findAll().forEach(this::ensureProfile);
    }

    @Transactional
    public UserProfile ensureProfile(User user) {
        if (user == null || user.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "persisted user is required.");
        }

        return userProfileRepository.findByUserIdx(user.getIdx())
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder()
                        .user(user)
                        .email(user.getEmail())
                        .build()));
    }

    @Transactional
    public UserProfileDto.Res getMyProfile(String userId) {
        return toResponse(getOrCreateProfile(userId));
    }

    @Transactional(readOnly = true)
    public String getProfileImageUrl(User user) {
        if (user == null || user.getIdx() == null) {
            return null;
        }

        return userProfileRepository.findByUserIdx(user.getIdx())
                .map(UserProfile::getProfileImageKey)
                .map(profileImageStorageService::createViewUrl)
                .orElse(null);
    }

    @Transactional
    public UserProfileDto.Res updateMyProfile(String userId, UserProfileDto.UpdateReq dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }

        UserProfile userProfile = getOrCreateProfile(userId);
        userProfile.updateContact(
                normalize(dto.email()),
                normalize(dto.phone())
        );

        return toResponse(userProfile);
    }

    @Transactional
    public UserProfileDto.ProfileImageUploadUrlRes createProfileImageUploadUrl(
            String userId,
            UserProfileDto.ProfileImageUploadUrlReq dto
    ) {
        User user = findUser(userId);
        ensureProfile(user);

        return profileImageStorageService.createUploadUrl(user, dto);
    }

    @Transactional
    public UserProfileDto.Res updateProfileImage(String userId, UserProfileDto.ProfileImageCommitReq dto) {
        if (dto == null || dto.objectKey() == null || dto.objectKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey is required.");
        }

        User user = findUser(userId);
        UserProfile userProfile = ensureProfile(user);
        String objectKey = dto.objectKey().trim();

        if (!profileImageStorageService.isProfileImageKeyForUser(user, objectKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "profile image key is not allowed.");
        }

        profileImageStorageService.validateUploadedObject(objectKey);

        String previousObjectKey = userProfile.getProfileImageKey();
        userProfile.updateProfileImageKey(objectKey);
        saveHistory(user, objectKey, ProfileImageHistoryType.APPLIED, ProfileImageSource.MANUAL, null, null);
        pruneHistory(user, userProfile, ProfileImageHistoryType.APPLIED);

        if (previousObjectKey != null && !previousObjectKey.equals(objectKey)) {
            deleteObjectIfUnreferenced(userProfile, previousObjectKey);
        }

        return toResponse(userProfile);
    }

    @Transactional
    public UserProfileDto.Res deleteProfileImage(String userId) {
        UserProfile userProfile = getOrCreateProfile(userId);
        String previousObjectKey = userProfile.getProfileImageKey();

        userProfile.clearProfileImage();
        deleteObjectIfUnreferenced(userProfile, previousObjectKey);

        return toResponse(userProfile);
    }

    public UserProfileDto.ProfileImageGenerateRes generateProfileImage(
            String userId,
            UserProfileDto.ProfileImageGenerateReq dto
    ) {
        String prompt = resolveGenerationPrompt(dto);
        Integer requestedSize = resolveGenerationSize(dto);
        User user = findUser(userId);
        ProfileImageGenerationLog generationLog = profileImageGenerationLogService.createRequested(
                user,
                prompt,
                requestedSize,
                openAiProfileImageClient.getModel()
        );
        String objectKey = null;

        try {
            byte[] imageBytes = openAiProfileImageClient.generateProfileImage(prompt);
            objectKey = profileImageStorageService.createGeneratedProfileImageKey(user);
            profileImageStorageService.uploadProfileImageObject(
                    objectKey,
                    imageBytes,
                    GENERATED_PROFILE_IMAGE_CONTENT_TYPE
            );
            String generatedObjectKey = objectKey;
            UserProfileDto.ProfileImageGenerateRes response = transactionTemplate.execute(status -> {
                User transactionalUser = findUser(userId);
                UserProfile userProfile = ensureProfile(transactionalUser);
                ProfileImageGenerationLog managedGenerationLog = profileImageGenerationLogRepository
                        .findById(generationLog.getIdx())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "profile image generation log not found."
                        ));

                managedGenerationLog.markSucceeded(generatedObjectKey);
                ProfileImageHistory history = saveHistory(
                        transactionalUser,
                        generatedObjectKey,
                        ProfileImageHistoryType.GENERATED,
                        ProfileImageSource.AI,
                        managedGenerationLog,
                        prompt
                );

                pruneHistory(transactionalUser, userProfile, ProfileImageHistoryType.GENERATED);

                return UserProfileDto.ProfileImageGenerateRes.builder()
                        .generationLogId(generationLog.getIdx())
                        .generatedImage(toHistoryResponse(history))
                        .histories(toHistoriesResponse(transactionalUser))
                        .build();
            });

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "profile image generation failed.");
            }

            return response;
        } catch (ResponseStatusException exception) {
            if (objectKey != null) {
                profileImageStorageService.deleteObject(objectKey);
            }
            profileImageGenerationLogService.markFailed(generationLog.getIdx(), exception.getReason());
            throw exception;
        } catch (RuntimeException exception) {
            if (objectKey != null) {
                profileImageStorageService.deleteObject(objectKey);
            }
            profileImageGenerationLogService.markFailed(generationLog.getIdx(), exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "profile image generation failed.");
        }
    }

    @Transactional
    public UserProfileDto.ProfileImageHistoriesRes getProfileImageHistories(String userId) {
        User user = findUser(userId);

        return toHistoriesResponse(user);
    }

    @Transactional
    public UserProfileDto.ProfileImageSelectRes selectProfileImage(
            String userId,
            UserProfileDto.ProfileImageSelectReq dto
    ) {
        if (dto == null || dto.historyId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "historyId is required.");
        }

        User user = findUser(userId);
        UserProfile userProfile = ensureProfile(user);
        ProfileImageHistory history = profileImageHistoryRepository.findByIdxAndUserIdx(dto.historyId(), user.getIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "profile image history not found."));

        if (!profileImageStorageService.isProfileImageKeyForUser(user, history.getObjectKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "profile image key is not allowed.");
        }

        userProfile.updateProfileImageKey(history.getObjectKey());
        saveHistory(
                user,
                history.getObjectKey(),
                ProfileImageHistoryType.APPLIED,
                history.getSource(),
                history.getGenerationLog(),
                history.getPrompt()
        );
        pruneHistory(user, userProfile, ProfileImageHistoryType.APPLIED);

        return UserProfileDto.ProfileImageSelectRes.builder()
                .profile(toResponse(userProfile))
                .histories(toHistoriesResponse(user))
                .build();
    }

    private UserProfile getOrCreateProfile(String userId) {
        User user = findUser(userId);

        return ensureProfile(user);
    }

    private User findUser(String userId) {
        return userRepository.findUserById(userId)
                .or(() -> userRepository.findByEmail(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String resolveGenerationPrompt(UserProfileDto.ProfileImageGenerateReq dto) {
        if (dto == null || dto.prompt() == null || dto.prompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required.");
        }

        String prompt = dto.prompt().trim();

        if (prompt.length() > MAX_GENERATION_PROMPT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt must be 200 characters or less.");
        }

        return prompt;
    }

    private Integer resolveGenerationSize(UserProfileDto.ProfileImageGenerateReq dto) {
        Integer requestedSize = dto == null ? null : dto.size();

        if (requestedSize == null) {
            return PROFILE_IMAGE_SIZE;
        }

        if (requestedSize != PROFILE_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profile image size must be 1024.");
        }

        return requestedSize;
    }

    private ProfileImageHistory saveHistory(
            User user,
            String objectKey,
            ProfileImageHistoryType historyType,
            ProfileImageSource source,
            ProfileImageGenerationLog generationLog,
            String prompt
    ) {
        ProfileImageGenerationLog managedGenerationLog = generationLog == null
                ? null
                : profileImageGenerationLogRepository.getReferenceById(generationLog.getIdx());

        return profileImageHistoryRepository.saveAndFlush(ProfileImageHistory.builder()
                .user(user)
                .objectKey(objectKey)
                .historyType(historyType)
                .source(source)
                .generationLog(managedGenerationLog)
                .prompt(prompt)
                .build());
    }

    private void pruneHistory(User user, UserProfile userProfile, ProfileImageHistoryType historyType) {
        List<ProfileImageHistory> histories = profileImageHistoryRepository
                .findByUserIdxAndHistoryTypeOrderByCreatedAtDesc(user.getIdx(), historyType);

        if (histories.size() <= MAX_HISTORY_COUNT) {
            return;
        }

        List<ProfileImageHistory> staleHistories = histories.subList(MAX_HISTORY_COUNT, histories.size());
        Set<String> staleObjectKeys = new HashSet<>();

        staleHistories.forEach(history -> staleObjectKeys.add(history.getObjectKey()));
        profileImageHistoryRepository.deleteAll(staleHistories);
        profileImageHistoryRepository.flush();
        staleObjectKeys.forEach(objectKey -> deleteObjectIfUnreferenced(userProfile, objectKey));
    }

    private void deleteObjectIfUnreferenced(UserProfile userProfile, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        if (objectKey.equals(userProfile.getProfileImageKey())) {
            return;
        }

        if (profileImageHistoryRepository.countByObjectKey(objectKey) > 0) {
            return;
        }

        profileImageStorageService.deleteObject(objectKey);
    }

    private UserProfileDto.ProfileImageHistoriesRes toHistoriesResponse(User user) {
        return UserProfileDto.ProfileImageHistoriesRes.builder()
                .appliedImages(toHistoryResponses(profileImageHistoryRepository
                        .findByUserIdxAndHistoryTypeOrderByCreatedAtDesc(
                                user.getIdx(),
                                ProfileImageHistoryType.APPLIED
                        )))
                .generatedImages(toHistoryResponses(profileImageHistoryRepository
                        .findByUserIdxAndHistoryTypeOrderByCreatedAtDesc(
                                user.getIdx(),
                                ProfileImageHistoryType.GENERATED
                        )))
                .build();
    }

    private List<UserProfileDto.ProfileImageHistoryRes> toHistoryResponses(List<ProfileImageHistory> histories) {
        return histories.stream()
                .limit(MAX_HISTORY_COUNT)
                .map(this::toHistoryResponse)
                .toList();
    }

    private UserProfileDto.ProfileImageHistoryRes toHistoryResponse(ProfileImageHistory history) {
        return UserProfileDto.ProfileImageHistoryRes.from(
                history,
                profileImageStorageService.createViewUrl(history.getObjectKey())
        );
    }

    private UserProfileDto.Res toResponse(UserProfile userProfile) {
        return UserProfileDto.Res.from(
                userProfile,
                profileImageStorageService.createViewUrl(userProfile.getProfileImageKey())
        );
    }
}
