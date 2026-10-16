package com.pacgram.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(
            @Value("${pacgram.jwt.secret}") String secret,
            @Value("${pacgram.jwt.expiration}") Duration expiration) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String issueToken(Long userId, String username) {
        return issueToken(userId, username, false);
    }

    public String issueAdministratorToken(Long userId, String username) {
        return issueToken(userId, username, true);
    }

    private String issueToken(Long userId, String username, boolean administrator) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("administrator", administrator)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public AccountPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Boolean administrator = claims.get("administrator", Boolean.class);
        return new AccountPrincipal(Long.valueOf(claims.getSubject()),
                claims.get("username", String.class), Boolean.TRUE.equals(administrator));
    }
}
