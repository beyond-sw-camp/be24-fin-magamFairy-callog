package org.example.backend.userInfo.userProfile.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.userInfo.userProfile.model.UserProfile;
import org.example.backend.userInfo.userProfile.model.UserProfileDto;
import org.example.backend.userInfo.userProfile.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileImageStorageService profileImageStorageService;

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

        if (previousObjectKey != null && !previousObjectKey.equals(objectKey)) {
            profileImageStorageService.deleteObject(previousObjectKey);
        }

        return toResponse(userProfile);
    }

    @Transactional
    public UserProfileDto.Res deleteProfileImage(String userId) {
        UserProfile userProfile = getOrCreateProfile(userId);
        String previousObjectKey = userProfile.getProfileImageKey();

        userProfile.clearProfileImage();
        profileImageStorageService.deleteObject(previousObjectKey);

        return toResponse(userProfile);
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

    private UserProfileDto.Res toResponse(UserProfile userProfile) {
        return UserProfileDto.Res.from(
                userProfile,
                profileImageStorageService.createViewUrl(userProfile.getProfileImageKey())
        );
    }
}
