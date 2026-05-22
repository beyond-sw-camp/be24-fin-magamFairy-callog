package org.example.springgw.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    private final SecretKey encodeKey;

    // 기존 callog의 jwt.key 설정을 똑같이 받아옵니다. Base64Url 디코딩 사양 유지
    public JwtUtil(@Value("${jwt.key}") String key) {
        this.encodeKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(key));
    }

    // 💡 Gateway에서 한 번의 파싱으로 모든 정보를 뜯어내기 위한 헬퍼 메서드
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}