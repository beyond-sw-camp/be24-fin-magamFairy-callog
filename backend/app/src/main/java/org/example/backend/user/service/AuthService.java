package org.example.backend.user.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.organization.repository.OrganizationRepository;
import org.example.backend.user.model.TokenDto;
import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.user.utils.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUtil jwtUtil;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public TokenDto.AuthTokenResponse issueTokens(Long userIdx, String id, String email, String name, String role, String companyName, String department) {
        String userId = requireId(id, email);
        User userEntity = userRepository.findById(userIdx).orElseThrow(NoSuchElementException::new);
        OrganizationType organizationType = (userEntity.getOrganization() != null) ? userEntity.getOrganization().getType() : null;
        String orgTypeName = (organizationType != null) ? organizationType.name() : null;

        String access = jwtUtil.createToken("access", userIdx, userId, email, name, role, companyName, department, 600000000L, organizationType);
        String refresh = jwtUtil.createToken("refresh", userIdx, userId, email, name, role, companyName, department, 1209600000L, organizationType);

        // Redis 저장 — TTL 14일 자동, RefreshTokenRedisService.save() 내부에서 기존 토큰 로테이션 처리
        refreshTokenRedisService.save(userId, refresh);

        return new TokenDto.AuthTokenResponse(access, refresh, orgTypeName);
    }

    @Transactional(readOnly = true)
    public TokenDto.AuthTokenResponse reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required.");
        }

        validateRefreshToken(refreshToken);

        String category = jwtUtil.getCategory(refreshToken);
        if (!"refresh".equals(category)) {
            throw new IllegalArgumentException("Invalid token category.");
        }

        Long userIdx = jwtUtil.getUserIdx(refreshToken);
        User user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        String userId = resolveUserId(user, refreshToken);

        // Redis 역인덱스 — 토큰으로 등록된 userId 조회 후 매칭 검증
        String registeredUserId = refreshTokenRedisService.findUserIdByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is not registered."));
        if (!userId.equals(registeredUserId)) {
            throw new IllegalArgumentException("Refresh token does not belong to user.");
        }

        if (!Boolean.TRUE.equals(user.getEnable()) || resolveStatus(user) != UserAccountStatus.ACTIVE) {
            refreshTokenRedisService.deleteByUserId(userId);
            throw new IllegalArgumentException("User is not allowed to access.");
        }
        User reissueUser = userRepository.findById(userIdx).orElseThrow(NoSuchElementException::new);
        OrganizationType organizationType = (reissueUser.getOrganization() != null) ? reissueUser.getOrganization().getType() : null;
        String newAccess = jwtUtil.createToken(
                "access",
                user.getIdx(),
                userId,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCompanyName(),
                user.getDepartment(),
                60000L,
                organizationType
        );
        return new TokenDto.AuthTokenResponse(newAccess, refreshToken, (organizationType != null) ? organizationType.name() : null);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRedisService.deleteByToken(refreshToken);
    }

    private UserAccountStatus resolveStatus(User user) {
        return user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus();
    }

    private String resolveUserId(User user, String refreshToken) {
        String tokenId = jwtUtil.getId(refreshToken);
        if (tokenId != null && !tokenId.isBlank()) {
            return tokenId;
        }
        return requireId(user.getId(), user.getEmail());
    }

    private String requireId(String id, String fallbackEmail) {
        if (id != null && !id.isBlank()) {
            return id;
        }
        if (fallbackEmail != null && !fallbackEmail.isBlank()) {
            return fallbackEmail;
        }
        throw new IllegalArgumentException("ID is required.");
    }

    private void validateRefreshToken(String refreshToken) {
        Boolean expired;
        try {
            expired = jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Refresh token has expired.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid refresh token.");
        }

        if (Boolean.TRUE.equals(expired)) {
            throw new IllegalArgumentException("Refresh token has expired.");
        }
    }
}
