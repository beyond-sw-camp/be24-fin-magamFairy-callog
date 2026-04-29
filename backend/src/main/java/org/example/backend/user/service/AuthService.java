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
    public TokenDto.AuthTokenResponse issueTokens(Long userIdx, String id, String email, String name, String role) {
        String userId = requireId(id, email);
        String access = jwtUtil.createToken("access", userIdx, userId, email, name, role, 600000L);
        String refresh = jwtUtil.createToken("refresh", userIdx, userId, email, name, role, 1209600000L);
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(14);

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(refresh, expiryDate),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(userId)
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
        String userId = resolveUserId(user, refreshToken);

        RefreshToken dbToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is not registered."));

        if (!dbToken.getToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh token does not match.");
        }

        if (!Boolean.TRUE.equals(user.getEnable()) || resolveStatus(user) != UserAccountStatus.ACTIVE) {
            refreshTokenRepository.deleteByUserId(userId);
            throw new IllegalArgumentException("User is not allowed to access.");
        }

        String newAccess = jwtUtil.createToken(
                "access",
                user.getIdx(),
                userId,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                600000L
        );
        String newRefresh = jwtUtil.createToken(
                "refresh",
                user.getIdx(),
                userId,
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
