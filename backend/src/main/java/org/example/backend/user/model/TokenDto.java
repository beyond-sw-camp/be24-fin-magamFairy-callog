package org.example.backend.user.model;

public class TokenDto {
    public record AuthTokenResponse(
            String accessToken,
            String refreshToken
    ) {}
}
