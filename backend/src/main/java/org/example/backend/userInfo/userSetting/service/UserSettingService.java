package org.example.backend.userInfo.userSetting.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.userInfo.userSetting.model.UserSetting;
import org.example.backend.userInfo.userSetting.model.UserSettingDto;
import org.example.backend.userInfo.userSetting.repository.UserSettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserSettingService {
    private static final String DENSITY_COMFORTABLE = "comfortable";
    private static final String DENSITY_COMPACT = "compact";

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;

    @Transactional
    public UserSettingDto.Res getMySetting(String userId) {
        return UserSettingDto.Res.from(getOrCreateSetting(userId));
    }

    @Transactional
    public UserSettingDto.Res updateMySetting(String userId, UserSettingDto.UpdateReq dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }

        UserSetting userSetting = getOrCreateSetting(userId);
        userSetting.update(
                resolveDensity(dto.density()),
                dto.reduceMotion(),
                dto.highContrast()
        );

        return UserSettingDto.Res.from(userSetting);
    }

    private UserSetting getOrCreateSetting(String userId) {
        User user = findUser(userId);

        return userSettingRepository.findByUserIdx(user.getIdx())
                .orElseGet(() -> userSettingRepository.save(UserSetting.builder()
                        .user(user)
                        .build()));
    }

    private User findUser(String userId) {
        return userRepository.findUserById(userId)
                .or(() -> userRepository.findByEmail(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
    }

    private String resolveDensity(String density) {
        String normalizedDensity = normalize(density);

        if (normalizedDensity == null) {
            return null;
        }

        if (DENSITY_COMFORTABLE.equals(normalizedDensity) || DENSITY_COMPACT.equals(normalizedDensity)) {
            return normalizedDensity;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "density must be comfortable or compact.");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase();
    }
}
