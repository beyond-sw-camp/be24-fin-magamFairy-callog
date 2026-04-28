package org.example.backend.user.repository;


import org.example.backend.user.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByLoginId(String loginId);
    void deleteByLoginId(String loginId);
    void deleteByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime now);
}
