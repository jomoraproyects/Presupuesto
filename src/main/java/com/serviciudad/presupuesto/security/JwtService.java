package com.serviciudad.presupuesto.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes UTF-8");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_MINUTES debe ser positivo");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = Math.multiplyExact(expirationMinutes, 60);
    }

    public String issue(String username) {
        Instant now = Instant.now();
        return Jwts.builder().subject(username).issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact();
    }

    public String username(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public long expirationSeconds() { return expirationSeconds; }
}
