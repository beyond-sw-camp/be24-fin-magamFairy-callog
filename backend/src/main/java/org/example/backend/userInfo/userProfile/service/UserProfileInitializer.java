package org.example.backend.userInfo.userProfile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class UserProfileInitializer implements ApplicationRunner {
    private final UserProfileService userProfileService;

    @Override
    public void run(ApplicationArguments args) {
        userProfileService.ensureProfilesForExistingUsers();
    }
}
