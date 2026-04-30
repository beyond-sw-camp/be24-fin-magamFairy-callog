package org.example.backend.user.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey encodeKey;

    public JwtUtil(@Value("${jwt.key}") String key) {
        this.encodeKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(key));
    }

    public String createToken(String category, Long idx, String id, String email, String name, String role, String companyName, String department, Long expiredMs) {
        JwtBuilder builder = Jwts.builder()
                .claim("category", category)
                .claim("idx", idx)
                .claim("id", id)
                .claim("role", role)
                .claim("name", name)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(encodeKey);

        if (email != null && !email.isBlank()) {
            builder.claim("email", email);
        }
        if (companyName != null && !companyName.isBlank()) {
            builder.claim("companyName", companyName);
        }
        if (department != null && !department.isBlank()) {
            builder.claim("department", department);
        }

        return builder.compact();
    }

    public Long getUserIdx(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("idx", Long.class);
    }

    public String getId(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id", String.class);
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }

    public String getName(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("name", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public String getCategory(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("category", String.class);
    }

    public Boolean isExpired(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());
    }
}
