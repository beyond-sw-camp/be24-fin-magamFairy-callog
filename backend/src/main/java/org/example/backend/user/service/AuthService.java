package org.example.backend.user.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.RefreshToken;
import org.example.backend.user.model.TokenDto;
import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.repository.RefreshTokenRepository;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.user.utils.JwtUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public TokenDto.AuthTokenResponse issueTokens(Long userIdx, String userId, String email, String name, String role) {
        String loginId = requireLoginId(userId, email);
        String access = jwtUtil.createToken("access", userIdx, loginId, email, name, role, 600000L);
        String refresh = jwtUtil.createToken("refresh", userIdx, loginId, email, name, role, 1209600000L);
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(14);

        refreshTokenRepository.findByLoginId(loginId)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(refresh, expiryDate),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .loginId(loginId)
                                        .token(refresh)
                                        .expiryDate(expiryDate)
                                        .build()
                        )
                );

        return new TokenDto.AuthTokenResponse(access, refresh);
    }

    @Transactional
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
        String loginId = resolveLoginId(user, refreshToken);

        RefreshToken dbToken = refreshTokenRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is not registered."));

        if (!dbToken.getToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh token does not match.");
        }

        if (!Boolean.TRUE.equals(user.getEnable()) || resolveStatus(user) != UserAccountStatus.ACTIVE) {
            refreshTokenRepository.deleteByLoginId(loginId);
            throw new IllegalArgumentException("User is not allowed to access.");
        }

        String newAccess = jwtUtil.createToken(
                "access",
                user.getIdx(),
                loginId,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                600000L
        );
        String newRefresh = jwtUtil.createToken(
                "refresh",
                user.getIdx(),
                loginId,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                1209600000L
        );

        dbToken.updateToken(newRefresh, LocalDateTime.now().plusDays(14));

        return new TokenDto.AuthTokenResponse(newAccess, newRefresh);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    private UserAccountStatus resolveStatus(User user) {
        return user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus();
    }

    private String resolveLoginId(User user, String refreshToken) {
        String tokenLoginId = jwtUtil.getId(refreshToken);
        if (tokenLoginId != null && !tokenLoginId.isBlank()) {
            return tokenLoginId;
        }
        return requireLoginId(user.getLoginId(), user.getEmail());
    }

    private String requireLoginId(String loginId, String fallbackEmail) {
        if (loginId != null && !loginId.isBlank()) {
            return loginId;
        }
        if (fallbackEmail != null && !fallbackEmail.isBlank()) {
            return fallbackEmail;
        }
        throw new IllegalArgumentException("Login id is required.");
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
