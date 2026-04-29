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

    @Transactional
    public UserProfileDto.Res getMyProfile(String userId) {
        return UserProfileDto.Res.from(getOrCreateProfile(userId));
    }

    @Transactional
    public UserProfileDto.Res updateMyProfile(String userId, UserProfileDto.UpdateReq dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }

        UserProfile userProfile = getOrCreateProfile(userId);
        userProfile.update(
                normalize(dto.email()),
                normalize(dto.phone()),
                normalize(dto.profileImageKey()),
                normalize(dto.profileImageUrl())
        );

        return UserProfileDto.Res.from(userProfile);
    }

    private UserProfile getOrCreateProfile(String userId) {
        User user = findUser(userId);

        return userProfileRepository.findByUserIdx(user.getIdx())
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder()
                        .user(user)
                        .email(user.getEmail())
                        .build()));
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
}
